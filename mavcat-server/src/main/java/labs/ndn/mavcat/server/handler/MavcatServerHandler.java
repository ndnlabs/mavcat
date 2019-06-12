package labs.ndn.mavcat.server.handler;

import labs.ndn.mavcat.server.config.ServerConfig;
import labs.ndn.mavcat.server.connection.ChannelConfig;
import labs.ndn.mavcat.server.connection.ConnectionManager;
import labs.ndn.mavcat.server.connection.GetTaskInfo;
import labs.ndn.mavcat.server.retransmit.RetranmitWorker;
import labs.ndn.mavcat.server.tools.FileTools;
import labs.ndn.mavcat.server.tools.SocketTools;
import labs.ndn.mavcat.server.tools.StringTools;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

@ChannelHandler.Sharable
public class MavcatServerHandler extends SimpleChannelInboundHandler<String> {
    // list dir command
    private final static String DIR = "dir";

    // download file
    private static final String GET = "get";

    public static final String VERSION = "version mavcatp 1.0";

    private static final String LOGIN_SUCCESSFUL = "success";

    private static final String LOGIN_FAILURE = "failed";

    private ChannelHandlerContext ctx;

    private static final String START_BYTE = "start_byte";
    private static final String BLOCK_SIZE = "block_size";
    private static final String RECEIVE_RATE = "receive_rate";
    //Todo optimize logic
    // this can buffer max 10g file, sometimes this is not enough
    private BlockingQueue<Integer> retransmit = new LinkedBlockingDeque<>();
    private boolean canStartRetranmit = false;

    private RetranmitWorker retranmitWorker;

    private static final Logger logger = LoggerFactory.getLogger(MavcatServerHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println(msg);
        if (msg.startsWith(DIR)) {
            handleDir(msg);
        } else if (msg.startsWith(GET)) {
            handleGet(msg);
        } else if (msg.startsWith("version")) {
            handleVersion(msg);
        } else if (msg.startsWith("auth")) {
            handleAuth(msg);
        } else if (msg.startsWith("r")) {
            handleTransmit(msg);
        } else if (msg.startsWith("setbandwidth")) {
            handleSetBandwidth(msg);
        } else if (msg.startsWith("stop")) {
            handleStop(msg);
        }
    }

    private void handleDir(String msg) {
        String rootDir = ServerConfig.getDir();
        if (msg.equals(DIR)) {
            SocketTools.writeAndFlush(ctx.channel(),FileTools.getDirListReprezenation(rootDir,"/"));
        } else {
            String[] command_list = msg.split(" ");
            String path = command_list[1];
            String requestPath = path;
            if (!path.endsWith("/")) {
                path = path + "/";
            } else if (!path.startsWith("/")) {
                path = "/" + path;
            }
            SocketTools.writeAndFlush(ctx.channel(),FileTools.getDirListReprezenation(rootDir+path,requestPath));
        }
    }

    private void handleVersion(String msg) {
        SocketTools.writeAndFlush(ctx.channel(),VERSION);
        // send random to start auth
        if (msg.equals(VERSION)) {
            //  LtpAddress address = SocketTools.getLtpAddressByChannel(ctx.channel());
            String random = StringTools.generateRandomString(10);
            ConnectionManager.addChannelConfig(ctx.channel(),new ChannelConfig(random));
            // send random string to start auth process
            SocketTools.writeAndFlush(ctx.channel(),random);

        }
    }

    private void handleAuth(String msg) {
        ChannelConfig config = ConnectionManager.getChannelConfig(ctx.channel());
        if (config == null) {
            // we have not sent random salt
            SocketTools.writeAndFlush(ctx.channel(),"protocol error");
        }
        String serverPass = ServerConfig.getPassword();


        String[] auth_a = msg.split(" ");
        if (auth_a.length < 2) {
            SocketTools.writeAndFlush(ctx.channel(),"auth have not enough parameters");
            return;
        }
        String requestString = auth_a[1];
        String rightString;
        if (serverPass == null) {
            rightString = StringTools.sha256(config.getRandom());
        } else {
            rightString = StringTools.sha256(serverPass + config.getRandom());
        }

        if (requestString.equals(rightString)) {
            SocketTools.writeAndFlush(ctx.channel(),LOGIN_SUCCESSFUL);
            ConnectionManager.getChannelConfig(ctx.channel()).setAuthed(true);
        } else {
            SocketTools.writeAndFlush(ctx.channel(),LOGIN_FAILURE);
        }
    }

