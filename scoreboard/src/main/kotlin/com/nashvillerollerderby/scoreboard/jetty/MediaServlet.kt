package com.nashvillerollerderby.scoreboard.jetty

import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.fileupload2.core.DiskFileItem
import org.apache.commons.fileupload2.core.DiskFileItemFactory
import org.apache.commons.fileupload2.core.FileItem
import org.apache.commons.fileupload2.core.FileItemFactory
import org.apache.commons.fileupload2.core.FileUploadException
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class MediaServlet(var scoreBoard: ScoreBoard, private var htmlDirName: String) : HttpServlet() {
    @Throws(ServletException::class, IOException::class)
    override fun doPost(request: HttpServletRequest, response: HttpServletResponse) {
        if (scoreBoard.clients.getDevice(request.session.id).mayWrite()) {
            scoreBoard.clients.getDevice(request.session.id).write()

            response.setHeader("Cache-Control", "no-cache")
            response.setHeader("Expires", "-1")
            response.characterEncoding = "UTF-8"

            if (request.pathInfo == "/upload") {
                upload(request, response)
            } else if (request.pathInfo == "/remove") {
                remove(request, response)
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND)
            }
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "No write access")
        }
    }

    @Throws(ServletException::class, IOException::class)
    fun upload(request: HttpServletRequest, response: HttpServletResponse) {
        try {
            if (!JakartaServletFileUpload.isMultipartContent(request)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST)
                return
            }

            var media: String? = null
            var type: String? = null
            val fiF: FileItemFactory<DiskFileItem> = DiskFileItemFactory.builder().get()
            val sfU = JakartaServletFileUpload(fiF)
            val fileItems: MutableList<FileItem<*>> = LinkedList()
            val i: Iterator<*> = sfU.parseRequest(request).iterator()

            while (i.hasNext()) {
                val item = i.next() as FileItem<*>
                if (item.isFormField) {
                    if (item.fieldName == "media") {
                        media = item.string
                    } else if (item.fieldName == "type") {
                        type = item.string
                    }
                } else if (item.name.matches(zipExtRegex.toRegex())) {
                    processZipFileItem(fiF, item, fileItems)
                } else if (scoreBoard.media.validFileName(item.name)) {
                    fileItems.add(item)
                }
            }

            if (fileItems.size == 0) {
                setTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, "No files provided to upload")
                return
            }

            processFileItemList(fileItems, media!!, type!!)

            val len = fileItems.size
            setTextResponse(
                response, HttpServletResponse.SC_OK,
                "Successfully uploaded " + len + " file" + (if (len > 1) "s" else "")
            )
        } catch (fnfE: FileNotFoundException) {
            setTextResponse(response, HttpServletResponse.SC_NOT_FOUND, fnfE.message)
        } catch (iaE: IllegalArgumentException) {
            setTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, iaE.message)
        } catch (fuE: FileUploadException) {
            setTextResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, fuE.message)
        }
    }

    @Throws(ServletException::class, IOException::class)
    fun remove(request: HttpServletRequest, response: HttpServletResponse) {
        val media = request.getParameter("media")
        val type = request.getParameter("type")
        val filename = request.getParameter("filename")

        val success = scoreBoard.media.removeMediaFile(media, type, filename)
        if (!success) {
            setTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Failed to remove file")
        } else {
            setTextResponse(response, HttpServletResponse.SC_OK, "Successfully removed")
        }
    }

    @Throws(FileNotFoundException::class, IOException::class)
    fun processFileItemList(fileItems: MutableList<FileItem<*>>, media: String, type: String) {
        val typeDir = getTypeDir(media, type)

        val fileItemIterator = fileItems.listIterator()
        while (fileItemIterator.hasNext()) {
            val item = fileItemIterator.next()
            if (item.isFormField) {
                fileItemIterator.remove()
            } else {
                createFile(typeDir, item)
            }
        }
    }

    @Throws(FileNotFoundException::class, IllegalArgumentException::class)
    fun getTypeDir(media: String, type: String): File {
        require(
            !(scoreBoard.media.getFormat(media) == null ||
                    scoreBoard.media.getFormat(media).getType(type) == null)
        ) { "Invalid media '$media' or type '$type'" }

        val htmlDir = File(htmlDirName)
        val mediaDir = File(htmlDir, media)
        val typeDir = File(mediaDir, type)
        return typeDir
    }

    @Throws(IOException::class, FileNotFoundException::class)
    fun createFile(typeDir: File?, item: FileItem<*>): File {
        val f = File(typeDir, item.name)
        f.parentFile.mkdirs()
        var fos: FileOutputStream? = null
        val `is` = item.inputStream
        try {
            fos = FileOutputStream(f)
            IOUtils.copyLarge(`is`, fos)
            return f
        } finally {
            `is`.close()
            fos?.close()
        }
    }

    @Throws(IOException::class)
    fun processZipFileItem(factory: FileItemFactory<DiskFileItem>, zip: FileItem<*>, fileItems: MutableList<FileItem<*>>) {
        val ziS = ZipInputStream(zip.inputStream)
        var zE: ZipEntry?
        ziS.use {
            while (null != (it.nextEntry.also { zE = it })) {
                if (zE!!.isDirectory || !scoreBoard.media.validFileName(zE!!.name)) {
                    continue
                }
                val fileBuilder = factory.fileItemBuilder<DiskFileItem.Builder>()
                fileBuilder.isFormField = false
                fileBuilder.fileName =zE!!.name
                val item = fileBuilder.get()

                val oS = item.outputStream
                IOUtils.copyLarge(it, oS)
                oS.close()
                fileItems.add(item)
            }
        }
    }

    @Throws(IOException::class)
    fun setTextResponse(response: HttpServletResponse, code: Int, text: String?) {
        response.contentType = "text/plain"
        response.writer.print(text)
        response.status = code
    }

    companion object {
        const val zipExtRegex: String = "^.*[.][zZ][iI][pP]$"
    }
}
