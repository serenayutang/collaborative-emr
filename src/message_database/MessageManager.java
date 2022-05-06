package message_database;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chat database of group chat among clients
 */
public class MessageManager implements Serializable {
    private static final long serialVersionUID = -1L;

    /**
     * Full range is from 233.0.0.1 to 233.255.255.255;
     */
    private static final long START_ADDR = 3909091329L;
    private static final long END_ADDR = 3925868543L;

    private ConcurrentHashMap<String, Long> messageDatabase;

    public MessageManager() {
        messageDatabase = new ConcurrentHashMap<>();
    }

    /**
     * Convert IP address to long.
     */
    public static long addressToLong(InetAddress address) {
        int result = 0;
        for (byte b : address.getAddress()) {
            result = result << 8 | (b & 0xFF);
        }
        return result;
    }

    /**
     * Converts a long value to its InetAddress representation.
     */
    public static InetAddress longToAddress(long address) throws UnknownHostException {
        return InetAddress.getByName(String.valueOf(address));
    }

    /**
     * Get available multicast address.
     */
    public long getNextAvailableAddress() {
        for (long address = START_ADDR; address <= END_ADDR; address++) {
            return address;
        }
        return -1L;
    }

    public void putAddress(String patientFile, long address) {
        if (!messageDatabase.contains(patientFile)) {
            messageDatabase.put(patientFile, address);
        }
    }

    public long getResultAddress(String patientFile) {
        return messageDatabase.get(patientFile);
    }

    /**
     * Remove from message database.
     */
    public void remove(String patientFile) {
        messageDatabase.remove(patientFile);
    }

    public ConcurrentHashMap<String, Long> getMessageDatabase() {
        return messageDatabase;
    }

    public void setMessageDatabase(ConcurrentHashMap<String, Long> messageDatabase) {
        this.messageDatabase = messageDatabase;
    }
}
