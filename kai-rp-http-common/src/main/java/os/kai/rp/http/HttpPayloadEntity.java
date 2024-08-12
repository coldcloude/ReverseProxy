package os.kai.rp.http;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HttpPayloadEntity {
    private String hsid = "";
    private String data64 = "";
}
