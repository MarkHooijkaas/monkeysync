package org.kisst.monkeysync;

public interface RecordSource {
    boolean recordExists(String key);
    SourceRecord getRecord(String key);

    void markAsHandled(String key);
    Iterable<SourceRecord>  unhandledRecords();
}
