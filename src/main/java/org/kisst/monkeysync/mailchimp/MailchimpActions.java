package org.kisst.monkeysync.mailchimp;

import org.kisst.monkeysync.ActionHandler;
import org.kisst.monkeysync.JsonBuilder;
import org.kisst.monkeysync.Record;

import java.util.Map;

public class MailchimpActions implements ActionHandler {
    private final MailchimpConnector connector;

    public MailchimpActions(MailchimpConnector connector) {
        this.connector = connector;
    }

    @Override
    public void create(Record rec) {
        connector.createMember(rec.getKey(), JsonBuilder.toJson(rec));
    }

    @Override
    public void update(Record rec, Map<String, String> diffs) {
        connector.updateMemberFields(rec.getKey(), JsonBuilder.toJson(diffs));
    }

    @Override
    public void delete(Record rec) {
        connector.unsubscribeMember(rec.getKey());
    }

}
