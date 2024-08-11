package os.kai.rp.http;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * Created by hujq on 2023/3/14.
 */
public class IgnoreHostnameVerifier implements HostnameVerifier {
	@Override
	public boolean verify(String s,SSLSession sslSession){
		return true;
	}
}
