package org.kisst.monkeysync;

import java.util.Map;

public interface RecordDestination {
    Iterable<Record> records();
}
