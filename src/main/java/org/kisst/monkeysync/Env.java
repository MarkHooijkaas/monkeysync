package org.kisst.monkeysync;

import org.kisst.monkeysync.json.JsonFileTable;
import org.kisst.monkeysync.mailchimp.MailchimpTable;
import org.kisst.monkeysync.map.MapTable;
import org.kisst.monkeysync.sql.SqlTable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

public class Env {
    static private final LinkedHashMap<String, Table> tables=new LinkedHashMap<>();
    static public final Props props=new Props();
    static public int verbosity=1;
    static public boolean interactive=false;
    static public boolean enableDeleteMissingRecords =false;

    static public void loadProps(Path p) {
        info("loading props from: ",p.toString());
        props.loadProps(p);
    }

    static public void info(String s, String key) {
        if (verbosity>=1)
            System.out.println(s+key);
    }
    static public void verbose(String s, String key) {
        if (verbosity>=2)
            System.out.println(s+key);
    }
    static public void debug(String s, String key) {
        if (verbosity>=3)
            System.out.println(s+key);
    }
    static public boolean ask(String s) {
        if (interactive || verbosity>=1)
            System.out.println(s);
        if (interactive) {
            System.console().readLine();
        }
        return true;
    }

    static public Table getTable(String name) {
        if (tables.get(name)==null) {
            verbose("Loading table ",name);
            tables.put(name, loadTable(props.getProps(name)));
            info("Loaded "+tables.get(name).size()+" records from ", name);
        }
        return tables.get(name);
    }

    static private Table loadTable(Props props) {
        String type=props.getString("type");
        if ("SqlTable".equals(type))
            return new SqlTable(props,"");
        if ("MailchimpTable".equals(type))
            return new MailchimpTable(props);
        if ("MapTable".equals(type))
            return new MapTable();
        if ("JsonFileTable".equals(type))
            return new JsonFileTable(props);
        throw new RuntimeException("Unknow table type "+type);
    }

    public static void loadTable(String name, String filename) {
        JsonFileTable table=new JsonFileTable(Paths.get(props.getString("path")), props.getProps(name));
        tables.put(name, table);
    }
}
