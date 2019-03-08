package org.kisst.monkeysync.map;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kisst.monkeysync.CachedObject;
import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.Record;
import org.kisst.monkeysync.Table;
import org.kisst.script.Context;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public abstract class BaseTable<R extends Record> implements Table, CachedObject {
    private static final Logger logger= LogManager.getLogger();

    protected final Props props;
    private final LinkedHashMap<String, String> fieldNames=new LinkedHashMap<>();
    protected final LinkedHashMap<String, R> records=new LinkedHashMap<>();
    protected final String file;
    protected final boolean autoSave;
    private final String statusField;
    private final Set<String> statusActiveValues;
    private final Set<String> statusDeletedValues;
    private final boolean statusCaseSensitive;
    private final boolean keyIsCaseInsensitive;

    public BaseTable(Props props) {
        this.props=props;
        this.keyIsCaseInsensitive=props.getBoolean("keyIsCaseInsensitive", true);
        this.file = props.getString("file",null);
        //if (file!=null)
        //    load(Paths.get(file));
        this.autoSave= props.getBoolean("autoSave",true);
        this.statusField =props.getString("statusField",null);
        this.statusCaseSensitive =props.getBoolean("statusField",true);
        this.statusActiveValues=props.getStringSet("statusActiveValues");
        this.statusDeletedValues=props.getStringSet("statusDeletedValues");
        if (! statusCaseSensitive) {
            for(String s: statusDeletedValues)
                statusDeletedValues.add(s.toUpperCase());
            for(String s: statusActiveValues)
                statusActiveValues.add(s.toUpperCase());
        }
    }

    protected String key(String key) { if (keyIsCaseInsensitive) return key.toLowerCase(); return key; }
    protected String key(Record rec) { if (keyIsCaseInsensitive) return rec.getKey().toLowerCase(); return rec.getKey(); }

    @Override public boolean recordExists(String key)  {
        return records.containsKey(key(key));
    }
    public boolean isActive(Record rec) {
        if (statusField==null)
            return true;
        if (rec==null)
            return false;
        String status=rec.getField(statusField);
        if (status!=null && statusCaseSensitive)
            status=status.toUpperCase();
        return statusActiveValues.contains(status);
    }

    public void addFieldName(String name) { fieldNames.put(name,name);}
    @Override public boolean isDeleteDesired(Record rec) {
        if (statusField==null)
            return false;
        if (rec==null)
            return false;
        String status=rec.getField(statusField);
        if (status!=null && statusCaseSensitive)
            status=status.toUpperCase();
        return statusDeletedValues.contains(status);
    }
    @Override public boolean mayDeleteRecord(String key) { return isActive(getRecord(key)); }
    @Override public boolean mayUpdateRecord(String key) { return isActive(getRecord(key)); }
    @Override public boolean mayCreateRecord(String key) { return ! recordExists(key); }


    @Override public Record getRecord(String key) {
        return records.get(key(key));
    }
    @Override public int size() { return records.size();}

    @Override public Iterable<Record> records() {
        ArrayList<Record> result = new ArrayList<>();
        for (Record record : records.values())
               result.add(record);
        return result;
    }

    abstract protected R createRecord(String json);
    abstract protected R createRecord(Record rec);

    @Override public void create(Record srcrec) {
        records.put(key(srcrec), createRecord(srcrec));}

    @Override public void update(Record destrec, Map<String, String> diffs) {
        for (String field: diffs.keySet())
           destrec.setField(field, diffs.get(field));
    }
    @Override public void delete(Record destrec) { records.remove(key(destrec));}

    protected final static Gson gson = new Gson();

    @Override public void load() {
        if (file==null)
            throw new IllegalArgumentException("No file configured to load table");
        load(file);
    }
    public void load(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                //System.out.println(line);
                if (line.trim().length() > 0) {
                    R rec = createRecord(line);
                    records.put(key(rec), rec);
                }
            }
        }
        catch (IOException e) { throw new RuntimeException(e);}
        logger.info("loaded {} records from {}",records.size(), filename);
    }

    @Override public void autoSave() {
        if (autoSave && file!=null)
          save(file);
    }
    @Override public void save() {
        if (file==null)
            throw new IllegalArgumentException("No file configured to save table");
        save(file);
    }
    @Override public void save(String filename) {
        try (FileWriter file = new FileWriter(filename)) {
            for (Record rec: records.values()) {
                file.write(rec.toJson());
                file.write('\n');
            }
        }
        catch (IOException e) { throw new RuntimeException(e);}
        logger.info("Saved {} records to {}",records.size(),filename);
    }

    public void saveTabDelimited(Context ctx, String filename) {
        try (FileWriter file = new FileWriter(filename)) {
            for (Record rec: records.values()) {
                String sep="";
                for (String name : fieldNames.keySet()) {
                    String value=rec.getField(name);
                    if (value!=null)
                        value=value.replaceAll("[\t\n]"," ");
                    else
                        value="";
                    file.write(sep+value);
                    sep="\t";
                }
                file.write('\n');
            }
        }
        catch (IOException e) { throw new RuntimeException(e);}
        logger.info("Saved {} records to {}",records.size(),filename);
    }
}
