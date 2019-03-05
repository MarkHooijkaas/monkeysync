package org.kisst.monkeysync.mailchimp;

import org.kisst.monkeysync.Env;
import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.Record;
import org.kisst.monkeysync.map.BaseTable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class MailchimpTable extends BaseTable<MailchimpRecord> implements MailchimpConnector.MemberInserter {
    private final MailchimpConnector connector;
    private final String necessaryInterest;
    private final boolean retrieveAll;
    private final String retrieveSince;
    private final int offset;
    private final int maxsize;

    private final boolean resubscribeAllowed;
    private final boolean useUnsubscribe;
    private final boolean clearAllOnUnsubscribe;
    private final boolean deletePermanent;
    private final boolean modifyMailchimp;

    public MailchimpTable(Props props) {
        super(props);
        this.connector = new MailchimpConnector(props);
        this.offset = props.getInt("offset", 0); // Not too big, since there is still ssome arihtmetic done
        this.maxsize = props.getInt("count", 999999); // Not too big, since there is still ssome arihtmetic done
        this.necessaryInterest = props.getString("necessaryInterest", null);

        this.retrieveAll = props.getBoolean("retrieveAll", true);
        this.retrieveSince = props.getString("retrieveSince", null);
        this.modifyMailchimp=props.getBoolean("modifyMailchimp", false);
        this.resubscribeAllowed=props.getBoolean("resubscribeAllowed", false);
        this.useUnsubscribe=props.getBoolean("useUnsubscribe", true);
        this.clearAllOnUnsubscribe=props.getBoolean("clearAllOnUnsubscribe", true);
        this.deletePermanent=props.getBoolean("deletePermanent", false);
    }

    @Override  public void fetch() {
        if (this.retrieveAll) {
            retrieveAll();
            autoSave();
        }
        if (this.retrieveSince!=null) {
            retrieveMembersSince(retrieveSince);
            autoSave();
        }
    }


    public boolean hasAllNecessaryInterests(MailchimpRecord rec) {
        if (necessaryInterest!=null) {
            Boolean b = rec.interests.get(necessaryInterest);
            if (b==null || b==false)
                return false;
        }
        return true;
    }

    @Override public boolean isActive(Record rec) {
        if (((MailchimpRecord) rec).status.equals("subscribed"))
            return super.isActive(rec);
        return false;
    }
    @Override public boolean isDeleteDesired(Record rec) {
        if (((MailchimpRecord) rec).status.equals("subscribed"))
            return super.isDeleteDesired(rec);
        return true;
    }

        protected boolean mayRecordBeChanged(MailchimpRecord rec) {
        if (rec==null)
            return false;
        if ("unsubscribed".equals(rec.status) && ! resubscribeAllowed)
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

    public void retrieveAll() {
        connector.insertAllMembers(this, offset, maxsize, "");
    }
    public void retrieveMembersSince(String durationSpec) {
        if (! durationSpec.startsWith("P"))
            durationSpec="P"+durationSpec;
        Duration p=Duration.parse(durationSpec);
        Instant time= Instant.now().minus(p);
        String iso8601=time.toString();
        connector.insertAllMembers(this, offset, maxsize, "since_last_changed="+iso8601);
    }
    @Override public void insert(MailchimpRecord rec) {
        records.put(rec.getKey(),rec);
    }


    @Override protected MailchimpRecord createRecord(String json) {
        return new MailchimpRecord(json);
    }
    @Override protected MailchimpRecord createRecord(Record rec) {
        return new MailchimpRecord(rec, necessaryInterest);
    }


    @Override public void create(Record srcrec) {
        super.create(srcrec);
        Env.debug("Create: "+srcrec.getKey(),srcrec.toJson());
        if (modifyMailchimp)
            connector.createMember(srcrec.getKey(), getRecord(srcrec.getKey()).toJson()); // TODO: ugly construct
    }

    @Override public void update(Record destrec, Map<String, String> diffs) {
        super.update(destrec, diffs);
        if (modifyMailchimp)
            connector.updateMemberFields(destrec.getKey(), diffs); // no reason to set necessary interest again
    }

    @Override public void delete(Record destrec) {
        super.delete(destrec);
        Env.debug("Delete: ",destrec.getKey());
        if (modifyMailchimp) {
            if (useUnsubscribe)
                connector.unsubscribeMember(destrec.getKey(), clearAllOnUnsubscribe);
            else
                connector.deleteMember(destrec.getKey(), deletePermanent);
        }

    }

}
