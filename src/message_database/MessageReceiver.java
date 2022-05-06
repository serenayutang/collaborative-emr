package message_database;

import client.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.IOException;

/**
 * Message receiver class of the message channel
 */
public class MessageReceiver implements Runnable {

    private List<Message> messages;
    private MembershipKey activeGroup;
    private DatagramChannel channel;
    private NetworkInterface networkInterface;

    public MessageReceiver() {
        messages = new ArrayList<>();
        activeGroup = null;
        channel = null;
        networkInterface = null;
    }

    @Override
    public void run() {
        try {
            Selector selector = Selector.open();
            channel = DatagramChannel.open(StandardProtocolFamily.INET);
            networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.bind(new InetSocketAddress(Client.UDP_PORT));
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            ByteBuffer buffer = ByteBuffer.allocate(2048);
            while (!Thread.currentThread().isInterrupted()) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isReadable()) {
                        buffer.clear();
                        InetSocketAddress socketAddress = (InetSocketAddress) channel.receive(buffer);
                        if (socketAddress != null) {
                            buffer.flip();
                            try {
                                String sender = getString(buffer);
                                String content = getString(buffer);
                                long timestamp = buffer.getLong();
                                Message message = new Message(sender, content, timestamp);
                                synchronized (messages) {
                                    messages.add(message);
                                }
                            } catch (BufferUnderflowException e) {
                                System.out.println(e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private String getString(ByteBuffer buffer) {
        int size = buffer.getInt();
        byte[] res = new byte[size];
        buffer.get(res, 0, size);
        return new String(res);
    }

    public List<Message> retrieve() {
        List<Message> current = new ArrayList<>(messages);
        messages.clear();
        return current;
    }

    public void setNewGroup(long group) throws IOException {
        if (channel != null) {
            if (group > 0) {
                byte[] rawAddress = MessageManager.longToAddress(group).getAddress();
                if (activeGroup != null && activeGroup.isValid()) {
                    activeGroup.drop();
                }
                activeGroup = channel.join(InetAddress.getByAddress(rawAddress), networkInterface);
            } else {
                activeGroup.drop();
                activeGroup = null;
            }
        }
    }

    public InetAddress getActiveGroup() {
        return activeGroup.group();
    }
}
