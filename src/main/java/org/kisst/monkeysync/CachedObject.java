package org.kisst.monkeysync;

import org.kisst.script.Context;

public interface CachedObject {
    void load();
    void load(String filename);
    void fetch();
    void save(String filename);
    void save();
    void autoSave();
}
