package org.kisst.monkeysync;

import org.kisst.script.Context;

public interface CachedObject {
    void load(Context ctx);
    void load(Context ctx, String filename);
    void fetch(Context ctx);
    void save(Context ctx, String filename);
    void save(Context ctx);
    void autoSave(Context ctx);
}
