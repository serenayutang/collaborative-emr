package client;

import doctor_database.*;
import message_database.*;

import model.*;
import server.*;
import com.healthmarketscience.rmiio.RemoteInputStreamServer;
import com.healthmarketscience.rmiio.SimpleRemoteInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Implements Client class.
 */
public class Client {

  private static String CENTRAL_SERVER_HOST = "127.0.0.1";
  private static int CENTRAL_SERVER_RMI_PORT;
  public static int UDP_PORT = 4567;
  private static String DATA_DIR;
  private String clientName;
  private ServerInterface serverInterface;
  private MessageSender messageSender;
  private MessageReceiver messageReceiver;
  private CheckMessage checkMessage = new CheckMessage(serverInterface);
  private Session session;
  private Doctor doctor;

  public static Logger Log = LogManager.getLogger(Client.class);

  /**
   * constructor
   * @param clientName
   */
  public Client(String clientName) {
    try {
      this.clientName = clientName;
      DATA_DIR = "./client_data_" + clientName + "/";
      messageReceiver = new MessageReceiver();
      checkDataDirectory();
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        Log.info(clientName + " is shutting down...");
        checkMessage.stop();
      }));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Check if the data directory  is a valid directory, otherwise creates it.
   */
  private static void checkDataDirectory() {
    File dataDir = new File(DATA_DIR);
    if (!dataDir.isDirectory() || !dataDir.exists()) dataDir.mkdirs();
  }

  /**
   * Connect to a server.
   */
  private void connect( ) throws Exception {
    serverInterface = this.getAvailableServer();
    checkMessage = new CheckMessage(this.serverInterface);
    messageSender = MessageSender.create();
    Thread thread = new Thread(messageReceiver);
    thread.start();
    if (messageSender == null) throw new IOException();
  }

  /**
   * Print help message.
   */
  private void printHelpMessage() {
    String message =
            "Client Command Summary:\n" +
                    "  help: to show this help message\n\n" +
                    "  register DOCTOR PWD: to register a new account with doctorName DOCTOR and password PWD\n" +
                    "  login DOCTOR PWD: to login using doctorName DOCTOR and PWD credentials\n" +
                    "  create PATIENT_FILE SECTION: to create a new file PATIENT_FILE and contains SECTION sections\n" +
                    "  edit PATIENT_FILE SECTION: to edit the section of patient file\n" +
                    "  stopedit: to stop the current editing session if exists\n" +
                    "  logout: to logout\n" +
                    "  list: to list all the accessible and editable patient files\n" +
                    "  share DOCTOR PATIENT_FILE: to share a patient file with another doctor\n" +
                    "  news: to get all the news\n" +
                    "  unread: to retrieve all the unread chat messages\n" +
                    "  send TEXT: to send the TEXT message regarding the document being edited\n" +
                    "  ";
    Log.info(message);
  }

  /**
   * interprets and executes the command.
   */
  private void interpretsCommand() {
    String command = null;
    Scanner input = new Scanner(System.in);
    boolean isAlive = true;
    while (isAlive) {
      System.out.print("please enter a command (enter help to show instructions):\n");
      String argsLine = input.nextLine();
      String[] args = argsLine.split(" ");
      if (argsLine.length() > 0 && args.length > 0) {
        command = args[0].toLowerCase();
        try {
          switch (command) {
            case "exit":
            case "quit":
              if (session == null) {
                isAlive = false;
              } else {
                Log.error("Please logout first.");
              }
              break;
            case "register":
              if (args.length > 2) {
                String username = args[1];
                String password = args[2];
                register(username, password);
              } else throw new IllegalArgumentException();
              break;
            case "login":
              if (args.length > 2) {
                String username = args[1];
                String password = args[2];
                login(username, password);
              } else throw new IllegalArgumentException();
              break;
            case "create":
              if (args.length > 2) {
                try {
                  String docName = args[1];
                  int secNum = Integer.valueOf(args[2]);
                  create(docName, secNum);
                } catch (NumberFormatException ex) {
                  throw new IllegalArgumentException();
                }
              } else throw new IllegalArgumentException();
              break;
            case "edit":
              if (args.length > 2) {
                String tmpFile = null;
                if (args.length > 3) tmpFile = args[3];
                try {
                  String docName = args[1];
                  int secNum = Integer.valueOf(args[2]);
                  edit(docName, secNum, tmpFile);
                } catch (NumberFormatException ex) {
                  throw new IllegalArgumentException();
                }
              } else throw new IllegalArgumentException();
              break;
            case "stopedit":
              stopEdit();
              break;

            case "logout":
              logout();
              break;
            case "list":
              documentsList();
              break;
            case "share":
              if (args.length > 2) {
                String username = args[1];
                String docName = args[2];
                share(username, docName);
              } else throw new IllegalArgumentException();
              break;
            case "news":
              printNewMessage();
              break;
            case "unread":
              showMessages();
              break;
            case "send":
              if (args.length > 1) {
                String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                sendMessage(text);
              } else throw new IllegalArgumentException();
              break;
            case "help":
              printHelpMessage();
              break;
            default:
              throw new IllegalArgumentException();
          }
        } catch (IllegalArgumentException ex) {
         ex.printStackTrace();
          Log.debug("Unsupported arguments. Please try again.");
        } catch (Exception e) {
          e.printStackTrace();
          Log.error("Internal error. Please try again.");
        }
      }
    }
  }

  /**
   * get an available server from central server.
   *
   * @return assigned server
   */
  private ServerInterface getAvailableServer() {
    try {
      Registry centralRegistry = LocateRegistry.getRegistry(CENTRAL_SERVER_HOST, CENTRAL_SERVER_RMI_PORT);
      CentralServerInterface central = (CentralServerInterface) centralRegistry.lookup("CentralServer");
      int serverPort = central.assignAliveServerToClient();
      Log.info("Connected to server: " + serverPort);

      Registry registry = LocateRegistry.getRegistry(serverPort);
      ServerInterface serverInterface = (ServerInterface) registry.lookup(Server.class.getSimpleName() + serverPort);
      return serverInterface;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Unable to connect to a live server!");
    }
  }


  /**
   * Register a new doctor.
   */
  private void register(String username, String password) throws Exception {
    try {
      Result result = serverInterface.createUser(new Doctor(username, password));
      if (result.getStatus() == 1) {
        Log.info("User " + username + " registered successfully!");
      } else {
        Log.error(result.getMessage());
        return;
      }
    } catch (Exception e) {
      Log.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * user login
   *
   * @param username
   * @param password
   * @throws Exception
   */
  private void login(String username, String password) throws Exception {
    try {
      if (session == null) {
        Result result = serverInterface.login(new Doctor(username, password));
        if (result.getStatus() == 0) {
          Log.error(result.getMessage());
          return;
        }
        String token = result.getMessage();
        doctor = new Doctor(username,password);
        checkMessage.setDoctor(doctor);
        new Thread(checkMessage).start();
        session = new Session(token, doctor);
        Log.info("Logged in successfully as " + username);
      } else {
        Log.error("You're already logged in.");
      }
    } catch (Exception e) {
      Log.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * User logout.
   */
  private void logout() throws Exception {
    try {
      if (session != null) {
        if (!session.isEditing()) {
          Result result = serverInterface.logout(new Doctor(session.getDoctor().getDoctorName()));
          if (result.getStatus() == 0) {
            Log.error(result.getMessage());
            return;
          }
          session = null;
          checkMessage.clearMessageList();
          checkMessage.setDoctor(null);
          Log.info("Successfully logged out.");
        } else Log.error("You should 'endedit' before logging out");
      } else Log.error("You're not logged in");
    } catch (Exception e) {
      Log.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Create a new Document.
   *
   * @param docName   new document name
   * @param secNumber number of the new document
   */
  private void create(String docName, int secNumber) throws Exception {
    try {
      if (session != null) {
        Doctor user = new Doctor(session.getDoctor().getDoctorName());
        Request request = new Request();
        request.setToken(session.getSessionToken());
        request.setDocName(docName);
        request.setSectionNum(secNumber);
        Result result = serverInterface.createDocument(user, request);
        if (result.getStatus() == 0) {
          Log.error(result.getMessage());
          return;
        }
        Log.info("Successfully create a new document.");
      } else Log.error("You're not logged in");
    } catch (Exception e) {
      Log.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * edit a document. Retrieve file stream from server and write in file.
   *
   * @param docName        document name
   * @param secNumber      section index
   * @param fileName       output filename
   */
  private void edit(String docName, int secNumber, String fileName) throws Exception {
    FileChannel fileChannel = null;
    OutputStream fileStream = null;
    try {
      if (session != null) {
        String filepath = fileName != null ? fileName : DATA_DIR + docName + "_" + secNumber;
        try {
          Request request = new Request();
          request.setDocName(docName);
          request.setSectionNum(secNumber);
          request.setToken(session.getSessionToken());
          Doctor doctor=new Doctor(session.getDoctor().getDoctorName());
          Result result = serverInterface.edit(doctor, request);
          if (result.getStatus() == 0) {
            Log.error(result.getMessage());
            return;
          }
          fileChannel = FileChannel.open(Paths.get(filepath), StandardOpenOption.CREATE,
                  StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
          fileStream = Channels.newOutputStream(fileChannel);
          fileStream.write(RemoteInputStreamUtils.toBytes(result.getRemoteInputStream()));

          session.setOccupiedFilePath(filepath);
          session.setOccupiedFileName(docName);
          session.setSectionIndex(secNumber);
          long address = Long.parseLong(result.getMessage());
          messageReceiver.setNewGroup(address);
        } catch (IOException e) {
          Log.error(e.getMessage());
          throw new RuntimeException(e);
        }
      } else Log.error("You haven't logged in");
    } catch (Exception e) {
      Log.error(e.getMessage());
      throw new RuntimeException(e);
    } finally {
      try {
        if (fileChannel != null) {
          fileChannel.close();
        }
        if (fileChannel != null) {
          fileStream.close();
        }
      } catch (Exception e) {
        Log.error(e.getMessage());
      }
    }
  }

  /**
   * Stops edit.
   */
  private void stopEdit() throws Exception {
    try {
      if (session != null) {
        if (session.isEditing()) {
          try (FileChannel fileChannel = FileChannel.open(Paths.get(session.getOccupiedFilePath()), StandardOpenOption.READ);
               InputStream stream = Channels.newInputStream(fileChannel)) {
            RemoteInputStreamServer remoteFileData = new SimpleRemoteInputStream(stream);
            Request request = new Request();
            request.setDocName(session.getOccupiedFileName());
            request.setSectionNum(session.getSectionIndex());
            request.setRemoteInputStream(remoteFileData);
            request.setToken(session.getSessionToken());
            Result result = serverInterface.editEnd(new Doctor(session.getDoctor().getDoctorName()), request);
            if (result.getStatus() == 0) {
              Log.error(result.getMessage());
              return;
            }
            session.setOccupiedFilePath(null);
            session.setOccupiedFileName(null);
            session.setSectionIndex(0);
            messageReceiver.setNewGroup(0);
          } catch (IOException ex) {
            Log.error(ex.getMessage());
          }
        } else Log.error("You haven't editing any section");
      } else Log.error("You haven't logged in");
    } catch (Exception e) {
      Log.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }



  /**
   * Gets the documents list.
   */
  private void documentsList() throws Exception {
    try {
      if (session != null) {
        Request request = new Request();
        request.setToken(session.getSessionToken());
        Result result = serverInterface.listOwnedDocs(new Doctor(session.getDoctor().getDoctorName()), request);
        Log.info(result.getMessage());
      } else Log.error("You're not logged in");
    } catch (Exception e) {
      Log.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Shares a document with another user. The shared user will also
   * receive a notification.
   */
  private void share(String user, String docName) throws Exception {
    try {
      Request request = new Request();
      request.setToken(session.getSessionToken());
      request.setTargetUser(new Doctor(user));
      request.setDocName(docName);
      Result result = serverInterface.shareDoc(new Doctor(session.getDoctor().getDoctorName()), request);

      if (result.getStatus() == 1) {
        Log.info("Document shared successfully");
      } else {
        Log.error(result.getMessage());
        return;
      }
    } catch (Exception e) {
      Log.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }



  /**
   * Print all new notifications.
   */
  private void printNewMessage() throws Exception {
    try {
      if (session != null) {
        List<String> notifications = checkMessage.getAllMessages();
        if (!notifications.isEmpty())
          Log.info("You have permission on these new documents: " + String.join(",", notifications));
        else Log.error("No news available");
      } else Log.error("You're not logged in");
    } catch (Exception e) {
      Log.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Shows all new received messages.
   */
  private void showMessages() throws Exception {
    try {
      if (session != null) {
        if (session.isEditing()) {
          List<Message> messages = messageReceiver.retrieve();
          for (Message message : messages)
            Log.info(message);
        } else Log.info("You're not editing any document");
      } else Log.error("You're not logged in");
    } catch (Exception e) {
      Log.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Send a UDP multicast packet
   */
  private void sendMessage(String text) throws Exception {
    try {
      if (session != null) {
        if (session.isEditing()) {
          InetAddress groupAddress;
          if ((groupAddress = messageReceiver.getActiveGroup()) != null) {
            try {
              Message message = new Message(session.getDoctor().getDoctorName(), text, System.currentTimeMillis());
              messageSender.sendMessage(message, new InetSocketAddress(groupAddress, UDP_PORT));
            } catch (Exception ex) {
              Log.error(ex.getMessage());
            }
          } else Log.error("Generic message sending error");
        } else Log.error("You're not editing any document");
      } else Log.error("You're not logged in");
    } catch (Exception e) {
      Log.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * client start up.
   */
  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);

    System.out.println("Please enter the central server port to connect: ");
    CENTRAL_SERVER_RMI_PORT = Integer.parseInt(sc.nextLine());

    System.out.println("Please enter a client name: ");

    String clientName = sc.nextLine();
    Client client = new Client(clientName);
    try {
      client.connect();
      client.interpretsCommand();
    } catch (Exception ex) {
      Log.error(ex);
    } finally {
      try {
        client.checkMessage.stop();
      } catch (Exception ignore) {
      }
    }
  }

}
