package org.kisst.monkeysync.json;

import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.map.MapTable;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonFileTable extends MapTable {
    private final Path path;

    public JsonFileTable(Props props) {
        this.path= Paths.get(props.getString("path"));
        readJsonFile(path);
    }

}
