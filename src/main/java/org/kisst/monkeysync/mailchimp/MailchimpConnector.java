package org.kisst.monkeysync.mailchimp;

import com.google.gson.Gson;
import okhttp3.*;
import org.kisst.monkeysync.Env;
import org.kisst.monkeysync.Props;
import org.kisst.monkeysync.json.JsonBuilder;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
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
    public String get(String urlpart) {
        OkHttpClient client = new OkHttpClient();
        System.out.println(baseurl+urlpart);
        String userpass=Base64.getEncoder().encodeToString(("dummy:"+apikey).getBytes());
        Request request = new Request.Builder()
                .url(baseurl+urlpart)
                .header("Authorization", "Basic "+ userpass)
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        } catch (IOException e) { throw new RuntimeException(e);}
    }


    public String call(String method, String urlpart, String data) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, data);
        String userpass=Base64.getEncoder().encodeToString(("dummy:"+apikey).getBytes());
        Request request = new Request.Builder()
                .url(baseurl+urlpart)
                .method(method, body)
                .header("Authorization", "Basic "+ userpass)
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
        public int total_items=0;
        public List<MailchimpRecord> members;
    }

    public int insertAllMembers(MemberInserter db, int offset, int count) {
        int pagesize=100;
        MailchimpMemberResponse respons;
        int max_item=offset+count;
        do {
            String httpresult = getMembers(offset, Math.min(count,pagesize));
            Env.verbose(httpresult, "");
            respons =gson.fromJson(httpresult,MailchimpMemberResponse.class);
            count-=pagesize;
            offset+=pagesize;
            if (respons!=null)
                for (MailchimpRecord rec: respons.members)
                    db.insert(rec);
        }
        while (offset <= Math.min(respons.total_items,max_item));
        return respons.total_items;
    }

    private static final Gson gson = new Gson();
    public String getMembers(int offset, int count) {
        String fields="members.email_address,members.merge_fields,members.interests,members.status";
        return get("/members/?include_fields="+fields+"&exclude_fields=members._links,_links&count="+count+"&offset="+offset);

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
