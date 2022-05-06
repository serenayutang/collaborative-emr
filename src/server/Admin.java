package server;


import java.util.Scanner;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Implements an administrator server that can send kill/restart requests to
 * central server.
 * To test fault-tolerance paxos
 */

public class Admin {

    public static Logger Log = LogManager.getLogger(Admin.class);

    public static void main(String[] args) {
        try {

            Scanner sc = new Scanner(System.in);

            System.out.println("Please enter the center server coordinator port: ");
            int centerPort = Integer.parseInt(sc.nextLine());
            Registry centralRegistry = LocateRegistry.getRegistry(centerPort);
            CentralServerInterface centralServer = (CentralServerInterface) centralRegistry.lookup("CentralServer");

            System.out.println("-----------------------------------");
            System.out.println(" Please enter your command: ");
            System.out.println("\nEnter: kill <port> to kill a server");
            System.out.println("\nEnter: restart <port> to restart a server");
            System.out.println("-----------------------------------");

            boolean flag = true;
            while (flag) {
                System.out.print("admin@127.0.0.1# ");
                String argsLine = sc.nextLine();
                String[] arguments = argsLine.split(" ");
                if (argsLine.length() > 0 && arguments.length > 0) {

                    try {

                        if (arguments.length >= 2 && arguments[0].equalsIgnoreCase("kill")) {
                            try {
                                int port = Integer.parseInt(arguments[1]);
                                centralServer.killSlaveServer(port);
                                Log.debug("Server " + port + " is killed.");
                            } catch (NumberFormatException ex) {
                                throw new IllegalArgumentException();
                            }
                        } else if (arguments.length >= 2 && arguments[0].equalsIgnoreCase("restart")) {
                            try {
                                int port = Integer.parseInt(arguments[1]);
                                boolean result = centralServer.restartSlaveServer(port);
                                if (result) {
                                    Log.debug("Server " + port + " is restarted and data is recovered.");

                                } else {
                                    Log.debug("Server " + port + " failed to restarted.");
                                }

                            } catch (NumberFormatException ex) {
                                throw new IllegalArgumentException();
                            }
                        }
                    } catch (IllegalArgumentException ex) {
                        ex.printStackTrace();
                        Log.debug("Unsupported arguments. Please try again.");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.debug("Internal error. Please try again.");
                    }
                }
                System.out.print("Continue to the next command? Press 'F' to quit: ");
                String option = sc.nextLine();
                if (option.equalsIgnoreCase("F")) {
                    flag = false;

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

