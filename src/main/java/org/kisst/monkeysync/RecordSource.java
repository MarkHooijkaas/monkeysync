package org.kisst.monkeysync;

public interface RecordSource {
    boolean recordExists(String key);
    Record getRecord(String key);

    void markAsHandled(String key);
    Iterable<Record>  unhandledRecords();
}
