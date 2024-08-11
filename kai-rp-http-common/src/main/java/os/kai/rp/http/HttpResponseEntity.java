package os.kai.rp.http;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class HttpResponseEntity {
    private String hsid = "";
    private int status = 0;
    private Map<String,String> headers = new HashMap<>();
}
