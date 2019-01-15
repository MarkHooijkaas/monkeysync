package org.kisst.monkeysync.console;

import org.kisst.monkeysync.ActionHandler;
import org.kisst.monkeysync.JsonBuilder;
import org.kisst.monkeysync.Record;

import java.io.*;
import java.util.Map;

public class ConsoleActions implements ActionHandler {
    private final PrintStream out;

    public ConsoleActions(File f) {
        try {
            this.out=new PrintStream(new FileOutputStream(f));
        } catch (FileNotFoundException e) { throw new RuntimeException(e); }
    }

    public ConsoleActions() {
        this.out = System.out;
    }

    @Override
    public void create(Record rec) {
        out.println("creating:"+JsonBuilder.toJson(rec));
    }

    @Override
    public void update(Record rec, Map<String, String> diffs) {
        out.println("updating :"+JsonBuilder.toJson(rec)+ " with "+JsonBuilder.toJson(diffs));
    }

    @Override
    public void delete(Record rec) {
        out.println("deleting: "+rec.getKey());
    }

}
