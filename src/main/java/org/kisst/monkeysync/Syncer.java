package org.kisst.monkeysync;

import org.kisst.monkeysync.json.JsonFileTable;
import org.kisst.monkeysync.mailchimp.MailchimpTable;
import org.kisst.monkeysync.map.MapTable;
import org.kisst.monkeysync.sql.SqlTable;

import java.util.LinkedHashMap;

public class Syncer {
    private boolean quiet=false;
    private boolean verbose=false;
    private boolean debug=false;
    private boolean interactive=false;
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
        info("created :", ""+count);

        count=updateRecords(srcdb,destdb);
        info("updated:", ""+count);

        count=deleteInactiveRecords(srcdb,destdb);
        info("deleted :", ""+count);

        if (enableDeleteMissingRecords) {
            count=deleteMissingRecords(srcdb,destdb);
            info("deleted missing records:", ""+count);
        }
    }

    public int createNewRecords(Table srcdb, Table destdb) {
        int count=0;
        for (Record src : srcdb.records()) {
            final String key = src.getKey();
            if (destdb.mayCreateRecord(key)) {
                destdb.create(src);
                verbose("created ",src.getKey());
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
                    verbose("merging: ",key);
                    count++;
                }
                else {
                    debug("identical for ", key);
                    identical++;
                }
            }
        }
        debug("Total identical: ",""+identical);
        return count;
    }


    public int deleteInactiveRecords(Table srcdb, Table destdb) {
        int count = 0;
        for (Record src : srcdb.records()) {
            final String key = src.getKey();
            if (srcdb.isDeleteDesired(key) && destdb.mayDeleteRecord(key)) {
                destdb.delete(src);
                verbose("deleted ", src.getKey());
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
                    verbose("deleted record which does not exist at source ",key);
                    count++;

                }
            }
        }
        return count;
    }



    public void setDebug(boolean b) { this.debug=b;}
    public void setVerbose(boolean b) { this.verbose=b;}
    public void setInteractive(boolean b) { this.interactive=b;}

    public void info(String s, String key) {
        if (!quiet )
            System.out.println(s+key);
    }
    public void verbose(String s, String key) {
        if (!quiet && verbose || debug)
            System.out.println(s+key);
    }
    public void debug(String s, String key) {
        if (!quiet && debug)
            System.out.println(s+key);
    }
    public boolean ask(String s) {
        if (verbose || interactive ||debug)
            System.out.println(s);
        if (interactive) {
            System.console().readLine();
        }
        return true;
    }

    public Table getTable(Props props) {
        String type=props.getString("type");
        if ("SqlTable".equals(type))
            return new SqlTable(props);
        if ("MailchimpTable".equals(type))
            return new MailchimpTable(props);
        if ("MapTable".equals(type))
            return new MapTable();
        if ("JsonFileTable".equals(type))
            return new JsonFileTable(props);
        throw new RuntimeException("Unknow table type "+type);
    }
}
