package org.kisst.monkeysync.mailchimp;

import com.google.gson.Gson;
import org.kisst.monkeysync.Record;
import java.util.LinkedHashMap;

public class MailchimpRecord implements Record {
    private static final Gson gson = new Gson();

    private final String email_address;
    private final String status;
    private final LinkedHashMap<String,String> merge_fields;


    public MailchimpRecord(Record rec) {
        this.email_address=rec.getKey();
        this.status="subscribed";
        this.merge_fields=new LinkedHashMap<>();
        for (String field : rec.fieldNames())
            merge_fields.put(field, rec.getField(field));
    }

    public MailchimpRecord(String json) {
        LinkedHashMap<String, Object> map = gson.fromJson(json, LinkedHashMap.class);
        this.email_address= (String) map.get("email_address");
        this.status = (String) map.get("status");
        this.merge_fields= (LinkedHashMap<String, String>) map.get("merge_fields");
    }

    @Override public String toJson() { return gson.toJson(this, MailchimpRecord.class); }
    @Override public String getKey() { return email_address;}
    @Override public Iterable<String> fieldNames() { return merge_fields.keySet();}
    @Override public String getField(String name) { return merge_fields.get(name);}
    @Override public void setField(String name, String value) { merge_fields.put(name, value); }

}
