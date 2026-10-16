package io.coingift.android;

import android.content.ClipData;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Some devices silently deny writes. Only an exact read-back confirms a copy. */
final class VerifiedClipboard {
    static boolean writeAndVerify(ClipData expected,Consumer<ClipData> writer,Supplier<ClipData> reader){
        try{
            writer.accept(expected);ClipData actual=reader.get();
            if(actual==null||actual.getItemCount()!=1||expected.getItemCount()!=1)return false;
            CharSequence label=expected.getDescription().getLabel(),actualLabel=actual.getDescription().getLabel();
            CharSequence text=expected.getItemAt(0).getText(),actualText=actual.getItemAt(0).getText();
            return label!=null&&actualLabel!=null&&label.toString().contentEquals(actualLabel)
                &&text!=null&&actualText!=null&&text.toString().contentEquals(actualText);
        }catch(RuntimeException denied){return false;}
    }
}
