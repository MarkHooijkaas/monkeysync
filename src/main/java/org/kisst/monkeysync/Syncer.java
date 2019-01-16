package org.kisst.monkeysync;

import java.util.LinkedHashMap;

public class Syncer {
    /**
     * This method will sync all records from the source into the destination.
     * To be more precise, it will
     * - delete any records in the source that are marked as inactive in the source
     * - update any records in the source for which one or more of the fields in the source record does not match the field in the destination
     * - create any records that exist only in the source, but not (yet) in the destination
     * Note: if the destination record has any fields that are not know in the source record they are ignored (they are not deleted)
     *
     * @param srcdb the source of records
     * @param destdb the destination in which they are merged
     */
    public static void sync(RecordSource srcdb, RecordDestination destdb) {
        LinkedHashMap<String, String> diffs=new LinkedHashMap<>();
        for (DestRecord dest : destdb.records()) {
            if (dest.blocked()) {
                srcdb.markAsHandled(dest.getKey());
                continue;
            }
            if (! srcdb.recordExists(dest.getKey()))
                continue;
            SourceRecord src = srcdb.getRecord(dest.getKey());
            if (! src.deleted()) {
                destdb.delete(dest);
                continue;
            }
            diffs.clear();
            for (String fieldName: src.fieldNames()) {
                String value=src.getField(fieldName);
                if (value==null)
                    continue;
                if (! value.equals(dest.getField(fieldName))) // TODO: what if empty string
                    diffs.put(fieldName, value);
            }
            if (diffs.size()>0)
                destdb.update(dest, diffs);
        }
        for (SourceRecord src: srcdb.unhandledRecords())
            destdb.create(src);
    }
}
