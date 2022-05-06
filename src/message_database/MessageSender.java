package message_database;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.io.IOException;

/**
 * Message sender that sends datagram messages via UDP to other clients in the same chat channel.
 */
public class MessageSender {
    private DatagramChannel channel;
    private ByteBuffer buffer;

    private MessageSender(DatagramChannel channel) {
        this.channel = channel;
        buffer = ByteBuffer.allocate(2048);
    }

    /**
     * Initialize UDP channel and sets interface.
     */
    public static MessageSender create() {
        try {
            DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET);
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
            return new MessageSender(channel);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Put data into a buffer that will be sent through UDP.
     */
    public void sendMessage(Message message, InetSocketAddress group) throws IOException {
        buffer.clear();
        buffer.putInt(message.getSender().length());
        buffer.put(message.getSender().getBytes());
        buffer.putInt(message.getContent().length());
        buffer.put(message.getContent().getBytes());
        buffer.putLong(message.getTimestamp());
        buffer.flip();
        while (buffer.hasRemaining())
            channel.send(buffer, group);
    }
}
