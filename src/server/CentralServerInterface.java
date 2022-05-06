package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * CentralServerInterface.java
 * Interface of class CentralServer
 */

public interface CentralServerInterface extends Remote {

    public int assignAliveServerToClient() throws RemoteException;

    public void killSlaveServer(int slaveServerPort) throws RemoteException;

    public boolean restartSlaveServer(int slaveServerPort) throws RemoteException;

    public int getServerStatus(int port) throws RemoteException;

    public void setServerStatus(int port, int status) throws RemoteException;

    public void receiveNotification(String message) throws RemoteException;

    // public int[] getPeers(int toPort) throws RemoteException;


}
