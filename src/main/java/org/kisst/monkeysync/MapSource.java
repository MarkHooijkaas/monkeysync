package org.kisst.monkeysync;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class MapSource implements RecordSource {
    //private final ArrayList<String> fieldNames = new ArrayList<>();
    private final LinkedHashMap<String, Record> records = new LinkedHashMap<>();
    private final HashSet<String> handled = new HashSet<>();

    //public void addField(String name) { fieldNames.add(name);}
    public void add(Record rec) { records.put(rec.getKey(), rec);}
    @Override public boolean recordExists(String key) { return records.containsKey(key);}
    @Override public Record getRecord(String key) { return records.get(key);}

    //@Override public Iterable<String> fieldNames() { return fieldNames;} // TODO: Should be immutable or clone

    @Override public void markAsHandled(String key) { handled.add(key);}
    @Override public Iterable<Record>  unhandledRecords() {
        ArrayList<Record> result= new ArrayList<>();
        for (Record record: records.values()) {
            if (! handled.contains(record.getKey()))
                result.add(record);
        }
        return result;
    }
}
