package org.kisst.monkeysync.mailchimp;

import org.kisst.monkeysync.Env;
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


    public boolean hasAllNecessaryInterests(MailchimpRecord rec) {
        if (necessaryInterest!=null) {
            Boolean b = rec.interests.get(necessaryInterest);
            if (b==null || b==false)
                return false;
        }
        return true;
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
        return isActive(rec);
    }

    @Override public boolean mayDeleteRecord(String key) {
        MailchimpRecord rec = records.get(key);
        return  mayRecordBeChanged(rec) && hasAllNecessaryInterests(rec);
    }
    @Override public boolean mayUpdateRecord(String key) {
        MailchimpRecord rec = records.get(key);
        return mayRecordBeChanged(rec);
    }

    PrintWriter out; // logging file to save mailchimp retrieval if it crashes (e.g. Out of memory), and can't be saved
    public void retrieveAllMembers() {
        try (FileOutputStream f=new FileOutputStream("mailchimp-retrieval-log.json")) {
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
    @Override protected MailchimpRecord createRecord(Record rec) {
        return new MailchimpRecord(rec, necessaryInterest);
    }




    @Override public void create(Record srcrec) {
        super.create(srcrec);
        connector.createMember(srcrec.getKey(), getRecord(srcrec.getKey()).toJson()); // TODO: ugly construct
    }

    @Override public void update(Record destrec, Map<String, String> diffs) {
        super.update(destrec, diffs);
        connector.updateMemberFields(destrec.getKey(), diffs); // no reason to set necessary interest again
    }

    @Override public void delete(Record destrec) {
        super.delete(destrec);
        connector.unsubscribeMember(destrec.getKey());
    }

}
