package org.kisst.monkeysync.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.kisst.monkeysync.Record;
import org.kisst.monkeysync.Table;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseTable<R extends Record> implements Table {
    protected final LinkedHashMap<String, R> records;


    public BaseTable(LinkedHashMap<String, R> records) {
        this.records=records;
    }


    @Override public boolean recordExists(String key)  { return records.containsKey(key);}
    @Override public boolean deleteDesired(String key) { return false; }
    @Override public boolean updateBlocked(String key) { return false; }

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
        R newrec = createRecord(destrec);
        for (String field: diffs.keySet())
           newrec.setField(field, diffs.get(field));
        create(newrec);
    }
    @Override public void delete(Record destrec) { records.remove(destrec.getKey());}

    protected final static Gson gson = new Gson();

    public void readJsonFile(Path p) {
        try {
            Files.lines(p).forEach(line ->{
                if (line.trim().length()>0) {
                    create(createRecord(line));
                }
            });
        }
        catch (IOException e) { throw new RuntimeException(e);}
    }

    public void writeJsonFile(String filename) {
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