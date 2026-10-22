package io.coingift.core;

/** Local advisory only. Does not validate, store, transmit or estimate proven password entropy. */
public final class PasswordStrength {
    public enum Level { EMPTY, WEAK, MEDIUM, STRONG }
    private static final String[] COMMON={"123456","12345678","123456789","password","password123","qwerty","qwerty123","abc123","abcdef","letmein","iloveyou","admin123","admin","welcome","changeme","bitcoin","bittap","letmein123","dragon","monkey","correct horse battery staple"};
    private PasswordStrength(){}
    public static Level assess(CharSequence value){
        if(value==null||value.length()==0)return Level.EMPTY;
        value=PasswordNormalization.nfc(value);
        int n=value.length(),length=Character.codePointCount(value,0,n);
        for(String common:COMMON)if(equalIgnoringCase(value,common))return Level.WEAK;
        if(length<10||predictable(value))return Level.WEAK;
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
    /** Advisory only: recognize template adornments without creating another password String. */
    private static boolean predictable(CharSequence value){
        if(repeated(value)||sequential(value)||commonBase(value))return true;
        int n=value.length();
        // A few prefix/suffix characters do not disguise a long repeated/keyboard/common core.
        for(int left=0;left<=4;left++)for(int right=0;right<=4-left;right++){
            if(left+right==0||n-left-right<8)continue;
            CharSequence core=new Slice(value,left,n-right);
            if(repeated(core)||sequential(core)||commonBase(core))return true;
        }
        // Mostly sequential/repeated digits or letters plus short complexity-rule decoration.
        for(int start=0;start<n;){
            int type=asciiKind(value.charAt(start)),end=start+1;
            while(end<n&&asciiKind(value.charAt(end))==type)end++;
            if(type!=0&&end-start>=6&&end-start>=n-4){
                CharSequence run=new Slice(value,start,end);
                if(sequential(run)||repeated(run))return true;
            }
            start=end;
        }
        return false;
    }
    private static int asciiKind(char c){return c>='0'&&c<='9'?1:c>='a'&&c<='z'||c>='A'&&c<='Z'?2:0;}
    private static boolean commonBase(CharSequence value){
        int start=0,end=value.length();
        while(start<end&&asciiKind(value.charAt(start))!=2)start++;
        while(end>start&&asciiKind(value.charAt(end-1))!=2)end--;
        for(String common:COMMON){
            if(end-start!=common.length())continue;
            boolean same=true;for(int i=start;i<end;i++)if(fold(value.charAt(i))!=common.charAt(i-start)){same=false;break;}
            if(same)return true;
        }
        return false;
    }
    private static char fold(char c){
        c=Character.toLowerCase(c);
        switch(c){case '@':case '4':return 'a';case '$':case '5':return 's';case '0':return 'o';case '1':return 'i';case '3':return 'e';case '7':return 't';default:return c;}
    }
    private static final class Slice implements CharSequence {
        private final CharSequence source;private final int start,end;
        Slice(CharSequence source,int start,int end){this.source=source;this.start=start;this.end=end;}
        public int length(){return end-start;}
        public char charAt(int index){if(index<0||index>=length())throw new IndexOutOfBoundsException();return source.charAt(start+index);}
        public CharSequence subSequence(int from,int to){if(from<0||to<from||to>length())throw new IndexOutOfBoundsException();return new Slice(source,start+from,start+to);}
        public String toString(){return "[password view]";}
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
