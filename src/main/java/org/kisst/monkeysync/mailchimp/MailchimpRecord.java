package org.kisst.monkeysync.mailchimp;

import com.google.gson.Gson;
import org.kisst.monkeysync.Record;
import java.util.LinkedHashMap;
import java.util.Map;

public class MailchimpRecord implements Record {
    private static final Gson gson = new Gson();

    private final String email_address;
    private final String status;
    private final LinkedHashMap<String,String> merge_fields;


    public MailchimpRecord(String json) {
        LinkedHashMap<String, Object> map = gson.fromJson(json, LinkedHashMap.class);
        this.email_address= (String) map.get("email_address");
        this.status = (String) map.get("status");
        this.merge_fields= (LinkedHashMap<String, String>) map.get("merge_fields");
    }

    public String toJson() { return gson.toJson(this, MailchimpRecord.class); }
    @Override public String getKey() { return email_address;}
    @Override public Iterable<String> fieldNames() { return merge_fields.keySet();}
    @Override public String getField(String name) { return merge_fields.get(name);}
    public void merge(Map<String, String> diffs) {
        for (String field: diffs.keySet())
            merge_fields.put(field, diffs.get(field));
    }

}
