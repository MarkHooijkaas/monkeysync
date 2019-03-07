package org.kisst.monkeysync;

import org.kisst.script.Context;

import java.util.HashSet;
import java.util.LinkedHashMap;

public class Syncer {
    private boolean enableDeleteMissingRecords =false;
    private final boolean dryRun;
    private final HashSet<String> ignoreFields=new HashSet<>();

    public Syncer(Context ctx){
        // TODO: pass props
        String str=ctx.props.getString("sync.ignoreFields",null);
        if (str!=null) {
            for (String field : str.split(","))
                ignoreFields.add(field.trim());
        }
        this.dryRun=ctx.props.getBoolean("sync.dryRun", true);
    }

    /**
     * This method will sync all records from the source into the destination.
     * To be more precise, it will
     * - delete any records in the source that are marked as inactive in the source
     * - update any records in the source for which one or more of the fields in the source record does not match the field in the destination
     * - create any records that exist only in the source, but not (yet) in the destination
     * Note: if the destination record has any fields that are not know in the source record they are ignored (they are not deleted)
     */
    public void syncAll(Context ctx, Table srcTable, Table destTable) {
        updateRecords(ctx, srcTable,destTable);
        createNewRecords(ctx, srcTable,destTable);
        deleteInactiveRecords(ctx, srcTable,destTable);
        if (enableDeleteMissingRecords)
            deleteMissingRecords(ctx, srcTable,destTable);
    }

    public int createNewRecords(Context ctx, Table srcTable, Table destTable) {
        ctx.info("* Creating");
        int count=0;
        int inactive=0;
        for (Record src : srcTable.records()) {
            final String key = src.getKey();
            if (!srcTable.isActive(src)) {
                inactive++;
                continue;
            }
            if (destTable.mayCreateRecord(key)) {
                ctx.verbose("creating {} {}", key, src);
                if (ctx.ask("About to create "+key)) {
                    if (! dryRun)
                        destTable.create(src);
                    count++;
                }
            }
        }
        ctx.info("   created: {}", count);
        ctx.info("   inactive: {}", inactive);
        if (! dryRun)
            destTable.autoSave(ctx);
        return count;
    }

    public int updateRecords(Context ctx, Table srcTable, Table destTable) {
        ctx.info("Updating");
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
                    ctx.verbose("merging: {} {}", key, diffs);
                    if (ctx.ask("About to merge "+key)) {
                        if (! dryRun)
                            destTable.update(dest, diffs);
                        count++;
                    }
                }
                else {
                    ctx.debug("identical for {}", key);
                    identical++;
                }
            }
            else {
                ctx.debug("Not updatable: {}",key);
                blocked++;
            }
        }
        ctx.info("   updated: {}", count);
        ctx.info("   identical: {}",identical);
        ctx.info("   not found in destination: ",missing);
        ctx.info("   not active in source: {}",notActive);
        ctx.info("   blocked for updating: {}",blocked);
        if (! dryRun)
            destTable.autoSave(ctx);
        return count;
    }


    public int deleteInactiveRecords(Context ctx, Table srcTable, Table destTable) {
        ctx.info("* Deleting inactive");
        int count = 0;
        for (Record src : srcTable.records()) {
            final String key = src.getKey();
            if (srcTable.isDeleteDesired(src) && destTable.mayDeleteRecord(key)) {
                ctx.verbose("deleting {}", key);
                if (ctx.ask("About to delete "+key)) {
                    if (! dryRun)
                        destTable.delete(src);
                    count++;
                }
            }
        }
        ctx.info("   deleted because marked deleted in source: {}", count);
        if (! dryRun)
            destTable.autoSave(ctx);
        return count;
    }
    public int deleteMissingRecords(Context ctx, Table srcTable, Table destTable) {
        ctx.info("* Deleting missing");
        int count=0;
        if (false) {
            for (Record dest : destTable.records()) {
                final String key = dest.getKey();
                if (destTable.mayDeleteRecord(key) && !srcTable.recordExists(key)) {
                    ctx.verbose("deleting missing record {}", key);
                    if (ctx.ask("About to delete "+key)) {
                        if (! dryRun)
                            destTable.delete(dest);
                        count++;
                    }

                }
            }
        }
        ctx.info("   deleted because missing in source: {}", count);
        if (! dryRun)
            destTable.autoSave(ctx);
        return count;
    }
}
