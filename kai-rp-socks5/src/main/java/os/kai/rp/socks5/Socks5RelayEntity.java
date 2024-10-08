package os.kai.rp.socks5;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Socks5RelayEntity {
    private String ssid;
    private String data64;
}
