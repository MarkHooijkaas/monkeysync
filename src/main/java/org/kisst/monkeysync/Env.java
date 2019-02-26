package org.kisst.monkeysync;

import org.kisst.monkeysync.mailchimp.MailchimpTable;
import org.kisst.monkeysync.map.MapTable;
import org.kisst.monkeysync.sql.SqlTable;

import java.nio.file.Path;
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

    public static void warn(String msg) {
        System.out.println("WARNING: "+msg);
    }
    static public void info(String s, String key) {
        if (verbosity>=1)
            System.out.println(s+key);
    }
    static public void verbose(String s, String key) {
        if (verbosity>=2)
            System.out.println(s+key);
    }
    static public void debug(String s, Object data) {
        if (verbosity>=3)
            System.out.println(s+data);
    }
    static public boolean ask(String s) {
        if (interactive) {
            System.out.println(s);
            String line=System.console().readLine();
            if (line.trim().toLowerCase().startsWith("n"))
                return false;
        }
        return true;
    }

    static public Table getTable(String name) {
        if (tables.get(name)==null) {
            verbose("Loading table ",name);
            tables.put(name, loadTable(name));
            info("Loaded "+tables.get(name).size()+" records from ", name);
        }
        return tables.get(name);
    }

    static public Table loadTable(String name) {
        Table t=createTable(name);
        t.load();
        tables.put(name, t);
        return t;
    }
    static public Table loadTable(String name, String filename) {
        Table t=createTable(name);
        t.load(filename);
        tables.put(name, t);
        return t;
    }
    static public Table fetchTable(String name) {
        Table t=createTable(name);
        t.fetch();
        tables.put(name, t);
        return t;
    }
    static public Table createTable(String name) {
        Props tblprops=props.getProps(name);
        String type=tblprops.getString("type");
        if ("SqlTable".equals(type))
            return new SqlTable(tblprops);
        if ("MailchimpTable".equals(type))
            return new MailchimpTable(tblprops);
        if ("MapTable".equals(type))
            return new MapTable(tblprops);
        throw new RuntimeException("Unknown table type "+type);
    }

}
