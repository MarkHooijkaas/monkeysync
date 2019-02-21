package org.kisst.monkeysync;

import java.util.HashSet;
import java.util.LinkedHashMap;

public class Syncer {
    private boolean enableDeleteMissingRecords =false;
    private final boolean dryRun;
    private final HashSet<String> ignoreFields=new HashSet<>();

    public Syncer(){
        // TODO: pass props
        String str=Env.props.getString("sync.ignoreFields",null);
        if (str!=null) {
            for (String field : str.split(","))
                ignoreFields.add(field.trim());
        }
        this.dryRun=Env.props.getBoolean("dryRun", true);
    }

    /**
     * This method will sync all records from the source into the destination.
     * To be more precise, it will
     * - delete any records in the source that are marked as inactive in the source
     * - update any records in the source for which one or more of the fields in the source record does not match the field in the destination
     * - create any records that exist only in the source, but not (yet) in the destination
     * Note: if the destination record has any fields that are not know in the source record they are ignored (they are not deleted)
     */
    public void syncAll(Table srcTable, Table destTable) {
        updateRecords(srcTable,destTable);
        createNewRecords(srcTable,destTable);
        deleteInactiveRecords(srcTable,destTable);
        if (enableDeleteMissingRecords)
            deleteMissingRecords(srcTable,destTable);
    }

    public int createNewRecords(Table srcTable, Table destTable) {
        Env.info("* Creating", "");
        int count=0;
        int inactive=0;
        for (Record src : srcTable.records()) {
            final String key = src.getKey();
            if (!srcTable.isActive(src)) {
                inactive++;
                continue;
            }
            if (destTable.mayCreateRecord(key)) {
                if (Env.ask("About to create "+key)) {
                    if (! dryRun)
                        destTable.create(src);
                    Env.verbose("created ", src.getKey());
                    count++;
                }
            }
        }
        Env.info("   created :", ""+count);
        Env.info("   inactive :", ""+inactive);
        return count;
    }

    public int updateRecords(Table srcTable, Table destTable) {
        Env.info("* Updating ", "");
        int count=0;
        int identical=0;
        int blocked=0;
        int missing=0;
        int notActive=0;
        LinkedHashMap<String, String> diffs=new LinkedHashMap<>();
        for (Record src : srcTable.records()) {
            final String key = src.getKey();
            Record dest=destTable.getRecord(key);
            if (dest==null) {
                missing++;
                continue;
            }
            if (! srcTable.isActive(src)) {
                notActive++;
                continue;
            }
            if (destTable.mayUpdateRecord(key)) {
                diffs.clear();
                for (String fieldName: src.fieldNames()) {
                    if (ignoreFields.contains(fieldName))
                        continue;
                    String value=src.getField(fieldName);
                    if (value==null)
                        continue;
                    if (! value.equals(dest.getField(fieldName))) // TODO: what if empty string
                        diffs.put(fieldName, value);
                }
                // System.out.println(diffs.size()+" for "+dest.getKey());
                if (diffs.size()>0) {
                    if (Env.ask("About to merge "+key+diffs)) {
                        if (! dryRun)
                            destTable.update(dest, diffs);
                        Env.verbose("merging: ", key + diffs);
                        count++;
                    }
                }
                else {
                    Env.debug("identical for ", key);
                    identical++;
                }
            }
            else {
                Env.debug("Not updatable: ",key);
                blocked++;
            }
        }
        Env.info("   updated:", ""+count);
        Env.info("   identical: ",""+identical);
        Env.info("   not found in destination: ",""+missing);
        Env.info("   not active in source: ",""+notActive);
        Env.info("   blocked for updating: ",""+blocked);
        return count;
    }


    public int deleteInactiveRecords(Table srcTable, Table destTable) {
        Env.info("* Deleting inactive", "");
        int count = 0;
        for (Record src : srcTable.records()) {
            final String key = src.getKey();
            if (srcTable.isDeleteDesired(src) && destTable.mayDeleteRecord(key)) {
                if (Env.ask("About to delete "+key)) {
                    if (! dryRun)
                        destTable.delete(src);
                    Env.verbose("deleted ", src.getKey());
                    count++;
                }
            }
        }
        Env.info("   deleted because marked deleted in source:", ""+count);
        return count;
    }
    public int deleteMissingRecords(Table srcTable, Table destTable) {
        Env.info("* Deleting missing", "");
        int count=0;
        if (false) {
            for (Record dest : destTable.records()) {
                final String key = dest.getKey();
                if (destTable.mayDeleteRecord(key) && !srcTable.recordExists(key)) {
                    if (Env.ask("About to delete "+key)) {
                        if (! dryRun)
                            destTable.delete(dest);
                        Env.verbose("deleted record which does not exist at source ", key);
                        count++;
                    }

                }
            }
        }
        Env.info("   deleted because missing in source:", ""+count);
        return count;
    }
}
