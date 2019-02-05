package org.kisst.monkeysync.mailchimp;

import com.google.gson.Gson;
import okhttp3.*;
import org.kisst.monkeysync.Env;
import org.kisst.monkeysync.Props;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MailchimpConnector {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String baseurl;
    private final String apikey;
    private final String listid;
    private final boolean update;

    public MailchimpConnector(Props props) {
        this.listid=props.getString("listid");
        this.baseurl = props.getString("url")+"lists/"+listid;
        this.apikey=props.getString("apikey");
        this.update=props.getBoolean("update",false);
    }


    public void put(String urlpart, String data) {call("PUT", urlpart, data);}
    public void post(String urlpart, String data) {call("POST", urlpart, data);}
    public void patch(String urlpart, String data) {call("PATCH", urlpart, data);}
    public void delete(String urlpart) { call("DELETE", urlpart, null); }
    public String get(String urlpart) { return call("GET", urlpart, null); }

    public String call(String method, String urlpart, String data) {
        OkHttpClient client = new OkHttpClient();
        String userpass=Base64.getEncoder().encodeToString(("dummy:"+apikey).getBytes());
        Request.Builder builder= new Request.Builder()
                .url(baseurl+urlpart)
                .header("Authorization", "Basic "+ userpass);
        if ("GET".equals(method))
            builder.get();
        else if ("DELETE".equals(method))
            builder.delete();
        else
            builder.method(method, RequestBody.create(JSON, data));
        Env.info(method + ": " + urlpart, data);

        try (Response response = client.newCall(builder.build()).execute()) {
            if (response.code()!=200)
                System.err.println("ERROR: "+response);
            Env.debug("response: ", response.toString());
            return response.body().string();
        } catch (IOException e) { throw new RuntimeException(e);}
    }

    public void createMember(String email, String json) {
        Env.verbose("Create: "+email,json);
        if (update)
            post("/members/", json );
    }

    public void updateMemberFields(String email, Map<String, String> diffs) {
        HashMap<String, Object> struct= new HashMap<>();
        struct.put("email_address", email);
        struct.put("merge_fields", diffs);
        if (update)
            patch(memberUrl(email), gson.toJson(struct));
    }

    public void unsubscribeMember(String email, boolean clearAllFields) {
        HashMap<String, Object> struct= new HashMap<>();
        struct.put("email_address", email);
        struct.put("status", "unsubscribed");
        Env.verbose("Delete: ",email);
        if (update) {
            if (clearAllFields)
                put(memberUrl(email), gson.toJson(struct));
            else
                patch(memberUrl(email), gson.toJson(struct));
        }
    }

    public void deleteMember(String email, boolean permanent) {
        if (update) {
            if (permanent)
                post(memberUrl(email)+"/actions/delete-permanent", "");
            else
                delete(memberUrl(email));
        }
    }


    public static interface MemberInserter {
        //void suggestSize(int size);
        void insert(MailchimpRecord rec);
    }

    private static class MailchimpMemberResponse {
        public int total_items=0;
        public List<MailchimpRecord> members;
    }

    public int insertAllMembers(MemberInserter db, int offset, int count, String urlOptions) {
        int pagesize=100;
        MailchimpMemberResponse respons;
        int max_item=offset+count;
        do {
            String httpresult = getMembers(offset, Math.min(count,pagesize), urlOptions);
            Env.debug(httpresult, "");
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
    public String getMembers(int offset, int count, String urlOptions) {
        if (urlOptions.length()>0 && ! urlOptions.startsWith("&"))
            urlOptions="&"+urlOptions;
        String fields="members.email_address,members.merge_fields,members.interests,members.status";
        return get("/members/?include_fields="+fields+"&exclude_fields=members._links,_links&count="+count+"&offset="+offset+urlOptions);

    }

    private String memberUrl(String email) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(email.toLowerCase().getBytes());
            byte[] digest = md.digest();
             return "/members/"+DatatypeConverter.printHexBinary(digest).toLowerCase();
        }
        catch (NoSuchAlgorithmException e) { throw new RuntimeException(e); }
    }


}
