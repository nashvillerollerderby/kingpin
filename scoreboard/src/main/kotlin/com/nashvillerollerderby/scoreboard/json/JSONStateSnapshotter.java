package com.nashvillerollerderby.scoreboard.json;

import com.fasterxml.jackson.jr.ob.JSON;
import com.nashvillerollerderby.scoreboard.core.interfaces.Game;
import com.nashvillerollerderby.scoreboard.utils.BasePath;
import com.nashvillerollerderby.scoreboard.utils.Log4j2Logging;
import io.prometheus.client.Histogram;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class JSONStateSnapshotter implements JSONStateListener {

    private final Logger logger = Log4j2Logging.getLogger(this);

    public JSONStateSnapshotter(JSONStateManager jsm, Game g, boolean useMetrics) {
        this.directory = BasePath.get();
        game = g;
        this.useMetrics = useMetrics;
        if (useMetrics && updateStateDuration == null) {
            updateStateDuration = Histogram.build()
                    .name("crg_json_state_disk_snapshot_duration_seconds")
                    .help("Time spent writing JSON state snapshots to disk")
                    .register();
        }
        filters.add("ScoreBoard.Version");
        filters.add("ScoreBoard.Game(" + game.getId() + ")");
        jsm.register(this);
    }

    @Override
    public synchronized void sendUpdates(StateTrie newState, StateTrie changedState) {
        state = newState;
        if (writeOnNextUpdate) {
            writeOnNextUpdate = false;
            writeFile();
        }
    }

    public void writeOnNextUpdate() {
        writeOnNextUpdate = true;
    }

    public synchronized void writeFile() {
        Histogram.Timer timer = useMetrics ? updateStateDuration.startTimer() : null;

        File file = new File(new File(directory, "html/game-data/json"), game.getFilename() + ".json");
        File prev = new File(new File(directory, "html/game-data/json"), game.getFilename() + "_prev.json");
        file.getParentFile().mkdirs();

        File tmp = null;
        OutputStreamWriter out = null;
        try {
            // Put inside a "state" entry to match the WS.
            String json = JSON.std.with(JSON.Feature.PRETTY_PRINT_OUTPUT)
                    .composeString()
                    .startObject()
                    .putObject("state", state.filter(filters, true))
                    .end()
                    .finish();
            tmp = File.createTempFile(file.getName(), ".tmp", directory);
            out = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8);
            out.write(json);
            out.close();
            prev.delete();
            file.renameTo(prev);
            if (tmp.renameTo(file)) {
                prev.delete();
            }
        } catch (Exception e) {
            logger.error("Error writing JSON snapshot: {}", e.getMessage(), e);
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
        if (useMetrics) {
            timer.observeDuration();
        }
    }

    private final File directory;
    private final Game game;
    private boolean writeOnNextUpdate = false;
    private StateTrie state = new StateTrie();
    private final PathTrie filters = new PathTrie();

    private final boolean useMetrics;
    private static Histogram updateStateDuration = null;
}
