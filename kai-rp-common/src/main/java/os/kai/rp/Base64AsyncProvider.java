package os.kai.rp;

import os.kai.rp.util.AsyncProvider;
import os.kai.rp.util.Base64;

public class Base64AsyncProvider extends AsyncProvider<String,byte[]> {
    public Base64AsyncProvider(){
        super(Base64::decode);
    }
}
