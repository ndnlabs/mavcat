package labs.ndn.mavcat.server.handler;

import labs.ndn.mavcat.server.connection.ConnectionManager;
import labs.ndn.mavcat.server.connection.GetTaskInfo;
import labs.ndn.mavcat.server.thread.ThreadManager;
import labs.ndn.mavcat.server.transfter.FirstTransmitWorker;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;

public class MavcatServerUdpHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private ChannelHandlerContext ctx;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        ByteBuf buf = msg.content();
        int taskId = buf.readInt();
        // this is a initial block which request transmission
        if (taskId >0) {
            // let client_sdk wait for some time
            Thread.sleep(1000);

            GetTaskInfo info = ConnectionManager.getTask(taskId);
            // do nothing
            if (info == null) {
                return;
            }
            InetSocketAddress sender = msg.sender();
            // the message is not sent from valid address
            if (!info.getTcpAddress().getHost().equals(sender.getHostString())) {
                return;
            }
            info.setUdpChannel(ctx.channel());
         //   info.setRemoteAddress(sender);

            FirstTransmitWorker worker = new FirstTransmitWorker(taskId,ctx,msg.sender());
            ThreadManager.submit(worker);
            info.setTransferTask(worker);

        } else {
            // maybe receive packets

        }

    }


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        this.ctx = ctx;
    }
}
