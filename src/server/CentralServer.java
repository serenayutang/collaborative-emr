package server;

import java.util.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


import model.CommitParams;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * 1. The central service that supports users logging on, adding or removing
 * clients or servers, and other housekeeping tasks.
 * 2. The central server performs the function of paxos coordinator, and has
 * execute()
 * and learn() function to implement the fault-tolerance two phase protocol.
 * 3. Assumption: the central server never fails.
 */

public class CentralServer extends UnicastRemoteObject implements CentralServerInterface {
    private static String host;
    private static int voteNo = 0;
    private static final Server[] SERVERS = new Server[5];

    // server status: 0 -> empty and live, 1 -> busy, 2 -> down
    private static Map<Integer, Integer> statusMap;
    private static int coordinatorPort;

    public static Logger Log = LogManager.getLogger(CentralServer.class);

    /**
     * Constructor a central server object with given
     * host, central port and server port
     * 
     * @throws RemoteException
     */

    public CentralServer(String host, int coordinatorPort, int[] serverPorts) throws RemoteException {
        super();
        this.host = host;
        this.coordinatorPort = coordinatorPort;
        statusMap = new HashMap<>();
        bindRMI(coordinatorPort);
        for (int i = 0; i < serverPorts.length; i++) {

            SERVERS[i] = new Server(serverPorts[i], coordinatorPort);
            ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(SERVERS[i], 0);
            Registry registry = LocateRegistry.createRegistry(serverPorts[i]);
            registry.rebind(Server.class.getSimpleName() + serverPorts[i], stub);
            Log.debug("Server " + " at port " + serverPorts[i] + " is running...");

            // initialize the server to be live and empty
            // server status: 0 -> empty and live, 1 -> busy, 2 -> down
            statusMap.put(serverPorts[i], 0);
        }



    }

    public static void main(String[] args) throws RemoteException {

        Scanner sc = new Scanner(System.in);

        String[] input = new String[6];

        // validate user input
        boolean flag = true;
        while (flag) {

            System.out.println("Please enter the host: ");
            String host = sc.nextLine();
            System.out.println("Please enter the coordinator port and five server port, seperate by space: ");
            input = sc.nextLine().split(" ");

            if (input.length != 6) {
                Log.info("Invalid Input!");
                continue;
            }

            for (int i = 0; i < input.length; i++) {
                if (input[i].length() == 5 && isNumeric(input[i])) {
                    flag = false;
                } else {
                    Log.info("Invalid Input!");
                }

            }

        }
        // coordinator port is the first input port input[0]
        // the last five input port is server port input[1:]
        coordinatorPort = Integer.parseInt(input[0]);
        int[] serverPorts = new int[5];

        for (int i = 0; i < serverPorts.length; i++) {
            serverPorts[i] = Integer.parseInt(input[i + 1]);
        }
        // create a new central server object with user input port
        CentralServer cs = new CentralServer(host, coordinatorPort, serverPorts);

    }// end of main

    /**
     * function to binds RMI
     */
    public void bindRMI(int port) {
        try {
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("CentralServer", this);
            Log.debug("Central Server Coordinator " + port + " is running...");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

     /**
       * assign an alive server to client, loop through the servers until we found a live server
      */
    @Override
    public int assignAliveServerToClient() throws RemoteException {
        for(Server s: SERVERS){
            // server status: 0 -> empty and live, 1 -> busy, 2 -> down
            if(statusMap.get(s.port) == 0){

                Log.debug("Server port: " + s.port + "is assigned to client.");
                return s.port;
            }else{
                continue;
            }
        }
        Log.debug("NO alive server");
        return -1;
    }

    /**
     * Kill a server by updating the server status map
     */

    @Override
    public void killSlaveServer(int port) throws RemoteException {

        try {
            statusMap.put(port, 2);
            Log.debug("Server " + port + " is killed.");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // SF note: 其中用到的helpRecoverData() 需要Shuwei 在Server class 里 implement

    /**
     * Restart a slave server: assign a live server as helper to help the server
     * restart
     * and recover data.
     */
    @Override
    public boolean restartSlaveServer(int slaveServerPort) throws RemoteException {
        if (statusMap.get(slaveServerPort) != 2) {
            return false;
        }
        // start a new server
        Server server = new Server(slaveServerPort, coordinatorPort);
        ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(server, 0);
        Registry registry = LocateRegistry.getRegistry(slaveServerPort);
        registry.rebind(Server.class.getSimpleName() + slaveServerPort, stub);

        // loop through current servers until we found a live server to help recover
        // data
        for (Server s : SERVERS) {
            if (s.port == slaveServerPort)
                continue;
            if (statusMap.get(s.port) == 0) {
                try {
                    Log.info("Assign server " + s.port + " to help server " +
                            slaveServerPort + " to recover data.");
                    registry = LocateRegistry.getRegistry(s.port);
                    ServerInterface aliveServer = (ServerInterface) registry.lookup("Server" + s.port);
                    s.helpRecoverData(slaveServerPort);
                    statusMap.put(slaveServerPort, 0);
                    return true;

                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

            }

        }

        Log.info("No alive server found. Data recovery failed!");
        return false;

    }

    /**
     * Get the server status
     * 
     * @return 0 -> empty, 1 -> busy, 2 -> dead, -1 -> Not found
     */

    @Override
    public int getServerStatus(int port) throws RemoteException {
        if (statusMap.get(port) == null)
            return -1;
        return statusMap.get(port);

    }

    /**
     * Set server status as input
     *
     * @param port   server port #
     * @param status 0 -> empty, 1 -> busy, 2 -> dead
     * @throws RemoteException
     */
    @Override
    public void setServerStatus(int port, int status) throws RemoteException {
        statusMap.put(port, status);

    }

    // SF note: 暂不需要， 可删除
    @Override
    public void receiveNotification(String message) throws RemoteException {

    }

    // 暂不需要， 后续可删除
    // @Override
    // public int[] getPeers(int toPort) throws RemoteException {

    // return null;
    // }

    /**
     * When coordinator received a request msg, it will loop through each server to
     * check if the server is
     * live or not , if it is live, add to acceptors.
     * And then call Proposer.prepare() method to prepare the request.
     * 
     * @param msg
     * @return true if it is successfully prepared
     */

    public static boolean paxosPrepare(String msg) throws RemoteException {
        List<Acceptor> acceptors = new ArrayList<>();

        // loop through each server to add to acceptors
        for (Server s : SERVERS) {
            try {

                // s.prepareServer();
                if (statusMap.get(s.port) == 0) {
                    acceptors.add(s.gAcceptor());

                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.debug("Server " + s.port + " is down.");
            }

        }

        boolean result = Proposer.prepare(new Proposal(++voteNo, msg), acceptors);
        return result;

    }

    /**
     * All the other replicas need to learn to update its store
     * 
     * @param
     * @param port
     */


    public static void learn(CommitParams c, int port) throws RemoteException {
        for (Server server : SERVERS) {
            // argument server already updated, so ignore here
            if (server.port == port)
                continue;


            try {

           // learn
                Registry registry = LocateRegistry.getRegistry(server.port);
                ServerInterface stub = (ServerInterface) registry.lookup(Server.class.getSimpleName() + server.port);
                stub.executeCommit(c);

            } catch (Exception e) {
                e.printStackTrace();
                Log.debug("Server " + server.port + " is down.");
            }

        }

    }

//    // function to check if a string is numeric or not
    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

}
