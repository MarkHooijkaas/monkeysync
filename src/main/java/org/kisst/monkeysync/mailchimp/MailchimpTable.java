package org.kisst.monkeysync.mailchimp;

import com.google.gson.Gson;
import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.Record;
import org.kisst.monkeysync.json.JsonHelper;
import org.kisst.monkeysync.map.MapRecord;
import org.kisst.monkeysync.map.MapTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class MailchimpTable extends MapTable {
    private final MailchimpConnector connector;
    public boolean updatesAllowed=false;

    public MailchimpTable(Props props) {
        this.connector = new MailchimpConnector(props);
    }


    @Override public void create(Record srcrec) {
        super.create(srcrec);
        if (updatesAllowed)
            connector.createMember(srcrec.getKey(), JsonHelper.toJson(srcrec));
    }

    @Override public void update(Record destrec, Map<String, String> diffs) {
        super.update(destrec,diffs);
        if (updatesAllowed)
            connector.updateMemberFields(destrec.getKey(), JsonHelper.toJson(diffs));
    }

    @Override public void delete(Record destrec) {
        super.delete(destrec);
        if (updatesAllowed)
            connector.unsubscribeMember(destrec.getKey());
    }

    protected Record createRecord(String json) {
        LinkedHashMap<String, String> map = gson.fromJson(json, LinkedHashMap.class);
        return new MapRecord(map);
    }

}
