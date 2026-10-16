package io.coingift.android;

import android.content.Context;
import org.json.*;
import java.nio.charset.StandardCharsets;

/** The approved prototype's local copy, with no executable HTML or remote content. */
final class ApprovedCopy {
    private final Context context;private final JSONObject data;
    ApprovedCopy(Context context){this.context=context;try(java.io.InputStream in=context.getAssets().open("approved-copy.json")){data=new JSONObject(new String(read(in),StandardCharsets.UTF_8));}catch(Exception ex){throw new IllegalStateException("Local copy unavailable",ex);}}
    private static byte[] read(java.io.InputStream in)throws java.io.IOException{java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);return out.toByteArray();}
    String ui(String key){return data.optJSONObject("D").optJSONObject(Texts.language(context)).optString(key,key);}
    JSONObject section(String key){return data.optJSONObject(key).optJSONObject(Texts.language(context));}
    JSONObject security(){return data.optJSONObject("securityContent");}
    JSONArray terms(){return Texts.language(context).equals("zh")?data.optJSONArray("legalContent"):data.optJSONObject("legalTranslations").optJSONArray(Texts.language(context));}
}
