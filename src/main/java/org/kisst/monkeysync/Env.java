package org.kisst.monkeysync;

import org.kisst.monkeysync.mailchimp.MailchimpTable;
import org.kisst.monkeysync.map.MapTable;
import org.kisst.monkeysync.sql.SqlTable;

import java.nio.file.Path;
import java.util.LinkedHashMap;

public class Env {
    static private final LinkedHashMap<String, Object> objects=new LinkedHashMap<>();
    static private final LinkedHashMap<String, Table> tables=new LinkedHashMap<>();
    static public final Props props=new Props();
    static public int verbosity=1;
    static public int memoryLevel=2;
    static public boolean interactive=false;

    static public void loadProps(Path p) {
        info("loading props from: ",p.toString());
        props.loadProps(p);
    }


    static public void warn(String msg, Object... params) {
        output("WARN",0, msg,params);
    }
    static public void info(String msg, Object... params) {
        output("INFO",1, msg, params);
    }
    static public void verbose(String msg, Object... params) { output("TRACE",2, msg, params);    }
    static public void debug(String msg, Object... params) {
        output("DEBUG",3, msg, params);
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

    static private void output(String levelname, int level, String line, Object... parms) {
        if (verbosity<level && memoryLevel<level)
            return;
        for (Object parm:parms) {
            if (parm != null)
                line += " " + parm;
        }
        if (verbosity>=level)
            System.out.println(line);
        if (memoryLevel>=level) {
            StringBuilder buffer=getBuffer(levelname);
            buffer.append(line+"\n");
        }
    }

    private static StringBuilder getBuffer(String levelname) {
        StringBuilder result= (StringBuilder) getObject(levelname);
        if (result==null) {
            result=new StringBuilder();
            objects.put(levelname, result);
        }
        return result;
    }
    public static Object getObject(String name) { return objects.get(name);}


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
        t.fetch(null);
        tables.put(name, t);
        return t;
    }
    static public Table createTable(String name) {
        Props tblprops=props.getProps(name);
        String type=tblprops.getString("type");
        if ("SqlTable".equals(type))
            return new SqlTable(null, tblprops);
        if ("MailchimpTable".equals(type))
            return new MailchimpTable(tblprops);
        if ("MapTable".equals(type))
            return new MapTable(tblprops);
        throw new RuntimeException("Unknown table type "+type);
    }

    static  public String substitute(String str) {
        return StringUtil.substitute(str, props.props, objects, tables);
    }

    public static void clear() {
        objects.clear();
        tables.clear();
        // TODO: set props to initial state;
    }

    static public void sleep(long msecs) {
        try {
            Thread.sleep(msecs);
        }
        catch (InterruptedException e1) { throw new RuntimeException(e1);}
    }

}
