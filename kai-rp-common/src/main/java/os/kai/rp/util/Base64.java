package os.kai.rp.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Base64 {

    private static final char[] ALPHABET = new char[]{
            'A','B','C','D','E','F','G','H',
            'I','J','K','L','M','N','O','P',
            'Q','R','S','T','U','V','W','X',
            'Y','Z','a','b','c','d','e','f',
            'g','h','i','j','k','l','m','n',
            'o','p','q','r','s','t','u','v',
            'w','x','y','z','0','1','2','3',
            '4','5','6','7','8','9','+','/'
    };

    private static final Map<Character,Integer> CODE_MAP = new HashMap<>();

    static {
        for(int i = 0; i<ALPHABET.length; i++){
            CODE_MAP.put(ALPHABET[i],i);
        }
    }

    public static byte[] decode(String str) {
        int strLen = str.length();
        //format input string
        List<Character> cs = new ArrayList<>(strLen);
        for(int i = 0; i<strLen; i++){
            char c = str.charAt(i);
            if(CODE_MAP.containsKey(c)){
                cs.add(c);
            }
        }
        int strSize = cs.size();
        int segNum = strSize/4;
        int restSize = 4-(strSize-segNum*4);
        if(restSize>0){
            segNum++;
            strSize += restSize;
            for(int i = 0; i<restSize; i++){
                cs.add('=');
            }
        }
        //calculate return size
        int rSize = segNum*3;
        if(cs.get(strSize-1)=='='){
            rSize--;
            if(cs.get(strSize-2)=='='){
                rSize--;
            }
        }
        byte[] rst = new byte[rSize];
        //build
        int[] buf = new int[4];
        for(int si = 0; si<segNum; si++){
            int base = si*4;
            for(int i = 0; i<4; i++){
                buf[i] = CODE_MAP.getOrDefault(cs.get(base+i),-1);
            }
            int rBase = si*3;
            if(buf[0]>=0&&buf[1]>=0){
                rst[rBase] = (byte)(((buf[0]&0x3F)<<2)|((buf[1]&0x30)>>4));
                if(buf[2]>=0){
                    rst[rBase+1] = (byte)(((buf[1]&0x0F)<<4)|((buf[2]&0x3C)>>2));
                    if(buf[3]>=0){
                        rst[rBase+2] = (byte)(((buf[2]&0x03)<<6)|(buf[3]&0x3F));
                    }
                }
            }
        }
        return rst;
    }

    public static String encode(byte[] arr,int length) {
        length = Math.min(length,arr.length);
        StringBuilder builder = new StringBuilder();
        int[] ibuf = new int[4];
        for(int bi = 0; bi<length; bi += 3){
            ibuf[0] = (((int)arr[bi])&0xFC)>>2;
            ibuf[1] = (((int)arr[bi])&0x03)<<4;
            if(bi+1<length){
                ibuf[1] |= (((int)arr[bi+1])&0xF0)>>4;
                ibuf[2] = (((int)arr[bi+1])&0x0F)<<2;
                if(bi+2<length){
                    ibuf[2] |= (((int)arr[bi+2])&0xC0)>>6;
                    ibuf[3] = ((int)arr[bi+2])&0x3F;
                }else{
                    ibuf[3] = -1;
                }
            }else{
                ibuf[2] = -1;
                ibuf[3] = -1;
            }
            for(int i = 0; i<4; i++){
                builder.append(ibuf[i]<0?'=':ALPHABET[ibuf[i]]);
            }
        }
        return builder.toString();
    }

    public static String encode(byte[] arr) {
        return encode(arr,arr.length);
    }
}
