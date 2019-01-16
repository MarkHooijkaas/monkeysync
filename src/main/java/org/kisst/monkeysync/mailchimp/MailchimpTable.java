package org.kisst.monkeysync.mailchimp;

import org.kisst.monkeysync.Record;
import org.kisst.monkeysync.json.JsonBuilder;
import org.kisst.monkeysync.map.MapTable;

import java.util.Map;

public class MailchimpTable extends MapTable {
    private final MailchimpConnector connector;

    public MailchimpTable(MailchimpConnector connector) {
        this.connector = connector;
    }

    @Override public Iterable<Record> records() {
        return null; // TODO
    }

    @Override public void create(Record srcrec) {
        super.create(srcrec);
        connector.createMember(srcrec.getKey(), JsonBuilder.toJson(srcrec));
    }

    @Override public void update(Record destrec, Map<String, String> diffs) {
        super.update(destrec,diffs);
        connector.updateMemberFields(destrec.getKey(), JsonBuilder.toJson(diffs));
    }

    @Override public void delete(Record destrec) {
        super.delete(destrec);
        connector.unsubscribeMember(destrec.getKey());
    }

}
