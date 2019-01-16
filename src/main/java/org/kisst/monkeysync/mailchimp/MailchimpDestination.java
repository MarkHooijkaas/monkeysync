package org.kisst.monkeysync.mailchimp;

import org.kisst.monkeysync.DestRecord;
import org.kisst.monkeysync.SourceRecord;
import org.kisst.monkeysync.json.JsonBuilder;
import org.kisst.monkeysync.Record;
import org.kisst.monkeysync.RecordDestination;

import java.util.Map;

public class MailchimpDestination implements RecordDestination {
    private final MailchimpConnector connector;

    public MailchimpDestination(MailchimpConnector connector) {
        this.connector = connector;
    }

    @Override public Iterable<DestRecord> records() {
        return null; // TODO
    }

    @Override public void create(SourceRecord srcrec) {
        connector.createMember(srcrec.getKey(), JsonBuilder.toJson(srcrec));
    }

    @Override public void update(DestRecord destrec, Map<String, String> diffs) {
        connector.updateMemberFields(destrec.getKey(), JsonBuilder.toJson(diffs));
    }

    @Override public void delete(DestRecord destrec) {
        connector.unsubscribeMember(destrec.getKey());
    }

}
