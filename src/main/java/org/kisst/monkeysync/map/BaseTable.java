package org.kisst.monkeysync.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.Record;
import org.kisst.monkeysync.Table;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public abstract class BaseTable<R extends Record> implements Table {
    protected final LinkedHashMap<String, R> records=new LinkedHashMap<>();
    protected final String file;
    protected final boolean autoFetch;
    private final String statusField;
    private final Set<String> statusActiveValues;
    private final Set<String> statusDeletedValues;
    private final boolean statusCaseSensitive;

    public BaseTable(Props props) {
        this.file = props.getString("file",null);
        if (file!=null)
            load(Paths.get(file));
        this.autoFetch = props.getBoolean("autoFetch",file==null);
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



    @Override public boolean recordExists(String key)  { return records.containsKey(key);}
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


    @Override public Record getRecord(String key) { return records.get(key);}
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
        records.put(srcrec.getKey(), createRecord(srcrec));}

    @Override public void update(Record destrec, Map<String, String> diffs) {
        for (String field: diffs.keySet())
           destrec.setField(field, diffs.get(field));
    }
    @Override public void delete(Record destrec) { records.remove(destrec.getKey());}

    protected final static Gson gson = new Gson();
    //Gson gson = new GsonBuilder().create();

    public void load(Path p) {
        try (BufferedReader br = new BufferedReader(new FileReader(p.toString()))) {
            String line;
            while ((line = br.readLine()) != null) {
                //System.out.println(line);
                if (line.trim().length() > 0) {
                    R rec = createRecord(line);
                    records.put(rec.getKey(), rec);
                }
            }
        }
        catch (IOException e) { throw new RuntimeException(e);}
    }

    public void save(String filename) {
        try (FileWriter file = new FileWriter(filename)) {
            for (Record rec: records.values()) {
                file.write(rec.toJson());
                file.write('\n');
            }
        }
        catch (IOException e) { throw new RuntimeException(e);}
    }
}
