package org.kisst.monkeysync.map;

import org.json.simple.JSONObject;
import org.kisst.monkeysync.DestRecord;
import org.kisst.monkeysync.Record;
import org.kisst.monkeysync.SourceRecord;

import java.util.HashMap;
import java.util.Map;

public class MapRecord extends HashMap<String,String> implements DestRecord, SourceRecord {
    private final String key;

    public MapRecord(String key, Map m) {
        super(m);
        this.key = key;
    }

    public MapRecord(String key) {
        this.key = key;
    }

    public MapRecord(String key, Record rec) {
        this.key=key;
        for (String fld: rec.fieldNames())
            put(fld, rec.getField(fld));
    }

    @Override public String getKey() { return key;}
    @Override public boolean blocked() { return false;}
    @Override public boolean deleted() { return false;}
    @Override public Iterable<String> fieldNames() { return keySet();}
    @Override public String getField(String name) { return get(name);}
    public void merge(Map<String, String> diffs) {
        for (String key: diffs.keySet())
            put(key, diffs.get(key));
    }
}
