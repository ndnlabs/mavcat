package labs.ndn.mavcat.client_sdk.handler;

import labs.ndn.mavcat.client_sdk.connection.ConnectionManager;
import labs.ndn.mavcat.client_sdk.connection.LtpAddress;
import labs.ndn.mavcat.client_sdk.tools.SocketTools;
import labs.ndn.mavcat.client_sdk.tools.StringTools;
import labs.ndn.mavcat.client_sdk.config.ConnectionConfig;
import labs.ndn.mavcat.client_sdk.tools.CommandTools;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavcatClientAuthHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger logger = LoggerFactory.getLogger(MavcatClientVersionHandler.class);

    private static final String LOGIN_SUCCESSFUL = "success";

    private static final String LOGIN_FAILURE = "failed";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        LtpAddress remoteAddress = SocketTools.getLtpAddressByChannel(ctx.channel());
        String host = remoteAddress.getHost();
        int port = remoteAddress.getPort();

        if (msg.equals(LOGIN_SUCCESSFUL)) {
            logger.info("login successful to server {} {}",host,port);
            ctx.pipeline().remove(this);
            ctx.pipeline().addLast(new MavcatClientHandler());
            ConnectionManager.getInstance().setConnectionAuthed(remoteAddress);
        } else if (msg.equals(LOGIN_FAILURE)) {
            logger.info("login failure to server {} {}",host,port);
            ConnectionManager.getInstance().removeConnection(remoteAddress);
            CommandTools.print("wrong password");
        } else {
            String salt = msg;
            String serverPass = ConnectionConfig.getServerPasswordAndDelete(remoteAddress);

            String encrypt;
            if (serverPass != null) {
                encrypt = StringTools.sha256(serverPass + salt);
            } else {
                logger.info("empty password when connect {} {}", host, port);
                encrypt = "auth " + StringTools.sha256(salt);
            }

            SocketTools.writeAndFlush(ctx.channel(),encrypt);
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
    }
}
