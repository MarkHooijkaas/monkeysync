package org.kisst.monkeysync;

import org.kisst.monkeysync.json.JsonFileTable;
import org.kisst.monkeysync.mailchimp.MailchimpTable;
import org.kisst.monkeysync.map.MapTable;
import org.kisst.monkeysync.sql.SqlTable;

import java.util.HashSet;
import java.util.LinkedHashMap;

import static sun.security.jgss.GSSToken.debug;

public class Syncer {
    private final HashSet<String> handled = new HashSet<>();
    private boolean verbose=false;
    private boolean debug=false;
    private boolean interactive=false;

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
                info("skipping blocked destination: ",key);
                handled.add(key);
                continue;
            }
            if (! srcdb.recordExists(key))
                continue;
            Record src = srcdb.getRecord(key);
            if (! srcdb.deleteDesired(key)) {
                info("deleting destination: ",key);
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
            if (diffs.size()>0) {
                destdb.update(dest, diffs);
                info("merging: ",key);
            }
            else
                debug("identical for ",key);
        }
        for (Record src: srcdb.records()) {
            if (!handled.contains(src.getKey()))
                destdb.create(src);
        }
    }

    public void setDebug(boolean b) { this.debug=b;}
    public void setVerbose(boolean b) { this.verbose=b;}
    public void setInteractive(boolean b) { this.interactive=b;}

    public void info(String s, String key) {
        if (verbose || verbose)
            System.out.println(s+key);
    }
    public void debug(String s, String key) {
        if (debug)
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

}
