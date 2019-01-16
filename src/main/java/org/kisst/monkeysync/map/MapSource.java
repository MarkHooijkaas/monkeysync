package org.kisst.monkeysync.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.kisst.monkeysync.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapSource implements RecordSource, RecordDestination {
    private final LinkedHashMap<String, MapRecord> records = new LinkedHashMap<>();
    private final HashSet<String> handled = new HashSet<>();

    public MapSource() {}

    public MapSource(File file) {
        readJsonFile(file);
    }

    @Override public boolean recordExists(String key) { return records.containsKey(key);}
    @Override public MapRecord getRecord(String key) { return records.get(key);}

    @Override public Iterable<DestRecord> records() {
        ArrayList<DestRecord> result = new ArrayList<>();
        for (DestRecord record : records.values())
               result.add(record);
        return result;
    }


    @Override public void markAsHandled(String key) { handled.add(key);}
    @Override public Iterable<SourceRecord>  unhandledRecords() {
        ArrayList<SourceRecord> result= new ArrayList<>();
        for (SourceRecord record: records.values()) {
            if (! handled.contains(record.getKey()))
                result.add(record);
        }
        return result;
    }

    @Override public void create(SourceRecord srcrec) {
        if (! (srcrec instanceof MapRecord))
            srcrec = new MapRecord(srcrec);
        records.put(srcrec.getKey(), (MapRecord) srcrec);}

    @Override public void update(DestRecord destrec, Map<String, String> diffs) {
        MapRecord newrec = getRecord(destrec.getKey());
        newrec.merge(diffs);
        create(newrec);
    }
    @Override public void delete(DestRecord destrec) { records.remove(destrec.getKey());}

    public void readJsonFile(File f) {
        try (FileReader r=new FileReader(f)){
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(r);
            reader.beginArray();
            while (reader.hasNext()) {
                LinkedHashMap<String,String> map = gson.fromJson(reader, LinkedHashMap.class);
                create(new MapRecord(map));
            }
            reader.endArray();
            reader.close();
        }
        catch (IOException e) { throw new RuntimeException(e);}
    }

    public void writeJsonFile(String filename) {
        try (FileWriter file = new FileWriter(filename)) {
            Gson gson = new GsonBuilder().create();
            for (MapRecord rec: records.values()) {
                file.write(rec.toJson());
                file.write('\n');
            }
        }
        catch (IOException e) { throw new RuntimeException(e);}
    }
}
