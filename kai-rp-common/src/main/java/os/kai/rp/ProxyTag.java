package os.kai.rp;

public class ProxyTag {
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
}
