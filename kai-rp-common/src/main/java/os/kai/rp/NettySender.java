package os.kai.rp;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class NettySender extends Thread implements Consumer<String> {

    private final AtomicBoolean running = new AtomicBoolean(true);

    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

    private final ChannelHandlerContext ctx;

    public NettySender(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void accept(String s) {
        queue.offer(s);
    }

    @Override
    public void run() {
        while(running.get()){
            Thread.interrupted();
            try{
                String data = queue.take();
                String str = ProxyTag.DATA_START+data+ProxyTag.DATA_END+"\r\n";
                NettyUtil.writeLine(ctx,str);
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
    }

    public void shutdown(){
        running.set(false);
        interrupt();
    }
}
