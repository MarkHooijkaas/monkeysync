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
    public void syncAll(Table srcTable, Table destTable) {
        createNewRecords(srcTable,destTable);

        updateRecords(srcTable,destTable);
        deleteInactiveRecords(srcTable,destTable);

        if (enableDeleteMissingRecords)
            deleteMissingRecords(srcTable,destTable);
    }

    public int createNewRecords(Table srcTable, Table destTable) {
        int count=0;
        for (Record src : srcTable.records()) {
            final String key = src.getKey();
            if (destTable.mayCreateRecord(key)) {
                destTable.create(src);
                Env.verbose("created ",src.getKey());
                count++;
            }
        }
        Env.info("created :", ""+count);
        return count;
    }

    public int updateRecords(Table srcTable, Table destTable) {
        int count=0;
        int identical=0;
        int blocked=0;
        LinkedHashMap<String, String> diffs=new LinkedHashMap<>();
        for (Record src : srcTable.records()) {
            final String key = src.getKey();
            Record dest=destTable.getRecord(key);
            if (destTable.mayUpdateRecord(key)) {
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
                    destTable.update(dest, diffs);
                    Env.verbose("merging: ",key);
                    count++;
                }
                else {
                    Env.debug("identical for ", key);
                    identical++;
                }
            }
            else
                blocked++;
        }
        Env.info("updated:", ""+count);
        Env.info("identical: ",""+identical);
        Env.info("blocked for updating: ",""+blocked);
        return count;
    }


    public int deleteInactiveRecords(Table srcTable, Table destTable) {
        int count = 0;
        for (Record src : srcTable.records()) {
            final String key = src.getKey();
            if (srcTable.isDeleteDesired(key) && destTable.mayDeleteRecord(key)) {
                destTable.delete(src);
                Env.verbose("deleted ", src.getKey());
                count++;
            }
        }
        Env.info("deleted because marked deleted in source:", ""+count);
        return count;
    }
    public int deleteMissingRecords(Table srcTable, Table destTable) {
        int count=0;
        if (false) {
            for (Record dest : destTable.records()) {
                final String key = dest.getKey();
                if (destTable.mayDeleteRecord(key) && !srcTable.recordExists(key)) {
                    destTable.delete(dest);
                    Env.verbose("deleted record which does not exist at source ",key);
                    count++;

                }
            }
        }
        Env.info("deleted because missing in source:", ""+count);
        return count;
    }
}
