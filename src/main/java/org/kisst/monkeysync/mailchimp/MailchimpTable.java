package org.kisst.monkeysync.mailchimp;

import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.Record;
import org.kisst.monkeysync.json.JsonHelper;
import org.kisst.monkeysync.map.BaseTable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class MailchimpTable extends BaseTable<MailchimpRecord> implements MailchimpConnector.MemberInserter {
    private final MailchimpConnector connector;
    public boolean updatesAllowed=false;
    private final String necessaryInterest;
    private final int offset;
    private final int maxsize;

    public MailchimpTable(Props props) {
        super(props);
        this.connector = new MailchimpConnector(props);
        this.offset= props.getInt("offset", 0); // Not too big, since there is still ssome arihtmetic done
        this.maxsize= props.getInt("count", 999999); // Not too big, since there is still ssome arihtmetic done
        this.necessaryInterest = props.getString("necessaryInterest", null);
        if (this.autoFetch)
            retrieveAllMembers();
    }

    protected boolean mayRecordBeChanged(MailchimpRecord rec) {
        if (rec==null)
            return false;
        if ("unsubscribed".equals(rec.status))
            return false;
        if ("cleaned".equals(rec.status))
            return false;
        if ("pending".equals(rec.status))
            return false;
        if (necessaryInterest!=null) {
           Boolean b = rec.interests.get(necessaryInterest);
           if (b==null || b==false)
               return false;
        }
        return true;
    }

    @Override public boolean mayDeleteRecord(String key) {
        MailchimpRecord rec = records.get(key);
        if (! mayRecordBeChanged(rec))
            return false;
        return true;
    }
    @Override public boolean mayUpdateRecord(String key) {
        MailchimpRecord rec = records.get(key);
        if (! mayRecordBeChanged(rec))
            return false;
        return true;
    }

    PrintWriter out;
    public void retrieveAllMembers() {
        try (FileOutputStream f=new FileOutputStream("mc-log.json")) {
            out= new PrintWriter(f);
            connector.insertAllMembers(this, offset, maxsize);
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
    @Override protected MailchimpRecord createRecord(Record rec) { return new MailchimpRecord(rec, necessaryInterest);}




    @Override public void create(Record srcrec) {
        super.create(srcrec);
        if (updatesAllowed)
            connector.createMember(srcrec.getKey(), JsonHelper.toJson(srcrec));
    }

    @Override public void update(Record destrec, Map<String, String> diffs) {
        super.update(destrec, diffs);
        if (updatesAllowed)
            connector.updateMemberFields(destrec.getKey(), JsonHelper.toJson(diffs));
    }

    @Override public void delete(Record destrec) {
        super.delete(destrec);
        if (updatesAllowed)
            connector.unsubscribeMember(destrec.getKey());
    }

}
