package com.nashvillerollerderby.scoreboard.json;

import com.fasterxml.jackson.jr.ob.JSON;
import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider.Source;
import com.nashvillerollerderby.scoreboard.utils.Log4j2Logging;
import io.prometheus.client.Histogram;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoSaveJSONState implements Runnable {

    private final Logger logger = Log4j2Logging.getLogger(this);

    public AutoSaveJSONState(JSONStateManager jsm, File dir, boolean useMetrics) {
        this.dir = dir;
        this.jsm = jsm;
        this.useMetrics = useMetrics;
        autosaveDuration = useMetrics ? Histogram.build()
                .name("crg_json_autosave_write_duration_seconds")
                .help("Time spent writing JSON autosaves to disk")
                .register()
                : null;

        try {
            FileUtils.forceMkdir(dir);
        } catch (IOException ioE) {
            logger.warn("Unable to create auto-save directory '{}' : {}", dir, ioE.getMessage());
            throw new RuntimeException(ioE);
        }
        backupAutoSavedFiles();
        executor.scheduleAtFixedRate(AutoSaveJSONState.this, INTERVAL_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public synchronized void run() {
        Histogram.Timer timer = useMetrics ? autosaveDuration.startTimer() : null;
        try {
            int n = AUTOSAVE_FILES;
            getFile(n).delete();
            while (n > 0) {
                File to = getFile(n);
                File from = getFile(--n);
                if (from.exists()) {
                    from.renameTo(to);
                }
            }
            writeAutoSave(getFile(0));
        } catch (Exception e) {
            logger.warn("Unable to auto-save scoreboard : {}", e.getMessage(), e);
        }
        if (useMetrics) {
            timer.observeDuration();
        }
    }

    private void writeAutoSave(File file) {
        File tmp = null;
        OutputStreamWriter out = null;
        try {
            String json = JSON.std.with(JSON.Feature.PRETTY_PRINT_OUTPUT)
                    .composeString()
                    .startObject()
                    .putObject("state", jsm.getState())
                    .end()
                    .finish();
            tmp = File.createTempFile(file.getName(), ".tmp", dir);
            out = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8);
            out.write(json);
            out.close();
            tmp.renameTo(file); // This is atomic.
        } catch (Exception e) {
            logger.error("Error writing JSON autosave: {}", e.getMessage(), e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
            if (tmp != null) {
                try {
                    tmp.delete();
                } catch (Exception e) {
                }
            }
        }
    }

    private void backupAutoSavedFiles() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        File mainBackupDir = new File(dir, "backup");
        File backupDir = new File(mainBackupDir, dateFormat.format(new Date()));
        if (backupDir.exists()) {
            logger.warn("Could not back up auto-save files, backup directory already exists");
        } else {
            int n = 0;
            do {
                File from = getFile(n);
                if (from.exists()) {
                    try {
                        FileUtils.copyFileToDirectory(from, backupDir, true);
                    } catch (Exception e) {
                        logger.error("Could not back up auto-save file '{}' : {}", from.getName(), e.getMessage(), e);
                    }
                }
            } while (n++ < AUTOSAVE_FILES);
        }
    }

    public File getFile(int n) {
        return getFile(n, dir);
    }

    public static File getFile(int n, File dir) {
        return new File(dir, ("scoreboard-" + (n * INTERVAL_SECONDS) + "-secs-ago.json"));
    }

    public static boolean loadAutoSave(ScoreBoard scoreBoard, File dir) {
        Logger logger = Log4j2Logging.getLogger(AutoSaveJSONState.class);
        for (int i = 0; i <= AUTOSAVE_FILES; i++) {
            File f = getFile(i, dir);
            if (!f.exists()) {
                continue;
            }
            try {
                loadFile(scoreBoard, f, Source.AUTOSAVE);
                logger.info("Loaded auto-saved scoreboard from {}", f.getPath());
                return true;
            } catch (Exception e) {
                logger.error("Could not load auto-saved scoreboard JSON file {} : {}", f.getPath(), e.getMessage(), e);
            }
        }

        return false;
    }

    public static void loadFile(ScoreBoard scoreBoard, File f, Source source) throws Exception {
        Map<String, Object> map = JSON.std.mapFrom(f);
        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) map.get("state");

        ScoreBoardJSONSetter.updateToCurrentVersion(state);
        ScoreBoardJSONSetter.set(scoreBoard, state, source);
    }

    private final File dir;
    private final JSONStateManager jsm;
    private final boolean useMetrics;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private static final int AUTOSAVE_FILES = 6;
    private static final int INTERVAL_SECONDS = 10;

    private final Histogram autosaveDuration;
}
