package io.coingift.core;

/** Creation-only checks of the same NFC representation used by the KDF. */
public final class NewPasswordPolicy {
    public static final String INVALID_MESSAGE="新密码至少 10 位，须同时包含大写字母、小写字母、数字和特殊字符（最多 1024 字符）";
    public static final String CHARACTERS_MESSAGE="仅支持英文大小写字母、数字和半角标点符号，不支持中文、表情或空格";
    private NewPasswordPolicy(){}

    public static final class Requirements {
        public final boolean length,upper,lower,digit,special,characters;
        private Requirements(boolean length,boolean upper,boolean lower,boolean digit,boolean special,boolean characters){
            this.length=length;this.upper=upper;this.lower=lower;this.digit=digit;this.special=special;this.characters=characters;
        }
        public boolean satisfied(){return length&&upper&&lower&&digit&&special&&characters;}
    }

    public static Requirements inspect(CharSequence value){
        if(value==null)return new Requirements(false,false,false,false,false,false);
        int inputLength=value.length();
        // Check the original input too: normalization must not admit look-alike
        // non-ASCII characters such as the Kelvin sign by converting them to ASCII.
        boolean characters=true;
        for(int i=0;i<inputLength;i++)characters&=value.charAt(i)>=0x21&&value.charAt(i)<=0x7e;
        value=PasswordNormalization.nfc(value);
        boolean upper=false,lower=false,digit=false,special=false;int count=0;
        for(int i=0;i<value.length();){
            int cp=Character.codePointAt(value,i);i+=Character.charCount(cp);count++;
            upper|=cp>='A'&&cp<='Z';lower|=cp>='a'&&cp<='z';digit|=cp>='0'&&cp<='9';
            special|=cp>=0x21&&cp<=0x7e&&!Character.isLetterOrDigit(cp);
        }
        return new Requirements(count>=10&&inputLength<=1024,upper,lower,digit,special,characters);
    }

    public static void require(char[] value){
        Requirements requirements=inspect(value==null?null:java.nio.CharBuffer.wrap(value));
        if(value!=null&&!requirements.characters)throw new IllegalArgumentException(CHARACTERS_MESSAGE);
        if(!requirements.satisfied())
            throw new IllegalArgumentException(INVALID_MESSAGE);
    }
}
