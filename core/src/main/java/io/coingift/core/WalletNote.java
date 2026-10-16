package io.coingift.core;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/** Public, authenticated text only. Never used as key generation material or a filename. */
public final class WalletNote {
    private WalletNote() {}
    public static String normalize(String value) {
        if(value==null)value="";
        String text=Normalizer.normalize(value,Normalizer.Form.NFC);
        if(text.codePointCount(0,text.length())>16||text.getBytes(StandardCharsets.UTF_8).length>64)
            throw new IllegalArgumentException("钱包备注最多 16 个字符、64 字节");
        String punctuation=" .,，。!！?？:：;；、_-()（）[]【】'\"+/#&@·";
        for(int i=0;i<text.length();) {
            int c=text.codePointAt(i);i+=Character.charCount(c);int type=Character.getType(c);
            if(!Character.isLetterOrDigit(c)&&type!=Character.NON_SPACING_MARK&&type!=Character.COMBINING_SPACING_MARK&&punctuation.indexOf(c)<0)
                throw new IllegalArgumentException("备注仅支持文字、数字、空格和常见标点，不支持表情或控制字符");
        }
        return text;
    }
}
