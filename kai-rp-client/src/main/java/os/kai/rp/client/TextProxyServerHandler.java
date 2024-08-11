package os.kai.rp.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import os.kai.rp.LineDataNettySender;
import os.kai.rp.util.NettyUtil;
import os.kai.rp.TextProxyHub;
import os.kai.rp.TextProxyTag;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TextProxyServerHandler extends ChannelInboundHandlerAdapter {

    private final String sessionId;

    private final long timeout;

    private final Lock lock = new ReentrantLock();

    private volatile LineDataNettySender sender;

    private volatile Timer timer;

    static class Ticker extends TimerTask{
        private final ChannelHandlerContext ctx;
        Ticker(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }
        @Override
        public void run() {
            NettyUtil.writeLine(ctx,TextProxyTag.KEEP_SINGLE);
        }
    }

    public TextProxyServerHandler(String sessionId,long timeout) {
        this.sessionId = sessionId;
        this.timeout = timeout;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyUtil.writeLine(ctx,TextProxyTag.INIT_START+sessionId+TextProxyTag.INIT_END);
        lock.lock();
        try{
            sender = new LineDataNettySender(ctx);
            sender.start();
            TextProxyHub.get().registerServerReceiver(sessionId,sender);
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
            TextProxyHub.get().unregisterServerReceiver(sessionId);
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
        String data = TextProxyTag.unpackData(line);
        if(data!=null){
            TextProxyHub.get().sendToClient(sessionId,data);
        }
    }
}
