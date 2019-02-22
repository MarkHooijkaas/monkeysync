package org.kisst.monkeysync;

public interface CachedObject {
    void load();
    void load(String filename);
    void fetch();
    void save(String filename);
    void save();
    void autoSave();
}
