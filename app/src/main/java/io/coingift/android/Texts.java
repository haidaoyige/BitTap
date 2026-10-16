package io.coingift.android;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;
import java.util.Locale;

/** Only the language preference is persisted. No wallet state is stored here. */
final class Texts {
    static String selected(Context c){return c.getSharedPreferences("appearance",Context.MODE_PRIVATE).getString("language","system");}
    static String language(Context c){
        String selected=selected(c);if(!selected.equals("system"))return selected;
        LocaleList list=Resources.getSystem().getConfiguration().getLocales();
        for(int i=0;i<list.size();i++){String code=list.get(i).getLanguage();if(code.equals("zh")||code.equals("en")||code.equals("ja")||code.equals("ko"))return code;}
        return "en";
    }
    // All four locales are packaged in the APK; app/build.gradle disables language splits.
    // Verified with aapt2 dump configurations and the four-locale device regression.
    @android.annotation.SuppressLint("AppBundleLocaleChanges")
    static Context wrap(Context c){Configuration config=new Configuration(c.getResources().getConfiguration());config.setLocales(new LocaleList(Locale.forLanguageTag(language(c))));return c.createConfigurationContext(config);}
    static void select(Context c,String value){if(!java.util.Arrays.asList("system","zh","en","ja","ko").contains(value))throw new IllegalArgumentException();c.getSharedPreferences("appearance",Context.MODE_PRIVATE).edit().putString("language",value).apply();}
    static String tr(Context c,String text){if(text==null)return null;Integer id=TextCatalog.IDS.get(text);if(id!=null)return c.getString(id);
        if(language(c).equals("zh"))return text;
        // Localize the static fragments of our own capacity/status messages; user notes bypass this method.
        String result=text;java.util.List<String> keys=new java.util.ArrayList<>(TextCatalog.IDS.keySet());keys.sort((a,b)->Integer.compare(b.length(),a.length()));
        for(String key:keys)if(key.length()>=3&&result.contains(key))result=result.replace(key,c.getString(TextCatalog.IDS.get(key)));
        return result;}
}
