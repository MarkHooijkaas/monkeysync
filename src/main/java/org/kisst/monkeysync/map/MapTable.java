package org.kisst.monkeysync.map;

import org.kisst.monkeysync.Record;

import java.util.LinkedHashMap;

public class MapTable extends BaseTable<MapRecord> {
    private final LinkedHashMap<String, Record> records = new LinkedHashMap<>();

    public MapTable() {
        super(new LinkedHashMap<>());
    }

    @Override protected MapRecord createRecord(String json) {
        LinkedHashMap<String, String> map = gson.fromJson(json, LinkedHashMap.class);
        return new MapRecord(map);
    }

    @Override protected MapRecord createRecord(Record rec) { return new MapRecord(rec);}
}
