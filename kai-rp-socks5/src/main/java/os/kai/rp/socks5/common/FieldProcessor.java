package os.kai.rp.socks5.common;

@FunctionalInterface
public interface FieldProcessor {
    void process(byte[] bytes, int len) throws InvalidFieldException;
}
