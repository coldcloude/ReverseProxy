package os.kai.rp;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class HttpResponseEntity {
    private long timestamp = 0;
    private long sn = 0;
    private String method = "";
    private String path = "";
    private Map<String,String> headers = new HashMap<>();
    private int status = 0;
    private String result64 = "";
}
