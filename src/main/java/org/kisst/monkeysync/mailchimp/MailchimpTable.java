package org.kisst.monkeysync.mailchimp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.Record;
import org.kisst.monkeysync.Table;
import org.kisst.monkeysync.json.JsonHelper;
import org.kisst.monkeysync.map.BaseTable;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MailchimpTable extends BaseTable<MailchimpRecord> implements MailchimpConnector.MemberInserter {
    private final MailchimpConnector connector;
    public boolean updatesAllowed=false;
    private int maxsize;

    public MailchimpTable(Props props) {
        super(new LinkedHashMap<>());
        this.connector = new MailchimpConnector(props);
        this.maxsize= props.getInt("maxsize", 130);
        if (props.getBoolean("autoload", true))
            retrieveAllMembers();
    }

    PrintWriter out;
    public void retrieveAllMembers() {
        try (FileOutputStream f=new FileOutputStream("mc-log.json")) {
            out= new PrintWriter(f);
            connector.insertAllMembers(this, 37000, 1000); // Not too big, since there is still ssome arihtmetic done
            out.close();
        }
        catch (IOException e) {throw  new RuntimeException(e); }
    }
    @Override public void insert(MailchimpRecord rec) {
        records.put(rec.getKey(),rec);
        out.println(rec.toJson());
    }


    @Override protected MailchimpRecord createRecord(String json) {
        //LinkedHashMap<String, String> map = gson.fromJson(json, LinkedHashMap.class);
        return new MailchimpRecord(json);
    }
    @Override protected MailchimpRecord createRecord(Record rec) { return new MailchimpRecord(rec);}




    @Override public void create(Record srcrec) {
        if (updatesAllowed)
            connector.createMember(srcrec.getKey(), JsonHelper.toJson(srcrec));
    }

    @Override public void update(Record destrec, Map<String, String> diffs) {
        if (updatesAllowed)
            connector.updateMemberFields(destrec.getKey(), JsonHelper.toJson(diffs));
    }

    @Override public void delete(Record destrec) {
        if (updatesAllowed)
            connector.unsubscribeMember(destrec.getKey());
    }

}
