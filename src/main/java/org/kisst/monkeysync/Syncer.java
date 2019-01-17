package org.kisst.monkeysync;

import java.util.LinkedHashMap;

public class Syncer {
    private boolean enableDeleteMissingRecords =false;

    /**
     * This method will sync all records from the source into the destination.
     * To be more precise, it will
     * - delete any records in the source that are marked as inactive in the source
     * - update any records in the source for which one or more of the fields in the source record does not match the field in the destination
     * - create any records that exist only in the source, but not (yet) in the destination
     * Note: if the destination record has any fields that are not know in the source record they are ignored (they are not deleted)
     */
    public void syncAll(Table srcdb, Table destdb) {
        int count=createNewRecords(srcdb,destdb);
        Env.info("created :", ""+count);

        count=updateRecords(srcdb,destdb);
        Env.info("updated:", ""+count);

        count=deleteInactiveRecords(srcdb,destdb);
        Env.info("deleted :", ""+count);

        if (enableDeleteMissingRecords) {
            count=deleteMissingRecords(srcdb,destdb);
            Env.info("deleted missing records:", ""+count);
        }
    }

    public int createNewRecords(Table srcdb, Table destdb) {
        int count=0;
        for (Record src : srcdb.records()) {
            final String key = src.getKey();
            if (destdb.mayCreateRecord(key)) {
                destdb.create(src);
                Env.verbose("created ",src.getKey());
                count++;
            }
        }
        return count;
    }

    public int updateRecords(Table srcdb, Table destdb) {
        int count=0;
        int identical=0;
        LinkedHashMap<String, String> diffs=new LinkedHashMap<>();
        for (Record src : srcdb.records()) {
            final String key = src.getKey();
            Record dest=destdb.getRecord(key);
            if (destdb.mayUpdateRecord(key)) {
                diffs.clear();
                for (String fieldName: src.fieldNames()) {
                    String value=src.getField(fieldName);
                    if (value==null)
                        continue;
                    if (! value.equals(dest.getField(fieldName))) // TODO: what if empty string
                        diffs.put(fieldName, value);
                }
                // System.out.println(diffs.size()+" for "+dest.getKey());
                if (diffs.size()>0) {
                    destdb.update(dest, diffs);
                    Env.verbose("merging: ",key);
                    count++;
                }
                else {
                    Env.debug("identical for ", key);
                    identical++;
                }
            }
        }
        Env.debug("Total identical: ",""+identical);
        return count;
    }


    public int deleteInactiveRecords(Table srcdb, Table destdb) {
        int count = 0;
        for (Record src : srcdb.records()) {
            final String key = src.getKey();
            if (srcdb.isDeleteDesired(key) && destdb.mayDeleteRecord(key)) {
                destdb.delete(src);
                Env.verbose("deleted ", src.getKey());
                count++;
            }
        }
        return count;
    }
    public int deleteMissingRecords(Table srcdb, Table destdb) {
        int count=0;
        if (false) {
            for (Record dest : destdb.records()) {
                final String key = dest.getKey();
                if (destdb.mayDeleteRecord(key) && !srcdb.recordExists(key)) {
                    destdb.delete(dest);
                    Env.verbose("deleted record which does not exist at source ",key);
                    count++;

                }
            }
        }
        return count;
    }
}
