package os.kai.rp.socks5.server;

import lombok.Getter;
import lombok.Setter;
import os.kai.rp.UnsafeConsumer;

public class FieldProcessorWithLength implements FieldProcessor{

    @Getter
    @Setter
    private int length;

    private final FieldProcessor proc;

    public FieldProcessorWithLength(int length,FieldProcessor proc) {
        this.length = length;
        this.proc = proc;
    }

    public FieldProcessorWithLength(FieldProcessor proc) {
        this(0,proc);
    }

    @Override
    public void process(byte[] bytes,int len) throws InvalidFieldException {
        proc.process(bytes,len);
    }
}