    private void handleGet(String msg) {
        String[] command_list = msg.split(" ");
        if (command_list.length != 5) {
            SocketTools.writeAndFlush(ctx.channel(),"error: get command format error");
        } else {
            String rootDir = ServerConfig.getDir();
            String filepath = rootDir + '/' + command_list[1];
            File file = new File(filepath);
            if (!file.exists()) {
                SocketTools.writeAndFlush(ctx.channel(),"error: file not exists");
            } if (file.isDirectory()){
                SocketTools.writeAndFlush(ctx.channel(),"error: only file can be downloaded");
            } else {
                int startByte = parseInteger(command_list[2],START_BYTE);
                int blockSize = parseInteger(command_list[3],BLOCK_SIZE);
                float receiveRate = parseFloat(command_list[4]);
                if (receiveRate == -1) {
                    receiveRate = ChannelConfig.DEFAULT_RECEIVE_RATE;
                }

                if (startByte > file.length()) {
                    startByte = 0;
                }

                try {
                    int taskId = ConnectionManager.addTask(filepath,command_list[1],SocketTools.getLtpAddressByChannel(ctx.channel()),startByte,blockSize,receiveRate,ctx.channel());
                    GetTaskInfo info = ConnectionManager.getTask(taskId);
                    SocketTools.writeAndFlush(ctx.channel(),"task "+taskId+" "+ServerConfig.getUdpPort() + " " + info.getBlockCount() + " " + info.getRequestFilename()+" " +info.getHash());
                } catch (IOException e) {
                    e.printStackTrace();
                    SocketTools.writeAndFlush(ctx.channel(),"error: in read file");
                }
            }
        }
    }

    private void handleTransmit(String msg) {
        String[] command_list = msg.split(" ");
        int taskId = parseTaskId(command_list[1]);
        if (taskId == -1) {
            SocketTools.writeAndFlush(ctx.channel(),"error: Task id should be a number");
            return;
        }
        GetTaskInfo info = ConnectionManager.getTask(taskId);
        if (info == null) {
            SocketTools.writeAndFlush(ctx.channel(),"error: Task id not exist");
            return;
        }

        UdpHandler handler = info.getUdpHandler();
        if (handler == null) {
            SocketTools.writeAndFlush(ctx.channel(),"error: do not found handler");
            return;
        }
        //Todo validate sender address, for now, secure is useless

        for (int i = 2; i < command_list.length; i++) {
            try {
                int blockid = Integer.parseInt(command_list[i]);
                handler.addReTransmit(blockid);
            } catch (NumberFormatException e) {
                continue;
            }
        }



    }

    private void handleSetBandwidth(String msg) {
        String[] command_list = msg.split(" ");

        int taskId = parseTaskId(command_list[1]);
        if (taskId == -1) {
            SocketTools.writeAndFlush(ctx.channel(),"error: Task id should be a number");
            return;
        }

        float receive_rate = parseFloat(command_list[2]);
        if (receive_rate == -1) {
            return;
        }

        GetTaskInfo info = ConnectionManager.getTask(taskId);
        if (info != null) {
            info.setReceiveRate(receive_rate);
        } else {
            SocketTools.writeAndFlush(ctx.channel(),"error: taskid not found");
        }
    }

    //ToDo validate user credientials
    private void handleStop(String msg) {
        String[] command_list = msg.split(" ");
        int taskId = parseTaskId(command_list[1]);
        GetTaskInfo task = ConnectionManager.getTask(taskId);
        if (task != null) {
            task.getUdpHandler().stopTransfer();
        }
    }


    // when parse failure, use default value
    private int parseInteger(String str,String type) {
        int val = 0;
        try {
            val =  Integer.parseInt(str);
        } catch (NumberFormatException e) {
            if (type.equals(START_BYTE)) {
                // do nothing,0
            } else if (type.equals(BLOCK_SIZE)) {
                val = ChannelConfig.DEFAULT_BLOCK_SIZE;
            } else if (type.equals(RECEIVE_RATE)) {
                val = ChannelConfig.DEFAULT_RECEIVE_RATE;
            }
        }

        return val;
    }

    private float parseFloat(String str) {
        float val = 0;
        try {
            val = Float.parseFloat(str);
        } catch (NumberFormatException e) {
            return -1;
        }
        return val;
    }

    private int parseTaskId(String str)  {
        int taskId = 0;
        try {
            taskId = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return -1;
        }
        return taskId;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        this.ctx = ctx;
    }

    public void setCanStartRetranmit(boolean canStartRetranmit) {
        this.canStartRetranmit = canStartRetranmit;
    }
}
