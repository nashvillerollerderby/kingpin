package com.nashvillerollerderby.scoreboard.json;

import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard;
import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEvent;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProviderImpl;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardListener;
import com.nashvillerollerderby.scoreboard.event.Value;
import com.nashvillerollerderby.scoreboard.event.ValueWithId;
import com.nashvillerollerderby.scoreboard.utils.Log4j2Logging;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * Converts a ScoreBoardEvent into a representative JSON Update
 */
public class ScoreBoardJSONListener implements ScoreBoardListener {

    private final Logger logger = Log4j2Logging.getLogger(this);

    public ScoreBoardJSONListener(ScoreBoard sb, JSONStateManager jsm) {
        this.jsm = jsm;
        process(sb, false);
        updateState();
        sb.addScoreBoardListener(this);
    }

    @Override
    public void scoreBoardChange(ScoreBoardEvent<?> event) {
        synchronized (this) {
            try {
                ScoreBoardEventProvider p = event.getProvider();
                String provider = p.getProviderName();
                Property<?> prop = event.getProperty();
                Object v = event.getValue();
                boolean rem = event.isRemove();
                if (prop == ScoreBoardEventProviderImpl.BATCH_START) {
                    batch++;
                } else if (prop == ScoreBoardEventProviderImpl.BATCH_END) {
                    if (batch > 0) {
                        batch--;
                    }
                } else if (prop instanceof Value) {
                    update(getPath(p), prop, v);
                } else if (prop instanceof Child) {
                    if (v instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider) v).getParent() == p) {
                        process((ScoreBoardEventProvider) v, rem);
                    } else if (rem) {
                        remove(getPath(p), prop, ((ValueWithId) v).getId());
                    } else {
                        update(getPath(p), prop, v);
                    }
                } else {
                    logger.info("{} update of unknown kind.\tprop: {}, v: {}", provider, prop.getJsonName(), v);
                }
            } catch (Exception e) {
                logger.error(e);
            } finally {
                if (batch == 0) {
                    updateState();
                }
            }
        }
    }

    private void updateState() {
        synchronized (this) {
            if (updates.isEmpty()) {
                return;
            }
            jsm.updateState(updates);
            updates.clear();
        }
    }

    private void update(String prefix, Property<?> prop, Object v) {
        String path = prefix + "." + prop.getJsonName();
        if (prop instanceof Child) {
            updates.add(new WSUpdate(path + "(" + ((ValueWithId) v).getId() + ")", ((ValueWithId) v).getValue()));
        } else if (v instanceof ScoreBoardEventProvider) {
            updates.add(new WSUpdate(path, ((ScoreBoardEventProvider) v).getId()));
        } else if (v == null || v instanceof Boolean || v instanceof Integer || v instanceof Long) {
            updates.add(new WSUpdate(path, v));
        } else {
            updates.add(new WSUpdate(path, v.toString()));
        }
    }

    private void remove(String prefix, Property<?> prop, String id) {
        String path = prefix + "." + prop.getJsonName() + "(" + id + ")";
        updates.add(new WSUpdate(path, null));
    }

    private void process(ScoreBoardEventProvider p, boolean remove) {
        String path = getPath(p);
        updates.add(new WSUpdate(path, null));
        if (remove) {
            return;
        }

        for (Property<?> prop : p.getProperties()) {
            if (prop instanceof Value) {
                Object v = p.get((Value<?>) prop);
                update(path, prop, v);
            } else if (prop instanceof Child) {
                for (ValueWithId c : p.getAll((Child<?>) prop)) {
                    if (c instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider) c).getParent() == p) {
                        process((ScoreBoardEventProvider) c, false);
                    } else {
                        update(getPath(p), prop, c);
                    }
                }
            }
        }
    }

    String getPath(ScoreBoardEventProvider p) {
        String path = "";
        if (p.getParent() != null) {
            path = getPath(p.getParent()) + ".";
        }
        path = path + p.getProviderName();
        if (!"".equals(p.getProviderId()) && p.getProviderId() != null) {
            path = path + "(" + p.getProviderId() + ")";
        }
        return path;
    }

    private final JSONStateManager jsm;
    private final List<WSUpdate> updates = new LinkedList<>();
    private long batch = 0;
}
