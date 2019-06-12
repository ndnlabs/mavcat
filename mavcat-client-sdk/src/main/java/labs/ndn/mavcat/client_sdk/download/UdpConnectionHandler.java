package labs.ndn.mavcat.client_sdk.download;

import labs.ndn.mavcat.client_sdk.connection.LtpAddress;

import io.netty.channel.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class UdpConnectionHandler {

    private DatagramSocket socket;
    private InetAddress address;

    private int port;

    private DownloadTask task;

    private byte[] buf;

    private static final Logger logger = LoggerFactory.getLogger(UdpConnectionHandler.class);

    private boolean listenPacket = true;

    private Channel channel;



    public UdpConnectionHandler(LtpAddress address, DownloadTask task) {
        this.task = task;
        this.port = address.getPort();
        try {
            socket = new DatagramSocket();
            this.address = InetAddress.getByName(address.getHost());
            sendFirstPacket();
            socket.setReceiveBufferSize(104857600);
        } catch (SocketException | UnknownHostException e) {
            logger.error("error in create socket or get host");
        }

    }

    private void sendFirstPacket() {
        buf = ByteBuffer.allocate(4).putInt(task.getTaskId()).array();
        DatagramPacket packet = new DatagramPacket(buf,buf.length,address,port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            logger.error("error in send udp packet");
        }
        listenServerResponse();
    }

    private void listenServerResponse() {
        buf = new byte[task.getBlockSize()+4];
        DatagramPacket receivePacket = new DatagramPacket(buf,buf.length);
        while (listenPacket) {
            try {
                socket.receive(receivePacket);
            } catch (IOException e) {
                logger.error("io exception in receiving packet from server");
            }
            byte[] data = receivePacket.getData();
            int block = ByteBuffer.wrap(Arrays.copyOfRange(data,0,4)).getInt();
            int packetLength = receivePacket.getLength();
            if (packetLength <= 4) {
                continue;
            }
            byte[] cont = Arrays.copyOfRange(data,4,packetLength);

            task.addBlock(block,cont);
        }
    }



}
