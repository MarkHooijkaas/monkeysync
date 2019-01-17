package org.kisst.monkeysync.mailchimp;

import com.google.gson.Gson;
import okhttp3.*;
import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.json.JsonBuilder;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class MailchimpConnector {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String baseurl;
    private final String apikey;
    private final String listid;

    public MailchimpConnector(Props props) {
        this.listid=props.getString("listid");
        this.baseurl = props.getString("url")+"lists/"+listid;
        this.apikey=props.getString("apikey");
    }


    public void post(String urlpart, String data) {call("POST", urlpart, data);}
    public void patch(String urlpart, String data) {call("PATCH", urlpart, data);}
    public String get(String urlpart) { return call("GET", urlpart, null);
    }


    public String call(String method, String urlpart, String data) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, data);
        Request request = new Request.Builder()
                .url(baseurl+urlpart)
                .method(method, body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        } catch (IOException e) { throw new RuntimeException(e);}
    }

    public void createMember(String email, String fields) {
        JsonBuilder json=new JsonBuilder();
        json.addStringField("email_address", email);
        json.addStringField("status", "subscribed");
        json.addUnescapedField("merge_fields", fields);
        post("members/", json.toString() );
    }

    public void updateMemberFields(String email, String fields) {
        JsonBuilder json=new JsonBuilder();
        json.addStringField("email_address", email);
        json.addUnescapedField("merge_fields", fields);
        patch(memberUrl(email), json.toString() );
    }

    public void unsubscribeMember(String email) {
        JsonBuilder json=new JsonBuilder();
        json.addStringField("email_address", email);
        json.addStringField("status", "unsubscribed");
        patch(memberUrl(email), json.toString() );
    }

    public static interface MemberInserter {
        //void suggestSize(int size);
        void insert(MailchimpRecord rec);
    }

    private static class MailchimpMemberResponse {
        public int total_items;
        public List<MailchimpRecord> members;
    }

    public int insertAllMembers(MemberInserter db, int offset, int count) {
        int pagesize=1000;
        MailchimpMemberResponse respons;
        do {
            if (count<pagesize)
                pagesize=count;
            String httpresult = getMembers(offset, pagesize);
            respons =gson.fromJson(httpresult,MailchimpMemberResponse.class);
            count-=pagesize;
            offset+=pagesize;
            for (MailchimpRecord rec: respons.members)
                db.insert(rec);
        }
        while (offset<respons.total_items);
        return respons.total_items;
    }

    private static final Gson gson = new Gson();
    public String getMembers(int offset, int max) {
        String fields="members.email_address,members.merge_fields,members.interests,members.status";
        return get("/members/?include_fields="+fields+"&exclude_fields=members._links,_links&count="+max+"&offset"+offset);

    }

    private String memberUrl(String email) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(email.toLowerCase().getBytes());
            byte[] digest = md.digest();
             return "members/"+DatatypeConverter.printHexBinary(digest).toLowerCase();
        }
        catch (NoSuchAlgorithmException e) { throw new RuntimeException(e); }
    }


}
