package org.kisst.monkeysync;

import org.kisst.monkeysync.json.JsonFileTable;
import org.kisst.monkeysync.mailchimp.MailchimpTable;
import org.kisst.monkeysync.map.MapTable;
import org.kisst.monkeysync.sql.SqlTable;

import java.util.HashSet;
import java.util.LinkedHashMap;

public class Syncer {
    private final HashSet<String> handled = new HashSet<>();
    private boolean quiet=false;
    private boolean verbose=false;
    private boolean debug=false;
    private boolean interactive=false;
    private Table srcdb;
    private Table destdb;

    /**
     * This method will sync all records from the source into the destination.
     * To be more precise, it will
     * - delete any records in the source that are marked as inactive in the source
     * - update any records in the source for which one or more of the fields in the source record does not match the field in the destination
     * - create any records that exist only in the source, but not (yet) in the destination
     * Note: if the destination record has any fields that are not know in the source record they are ignored (they are not deleted)
     */
    public void sync() {
        int deleted=0;
        int identical=0;
        int merged=0;
        int created=0;
        int skipped=0;

        LinkedHashMap<String, String> diffs=new LinkedHashMap<>();
        for (Record dest : destdb.records()) {
            final String key=dest.getKey();
            handled.add(key);
            if (destdb.updateBlocked(key)) {
                verbose("skipping blocked destination: ",key);
                skipped++;
                continue;
            }
            if (! srcdb.recordExists(key))
                continue;
            Record src = srcdb.getRecord(key);
            if (srcdb.deleteDesired(key)) {
                verbose("deleting destination: ",key);
                destdb.delete(dest);
                deleted++;
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
            if (diffs.size()>0) {
                destdb.update(dest, diffs);
                verbose("merging: ",key);
                merged++;
            }
            else {
                debug("identical for ", key);
                identical++;
            }
        }
        for (Record src: srcdb.records()) {
            if (!handled.contains(src.getKey())) {
                destdb.create(src);
                verbose("created ",src.getKey());
                created++;
            }
        }
        info("created :", ""+created);
        info("deleted :", ""+deleted);
        info("merged :", ""+merged);
        info("skipped :", ""+skipped);
        info("identical:", ""+identical);

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

    public void setSource(String name, Props props) {
        verbose("Loading source ", name);
        srcdb=getTable(props);
        info("Loaded "+srcdb.size()+" records from ", name);
    }
    public void setDestination(String name, Props props) {
        verbose("Loading destination ", name);
        destdb=getTable(props);
        info("Loaded "+destdb.size()+" records from ", name);
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
