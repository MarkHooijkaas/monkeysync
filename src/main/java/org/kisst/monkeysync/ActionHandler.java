package org.kisst.monkeysync;

import java.util.Map;

public interface ActionHandler {
    void create(Record rec);
    void update(Record rec, Map<String, String> diffs);
    void delete(Record rec);
}
