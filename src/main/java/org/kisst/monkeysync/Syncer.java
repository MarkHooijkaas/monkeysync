package org.kisst.monkeysync;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kisst.script.Config;
import org.kisst.script.Context;

import java.util.HashSet;
import java.util.LinkedHashMap;

public class Syncer {
    private static final Logger logger= LogManager.getLogger();
    private final Config cfg;

    private boolean enableDeleteMissingRecords =false;
    private final boolean dryRun;
    private final HashSet<String> ignoreFields=new HashSet<>();

    public Syncer(Config cfg){
        this.cfg=cfg;
        String str=cfg.props.getString("sync.ignoreFields",null);
        if (str!=null) {
            for (String field : str.split(","))
                ignoreFields.add(field.trim());
        }
        this.dryRun=cfg.props.getBoolean("sync.dryRun", true);
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
        logger.info("* Creating");
        int count=0;
        int inactive=0;
        for (Record src : srcTable.records()) {
            final String key = src.getKey();
            if (!srcTable.isActive(src)) {
                inactive++;
                continue;
            }
            if (destTable.mayCreateRecord(key)) {
                logger.log(Context.VERBOSE, "creating {} {}", key, src);
                if (cfg.ask("About to create "+key)) {
                    if (! dryRun)
                        destTable.create(src);
                    count++;
                }
            }
        }
        logger.info("   created: {}", count);
        logger.info("   inactive: {}", inactive);
        if (! dryRun)
            destTable.autoSave();
        return count;
    }

    public int updateRecords(Table srcTable, Table destTable) {
        logger.info("Updating");
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
                    logger.log(Context.VERBOSE, "merging: {} {}", key, diffs);
                    if (cfg.ask("About to merge "+key)) {
                        if (! dryRun)
                            destTable.update(dest, diffs);
                        count++;
                    }
                }
                else {
                    logger.debug("identical for {}", key);
                    identical++;
                }
            }
            else {
                logger.debug("Not updatable: {}",key);
                blocked++;
            }
        }
        logger.info("   updated: {}", count);
        logger.info("   identical: {}",identical);
        logger.info("   not found in destination: ",missing);
        logger.info("   not active in source: {}",notActive);
        logger.info("   blocked for updating: {}",blocked);
        if (! dryRun)
            destTable.autoSave();
        return count;
    }


    public int deleteInactiveRecords(Table srcTable, Table destTable) {
        logger.info("* Deleting inactive");
        int count = 0;
        for (Record src : srcTable.records()) {
            final String key = src.getKey();
            if (srcTable.isDeleteDesired(src) && destTable.mayDeleteRecord(key)) {
                logger.log(Context.VERBOSE,"deleting {}", key);
                if (cfg.ask("About to delete "+key)) {
                    if (! dryRun)
                        destTable.delete(src);
                    count++;
                }
            }
        }
        logger.info("   deleted because marked deleted in source: {}", count);
        if (! dryRun)
            destTable.autoSave();
        return count;
    }
    public int deleteMissingRecords(Table srcTable, Table destTable) {
        logger.info("* Deleting missing");
        int count=0;
        if (false) {
            for (Record dest : destTable.records()) {
                final String key = dest.getKey();
                if (destTable.mayDeleteRecord(key) && !srcTable.recordExists(key)) {
                    logger.log(Context.VERBOSE,"deleting missing record {}", key);
                    if (cfg.ask("About to delete "+key)) {
                        if (! dryRun)
                            destTable.delete(dest);
                        count++;
                    }

                }
            }
        }
        logger.info("   deleted because missing in source: {}", count);
        if (! dryRun)
            destTable.autoSave();
        return count;
    }
}
