package org.kisst.monkeysync.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.Record;
import org.kisst.monkeysync.Table;
import org.kisst.monkeysync.map.MapRecord;
import org.kisst.monkeysync.map.MapTable;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonFileTable extends MapTable {
    private final Path path;

    public JsonFileTable(Props props) {
        this.path= Paths.get(props.getString("path"));
        readJsonFile(path);
    }

}
