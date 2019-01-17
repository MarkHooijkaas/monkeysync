package org.kisst.monkeysync;

import java.util.Map;

public interface Table {
    boolean recordExists(String key);
    boolean mayCreateRecord(String key);
    boolean mayUpdateRecord(String key);
    boolean mayDeleteRecord(String key);

    boolean isDeleteDesired(String key);

    int size();
    Iterable<Record> records();

    Record getRecord(String key);

    void create(Record srcrec);
    void update(Record destrec, Map<String, String> diffs);
    void delete(Record destrec);
}
