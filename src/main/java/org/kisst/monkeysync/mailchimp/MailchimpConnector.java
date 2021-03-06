package org.kisst.monkeysync.mailchimp;

import com.google.gson.Gson;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kisst.monkeysync.Props;
import org.kisst.script.Context;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MailchimpConnector {
    private static final Logger logger= LogManager.getLogger();

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String baseurl;
    private final String apikey;
    private final String listid;
    private final boolean continueOnError;
    private final int unknownHostExceptionTries;
    private final long unknownHostExceptionRetryInterval;

    public MailchimpConnector(Props props) {
        this.listid=props.getString("listid");
        this.baseurl = props.getString("url")+"lists/"+listid;
        this.apikey=props.getString("apikey");
        this.continueOnError=props.getBoolean("continueOnError", false);
        this.unknownHostExceptionTries = props.getInt("unknownHostExceptionTries",10);
        this.unknownHostExceptionRetryInterval = props.getInt("unknownHostExceptionRetryInterval",60);
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
        logger.debug("{}: {} {}", method, urlpart, data);

        int tries=0;
        while (true) {
            try (Response response = client.newCall(builder.build()).execute()) {
                String body = response.body().string();
                if (response.code() != 200 && response.code() != 204) {// 204 Means "no content", and is returned after a succesfull DELETE
                    if (continueOnError)
                        logger.warn("HTTP ERROR: {} {} when handling {} {}", response, body, method, data);
                    else
                        throw new RuntimeException("HTTP ERROR: " + response + body + method + data);
                } else
                    logger.debug("response: {}", response);
                return body;
            }
            catch (UnknownHostException e) {
                tries++;
                if (tries>=unknownHostExceptionTries)
                    throw new RuntimeException(e);
                logger.warn("UnknownHostException occurred, will automatically retry in {} seconds",unknownHostExceptionRetryInterval);
                Context.sleep(unknownHostExceptionRetryInterval*1000);
            }
            catch (IOException e) {throw new RuntimeException(e);}
        }
    }

    public void createMember(String email, String json) {
        put(memberUrl(email), json );
        //post("/members/", json );
    }

    public void updateMemberFields(String email, Map<String, String> diffs) {
        HashMap<String, Object> struct= new HashMap<>();
        struct.put("email_address", email);
        struct.put("merge_fields", diffs);
        patch(memberUrl(email), gson.toJson(struct));
    }

    public void unsubscribeMember(String email, boolean clearAllFields) {
        HashMap<String, Object> struct= new HashMap<>();
        struct.put("email_address", email);
        struct.put("status", "unsubscribed");
        if (clearAllFields)
            put(memberUrl(email), gson.toJson(struct));
        else
            patch(memberUrl(email), gson.toJson(struct));
    }

    public void deleteMember(String email, boolean permanent) {
        if (permanent)
            post(memberUrl(email)+"/actions/delete-permanent", "");
        else
            delete(memberUrl(email));
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
            //TODO: if (ctx.verbosity>0)
            System.out.print("=");
            String httpresult = getMembers(offset, Math.min(count,pagesize), urlOptions);
            logger.debug("httpresult: {}", httpresult);
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
