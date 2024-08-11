package os.kai.rp;

public class TextProxyTag {
    private static final String INIT = "INIT";
    private static final String KEEP = "KEEP";
    private static final String DATA = "DATA";
    public static final int TAG_LEN = 4;
    public static final int TAG_START_LEN = TAG_LEN+2;
    public static final int TAG_END_LEN = TAG_LEN+3;
    public static final int TAG_SINGLE_LEN = TAG_LEN+4;
    public static final String INIT_START = startTag(INIT);
    public static final String INIT_END = endTag(INIT);
    public static final String DATA_START = startTag(DATA);
    public static final String DATA_END = endTag(DATA);
    public static final String KEEP_SINGLE = singleTag(KEEP);

    private static String startTag(String tag){
        return "<"+tag+">";
    }
    private static String endTag(String tag){
        return "</"+tag+">";
    }
    private static String singleTag(String tag){
        return "<"+tag+" />";
    }

    private static String unpack(String line, String startTag, String endTag){
        String r = null;
        int ll = line.length();
        if(ll>=TAG_START_LEN+TAG_END_LEN&&line.startsWith(startTag)&&line.endsWith(endTag)){
            r = line.substring(TAG_START_LEN,ll-TAG_END_LEN);
        }
        return r;
    }

    public static String unpackInit(String line){
        return unpack(line,INIT_START,INIT_END);
    }

    public static String unpackData(String line){
        return unpack(line,DATA_START,DATA_END);
    }
}
