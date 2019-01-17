package org.kisst.monkeysync.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.kisst.monkeysync.Record;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapRecord implements Record {
    private final String key;
    private final LinkedHashMap<String,String> fields;

    public MapRecord(LinkedHashMap<String,String> m) {
        this.key=m.values().iterator().next();
        this.fields=new LinkedHashMap<>(m);
    }
    public MapRecord(String key, Map<String,String> m) {
        this.key = key;
        this.fields=new LinkedHashMap<>(m);
    }

    public MapRecord(Record rec) {
        this.fields=new LinkedHashMap<>();
        this.key=rec.getKey();
        for (String fld: rec.fieldNames())
            fields.put(fld, rec.getField(fld));
    }

    private final static Gson gson = new GsonBuilder().create();
    public String toJson() { return gson.toJson(fields); }

    @Override public String getKey() { return key;}
    @Override public Iterable<String> fieldNames() { return fields.keySet();}
    @Override public String getField(String name) { return fields.get(name);}
    @Override public void   setField(String name, String value) { fields.put(name, value);}

}