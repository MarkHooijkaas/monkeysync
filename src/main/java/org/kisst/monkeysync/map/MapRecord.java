package org.kisst.monkeysync.map;

import org.json.simple.JSONObject;
import org.kisst.monkeysync.DestRecord;
import org.kisst.monkeysync.Record;
import org.kisst.monkeysync.SourceRecord;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapRecord implements DestRecord, SourceRecord {
    private final LinkedHashMap<String,String> fields;
    private final String key;

    public MapRecord(String key, Map<String,String> m) {
        fields=new LinkedHashMap<>(m);
        this.key = key;
    }

    public MapRecord(String key) {
        this.fields=new LinkedHashMap<>();
        this.key = key;
    }

    public MapRecord(String key, Record rec) {
        this.fields=new LinkedHashMap<>();
        this.key=key;
        for (String fld: rec.fieldNames())
            fields.put(fld, rec.getField(fld));
    }

    @Override public String getKey() { return key;}
    @Override public boolean blocked() { return false;}
    @Override public boolean deleted() { return false;}
    @Override public Iterable<String> fieldNames() { return fields.keySet();}
    @Override public String getField(String name) { return fields.get(name);}
    public void merge(Map<String, String> diffs) {
        for (String key: diffs.keySet())
            fields.put(key, diffs.get(key));
    }
}
