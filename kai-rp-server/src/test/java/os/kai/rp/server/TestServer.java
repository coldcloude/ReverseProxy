package os.kai.rp.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class TestServer {
    public static void main(String[] args) throws Exception {
        new Thread(()->{
            ProxyServer server = new ProxyServer("127.0.0.1",13355,30000L);
            server.start();
        }).start();
        Set<String> sidSet = new HashSet<>();
        try(BufferedReader in = new BufferedReader(new InputStreamReader(System.in))){
            String line;
            while((line=in.readLine())!=null){
                int space = line.indexOf(" ");
                if(space>0){
                    String sid = line.substring(0,space);
                    String data = line.substring(space+1);
                    if(!sidSet.contains(sid)){
                        sidSet.add(sid);
                        ProxyHub.get().registerServerReceiver(sid,System.out::println);
                    }
                    ProxyHub.get().sendToClient(sid,data);
                }
            }
        }
    }
}
