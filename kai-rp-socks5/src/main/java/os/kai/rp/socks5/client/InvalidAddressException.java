package os.kai.rp.socks5.client;

public class InvalidAddressException extends Exception {
    public InvalidAddressException() {
    }
    public InvalidAddressException(String message) {
        super(message);
    }
    public InvalidAddressException(String message,Throwable cause) {
        super(message,cause);
    }
    public InvalidAddressException(Throwable cause) {
        super(cause);
    }
}
