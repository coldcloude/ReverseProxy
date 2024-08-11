package os.kai.rp.http;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class HttpRequestEntity {
    private String hsid = "";
    private String method = "";
    private String path = "";
    private Map<String,String> headers = new HashMap<>();
}
