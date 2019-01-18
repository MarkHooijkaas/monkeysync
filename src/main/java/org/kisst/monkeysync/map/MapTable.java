package org.kisst.monkeysync.map;

import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.Record;

import java.util.LinkedHashMap;

public class MapTable extends BaseTable<MapRecord> {
    public MapTable(Props props) {
        super(props);
    }

    @Override protected MapRecord createRecord(String json) {
        LinkedHashMap<String, String> map = gson.fromJson(json, LinkedHashMap.class);
        return new MapRecord(map);
    }

    @Override protected MapRecord createRecord(Record rec) { return new MapRecord(rec);}
}
