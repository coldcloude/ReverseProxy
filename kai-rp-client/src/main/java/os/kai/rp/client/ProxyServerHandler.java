package os.kai.rp.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import os.kai.rp.NettySender;
import os.kai.rp.NettyUtil;
import os.kai.rp.ProxyHub;
import os.kai.rp.ProxyTag;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ProxyServerHandler extends ChannelInboundHandlerAdapter {

    private final String sessionId;

    private final long timeout;

    private final Lock lock = new ReentrantLock();

    private volatile NettySender sender;

    private volatile Timer timer;

    static class Ticker extends TimerTask{
        private final ChannelHandlerContext ctx;
        Ticker(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }
        @Override
        public void run() {
            NettyUtil.writeLine(ctx,ProxyTag.KEEP_SINGLE+"\r\n");
        }
    }

    public ProxyServerHandler(String sessionId, long timeout) {
        this.sessionId = sessionId;
        this.timeout = timeout;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyUtil.writeLine(ctx,ProxyTag.INIT_START+sessionId+ProxyTag.INIT_END+"\r\n");
        lock.lock();
        try{
            sender = new NettySender(ctx);
            sender.start();
            ProxyHub.get().registerServerReceiver(sessionId,sender);
            timer = new Timer();
            timer.schedule(new Ticker(ctx),timeout/3);
        }
        finally{
            lock.unlock();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        lock.lock();
        try{
            //stop timer
            if(timer!=null){
                timer.cancel();
                timer = null;
            }
            //stop session
            ProxyHub.get().removeServerReceiver(sessionId);
            //stop sender
            if(sender!=null){
                sender.shutdown();
                sender = null;
            }
        }
        finally{
            lock.unlock();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception {
        String line = NettyUtil.readLine(msg);
        String data = ProxyTag.unpackData(line);
        if(data!=null){
            ProxyHub.get().sendToClient(sessionId,data);
        }
    }
}
