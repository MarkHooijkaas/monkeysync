package org.kisst.monkeysync;

import java.util.HashSet;
import java.util.LinkedHashMap;

public class Syncer {
    private final HashSet<String> handled = new HashSet<>();
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
    public void sync(Table srcdb, Table destdb) {
        LinkedHashMap<String, String> diffs=new LinkedHashMap<>();
        for (Record dest : destdb.records()) {
            final String key=dest.getKey();
            if (destdb.updateBlocked(key)) {
                handled.add(key);
                continue;
            }
            if (! srcdb.recordExists(key))
                continue;
            Record src = srcdb.getRecord(key);
            if (! srcdb.deleteDesired(key)) {
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
            // System.out.println(diffs.size()+" for "+dest.getKey());
            if (diffs.size()>0)
                destdb.update(dest, diffs);
            else
                System.out.println("identical for "+key);
        }
        for (Record src: srcdb.records()) {
            if (!handled.contains(src.getKey()))
                destdb.create(src);
        }
    }
}
