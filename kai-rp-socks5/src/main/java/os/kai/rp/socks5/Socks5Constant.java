package os.kai.rp.socks5;

public class Socks5Constant {
    public static final String SID = "socks5";
    public static final String PREFIX_REQ = "socks5.request:";
    public static final String PREFIX_RELAY = "socks5.relay:";
    public static final String PREFIX_CLOSE = "socks5.close:";
    public static final int PREFIX_REQ_LEN = PREFIX_REQ.length();
    public static final int PREFIX_RELAY_LEN = PREFIX_RELAY.length();
    public static final int PREFIX_CLOSE_LEN = PREFIX_CLOSE.length();
    public static final int BUF_LEN = 3072;
    public static final int ATYP_IPV4 = 0x01;
    public static final int ATYP_IPV6 = 0x04;
    public static final int ATYP_DOMAIN = 0x03;
}
