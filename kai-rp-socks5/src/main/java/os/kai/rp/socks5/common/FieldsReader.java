package os.kai.rp.socks5.common;

import io.netty.buffer.ByteBuf;

import java.util.LinkedList;
import java.util.List;

public class FieldsReader {
    private static final int SLEN = 16;
    private final List<FieldProcessorWithLength> processors = new LinkedList<>();
    private final byte[] sbs = new byte[SLEN];
    private int index = 0;
    private int offset = 0;
    private byte[] lbs = null;

    public void addFixedLengthProcessor(int length, FieldProcessor proc){
        processors.add(new FieldProcessorWithLength(length,proc));
    }

    public void addVariableLengthProcessor(FieldProcessor proc){
        FieldProcessorWithLength varProc = new FieldProcessorWithLength(proc);
        FieldProcessorWithLength lenProc = new FieldProcessorWithLength(1,(bs,len)->{
            int vlen = ((int)bs[0])&0xFF;
            varProc.setLength(vlen);
        });
        processors.add(lenProc);
        processors.add(varProc);
    }

    public boolean read(ByteBuf bb) throws InvalidFieldException {
        while(index<processors.size()){
            int readable = bb.readableBytes();
            if(readable<=0){
                break;
            }
            FieldProcessorWithLength proc = processors.get(index);
            int len = proc.getLength();
            byte[] bs;
            if(len<=SLEN){
                bs = sbs;
            }
            else {
                if(lbs==null){
                    lbs = new byte[len];
                }
                bs = lbs;
            }
            int rlen = Math.min(len-offset,readable);
            bb.readBytes(bs,offset,rlen);
            offset += rlen;
            if(offset>=len){
                proc.process(bs,len);
                index++;
                offset = 0;
                if(lbs!=null){
                    lbs = null;
                }
            }
        }
        return index>=processors.size();
    }
}
