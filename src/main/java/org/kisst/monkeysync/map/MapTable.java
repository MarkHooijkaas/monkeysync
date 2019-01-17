package org.kisst.monkeysync.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.kisst.monkeysync.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapTable implements Table {
    private final LinkedHashMap<String, Record> records = new LinkedHashMap<>();


    public MapTable() {}

    public MapTable(Path path) {
        readJsonFile(path);
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

    @Override public void create(Record srcrec) {
        if (! (srcrec instanceof MapRecord))
            srcrec = new MapRecord(srcrec);
        records.put(srcrec.getKey(), (MapRecord) srcrec);}

    @Override public void update(Record destrec, Map<String, String> diffs) {
        Record newrec = new MapRecord(destrec.getKey());
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

    protected Record createRecord(String json) {
        LinkedHashMap<String, String> map = gson.fromJson(json, LinkedHashMap.class);
        return new MapRecord(map);
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
