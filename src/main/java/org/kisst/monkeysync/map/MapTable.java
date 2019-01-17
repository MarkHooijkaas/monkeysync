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
