package org.kisst.monkeysync.map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kisst.monkeysync.*;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapSource implements RecordSource, RecordDestination {
    private final LinkedHashMap<String, MapRecord> records = new LinkedHashMap<>();
    private final HashSet<String> handled = new HashSet<>();

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
            srcrec = new MapRecord(srcrec.getKey(), srcrec);
        records.put(srcrec.getKey(), (MapRecord) srcrec);}

    @Override public void update(DestRecord destrec, Map<String, String> diffs) {
        MapRecord newrec = getRecord(destrec.getKey());
        newrec.merge(diffs);
        create(newrec);
    }
    @Override public void delete(DestRecord destrec) { records.remove(destrec.getKey());}

    public void readJsonFile(String filename, String keyfield) {
        JSONParser parser = new JSONParser();
        try {
            JSONArray arr = (JSONArray) parser.parse(new FileReader(filename));
            for (Object it: arr) {
                JSONObject obj= (JSONObject) it;
                create(new MapRecord((String) obj.get(keyfield), obj));
            }
        }
        catch (IOException e) { throw new RuntimeException(e);}
        catch (ParseException e) { throw new RuntimeException(e);}
    }

}
