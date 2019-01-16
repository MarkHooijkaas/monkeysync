package org.kisst.monkeysync;

import java.util.Map;

public interface RecordDestination {
    Iterable<DestRecord> records();

    void create(SourceRecord srcrec);
    void update(DestRecord destrec, Map<String, String> diffs);
    void delete(DestRecord destrec);

}
