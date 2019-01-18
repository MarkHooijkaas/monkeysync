package org.kisst.monkeysync.mailchimp;

import com.google.gson.Gson;
import org.kisst.monkeysync.Record;
import java.util.LinkedHashMap;
import java.util.Map;

public class MailchimpRecord implements Record {
    private static final Gson gson = new Gson();

    public final String email_address;
    public final String status;
    public final LinkedHashMap<String, Boolean> interests;
    private final LinkedHashMap<String,String> merge_fields;


    public MailchimpRecord(Record rec, String interest) {
        this.email_address=rec.getKey();
        this.status="subscribed";
        this.interests=new LinkedHashMap<>();
        if (interest!=null)
            interests.put(interest,true);
        this.merge_fields=new LinkedHashMap<>();
        for (String field : rec.fieldNames())
            merge_fields.put(field, rec.getField(field));
    }

    public MailchimpRecord(String json) {
        LinkedHashMap<String, Object> map = gson.fromJson(json, LinkedHashMap.class);
        this.email_address= (String) map.get("email_address");
        this.status = (String) map.get("status");
        this.interests= (LinkedHashMap<String, Boolean>) map.get("interests");
        this.merge_fields= new LinkedHashMap<> ((Map) map.get("merge_fields"));
    }

    @Override public String toJson() { return gson.toJson(this, MailchimpRecord.class); }
    @Override public String getKey() { return email_address;}
    @Override public Iterable<String> fieldNames() { return merge_fields.keySet();}
    @Override public String getField(String name) { return merge_fields.get(name);}
    @Override public void setField(String name, String value) { merge_fields.put(name, value); }

}
