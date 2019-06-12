package labs.ndn.mavcat.client_sdk.handler;

import labs.ndn.mavcat.client_sdk.connection.LtpAddress;
import labs.ndn.mavcat.client_sdk.connection.ConnectionManager;
import labs.ndn.mavcat.client_sdk.tools.CommandTools;
import labs.ndn.mavcat.client_sdk.tools.SocketTools;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavcatClientVersionHandler extends SimpleChannelInboundHandler {
    public static final String VERSION = "version mavcatp 1.0";
    private static final Logger logger = LoggerFactory.getLogger(MavcatClientVersionHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        String serverVer = (String ) msg;
        if (serverVer.equals(VERSION)) {
            // protocol version match
            // remove this handler
            ctx.pipeline().addLast(new MavcatClientAuthHandler());
            ctx.pipeline().remove(this);
        } else {
            LtpAddress remoteAddress = SocketTools.getLtpAddressByChannel(ctx.channel());
            logger.info("client_sdk version and server version not match! server {} {}", remoteAddress.getHost(), remoteAddress.getPort());



            ConnectionManager.getInstance().removeConnection(remoteAddress);
            CommandTools.print("server version and client_sdk version not match");

            ctx.close();
        }
    }
}
