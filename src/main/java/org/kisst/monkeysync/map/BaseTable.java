package org.kisst.monkeysync.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.Record;
import org.kisst.monkeysync.Table;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseTable<R extends Record> implements Table {
    protected final LinkedHashMap<String, R> records=new LinkedHashMap<>();
    protected final String file;
    protected final boolean autoFetch;


    public BaseTable(Props props) {
        this.file = props.getString("file",null);
        if (file!=null)
            load(Paths.get(file));
        this.autoFetch = props.getBoolean("autoFetch",file==null);
    }

    @Override public boolean recordExists(String key)  { return records.containsKey(key);}
    @Override public boolean isDeleteDesired(String key) { return false; }
    @Override public boolean mayDeleteRecord(String key) { return recordExists(key); }
    @Override public boolean mayUpdateRecord(String key) { return recordExists(key); }
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

    public void load(Path p) {
        try {
            Files.lines(p).forEach(line ->{
                if (line.trim().length()>0) {
                    create(createRecord(line));
                }
            });
        }
        catch (IOException e) { throw new RuntimeException(e);}
    }

    public void save(String filename) {
        try (FileWriter file = new FileWriter(filename)) {
            Gson gson = new GsonBuilder().create();
            for (Record rec: records.values()) {
                file.write(rec.toJson());
                file.write('\n');
            }
        }
        catch (IOException e) { throw new RuntimeException(e);}
    }
}
