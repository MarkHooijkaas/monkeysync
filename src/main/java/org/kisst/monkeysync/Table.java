package org.kisst.monkeysync;

import java.util.Map;

public interface Table {
    boolean recordExists(String key);
    boolean deleteDesired(String key);
    boolean updateBlocked(String key);

    Iterable<Record> records();
    Record getRecord(String key);

    void create(Record srcrec);
    void update(Record destrec, Map<String, String> diffs);
    void delete(Record destrec);
}
