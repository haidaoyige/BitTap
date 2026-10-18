package io.coingift.core;

/** Creation-only format checks. No password is retained or copied to an immutable String. */
public final class NewPasswordPolicy {
    public static final String INVALID_MESSAGE="新密码至少 10 位，须同时包含大写字母、小写字母、数字和特殊字符（最多 1024 字符）";
    private NewPasswordPolicy(){}

    public static final class Requirements {
        public final boolean length,upper,lower,digit,special;
        private Requirements(boolean length,boolean upper,boolean lower,boolean digit,boolean special){
            this.length=length;this.upper=upper;this.lower=lower;this.digit=digit;this.special=special;
        }
        public boolean satisfied(){return length&&upper&&lower&&digit&&special;}
    }

    public static Requirements inspect(CharSequence value){
        if(value==null)return new Requirements(false,false,false,false,false);
        boolean upper=false,lower=false,digit=false,special=false;int count=0;
        for(int i=0;i<value.length();){
            int cp=Character.codePointAt(value,i);i+=Character.charCount(cp);count++;
            upper|=cp>='A'&&cp<='Z';lower|=cp>='a'&&cp<='z';digit|=cp>='0'&&cp<='9';
            // Same Unicode punctuation/symbol categories as the approved prototype. Spaces do not count.
            int type=Character.getType(cp);
            special|=type==Character.CONNECTOR_PUNCTUATION||type==Character.DASH_PUNCTUATION
                    ||type==Character.START_PUNCTUATION||type==Character.END_PUNCTUATION
                    ||type==Character.INITIAL_QUOTE_PUNCTUATION||type==Character.FINAL_QUOTE_PUNCTUATION
                    ||type==Character.OTHER_PUNCTUATION||type==Character.MATH_SYMBOL
                    ||type==Character.CURRENCY_SYMBOL||type==Character.MODIFIER_SYMBOL||type==Character.OTHER_SYMBOL;
        }
        return new Requirements(count>=10&&value.length()<=1024,upper,lower,digit,special);
    }

    public static void require(char[] value){
        if(!inspect(value==null?null:java.nio.CharBuffer.wrap(value)).satisfied())
            throw new IllegalArgumentException(INVALID_MESSAGE);
    }
}
