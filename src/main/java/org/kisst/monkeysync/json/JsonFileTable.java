package org.kisst.monkeysync.json;

import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.map.MapTable;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonFileTable extends MapTable {
    private final Path path;

    public JsonFileTable(Props props) {
        this(Paths.get(props.getString("path")), props);
    }

    public JsonFileTable(Path path, Props props) {
        this.path= path;
        readJsonFile(path);
    }

}
