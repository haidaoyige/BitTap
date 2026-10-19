package io.coingift.core;

/** Local advisory only. Does not validate, store, transmit or estimate proven password entropy. */
public final class PasswordStrength {
    public enum Level { EMPTY, WEAK, MEDIUM, STRONG }
    private static final String[] COMMON={"123456","12345678","123456789","password","password123","qwerty","qwerty123","abc123","abcdef","letmein","iloveyou","admin123","correct horse battery staple"};
    private PasswordStrength(){}
    public static Level assess(CharSequence value){
        if(value==null||value.length()==0)return Level.EMPTY;
        value=PasswordNormalization.nfc(value);
        int n=value.length(),length=Character.codePointCount(value,0,n);
        for(String common:COMMON)if(equalIgnoringCase(value,common))return Level.WEAK;
        if(length<10||repeated(value)||sequential(value))return Level.WEAK;
        boolean lower=false,upper=false,digit=false,other=false;int unique=0;
        for(int i=0;i<n;){
            int c=Character.codePointAt(value,i);
            lower|=Character.isLowerCase(c);upper|=Character.isUpperCase(c);digit|=Character.isDigit(c);
            other|=!Character.isLetterOrDigit(c);
            if(unique<12){boolean seen=false;for(int j=0;j<i;){int previous=Character.codePointAt(value,j);if(previous==c){seen=true;break;}j+=Character.charCount(previous);}if(!seen)unique++;}
            i+=Character.charCount(c);
        }
        int kinds=(lower?1:0)+(upper?1:0)+(digit?1:0)+(other?1:0);
        if(unique<4||digit&&kinds==1&&length<16)return Level.WEAK;
        if(length>=16&&kinds>=3&&unique>=8||length>=20&&unique>=8&&!(digit&&kinds==1&&length<24))return Level.STRONG;
        return Level.MEDIUM;
    }
    private static boolean equalIgnoringCase(CharSequence a,String b){
        if(a.length()!=b.length())return false;
        for(int i=0;i<a.length();i++)if(Character.toLowerCase(a.charAt(i))!=b.charAt(i))return false;return true;
    }
    private static boolean repeated(CharSequence value){
        int n=value.length();
        for(int period=1;period<=Math.min(16,n/2);period++){
            if(n%period!=0)continue;boolean all=true;
            for(int i=period;i<n;i++)if(value.charAt(i)!=value.charAt(i%period)){all=false;break;}
            if(all)return true;
        }
        return false;
    }
    private static boolean sequential(CharSequence value){
        for(String alphabet:new String[]{"0123456789","abcdefghijklmnopqrstuvwxyz","qwertyuiop","asdfghjkl","zxcvbnm"}){
            for(int direction:new int[]{-1,1}){
                int start=alphabet.indexOf(Character.toLowerCase(value.charAt(0)));if(start<0)continue;boolean matches=true;
                for(int i=1;i<value.length();i++)if(Character.toLowerCase(value.charAt(i))!=alphabet.charAt(Math.floorMod(start+direction*i,alphabet.length()))){matches=false;break;}
                if(matches)return true;
            }
        }
        return false;
    }
}
