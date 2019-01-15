package org.kisst.monkeysync.mailchimp;

import okhttp3.*;
import org.kisst.monkeysync.JsonBuilder;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MailchimpConnector {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String baseurl;
    private final String dc;
    private final String apikey;
    private final String listid;

    public MailchimpConnector(String dc, String listid, String apikey) {
        this.baseurl = "https://"+dc+".api.mailchimp.com/3.0/lists/"+listid;
        this.dc=dc;
        this.apikey=apikey;
        this.listid=listid;
    }


    public void post(String urlpart, String data) {call("POST", urlpart, data);}
    public void patch(String urlpart, String data) {call("PATCH", urlpart, data);}
    public void get(String urlpart) { call("GET", urlpart, null);}


    public void call(String method, String urlpart, String data) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, data);
        Request request = new Request.Builder()
                .url(baseurl+urlpart)
                .method(method, body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            response.body().string();
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

    public void getAllMembers(String fields, int offset, int max) {
        get("/members/?include_fields=members.email_address,members.merge_fields,members.interests,members.status&exclude_fields=members._links,_links&count="+max);
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
