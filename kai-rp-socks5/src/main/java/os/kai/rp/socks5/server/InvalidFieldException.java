package os.kai.rp.socks5.server;

public class InvalidFieldException extends Exception {
    public InvalidFieldException() {
    }
    public InvalidFieldException(String message) {
        super(message);
    }
    public InvalidFieldException(String message,Throwable cause) {
        super(message,cause);
    }
    public InvalidFieldException(Throwable cause) {
        super(cause);
    }
}
