package server;


import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.SimpleRemoteInputStream;
import doctor_database.ActiveDoctorData;
import doctor_database.Doctor;
import doctor_database.DoctorData;
import document_database.PatientFile;
import document_database.DocumentData;
import document_database.Section;
import message_database.MessageManager;
import model.*;
import back_up_database.BackUpData;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

/**
 * Implements the server class that contains methods for doctor/PatientFile administration.
 */

public class Server implements ServerInterface {

    public int port;
    // for paxos
    private Proposer proposer;
    private Acceptor acceptor;

    private String DATA_DIR;
    private final String USER_DB_NAME = "DoctorDB.dat";
    private final String DOC_DB_NAME = "DocDB.dat";

    private int centralPort;
    private DocumentData documentData;
    private ActiveDoctorData activeDoctorData;
    private DoctorData doctorData;
    private MessageManager messageManager;


    public static Logger Log = LogManager.getLogger(Server.class);

    /**
     * constructor to initialize a server with given port and given central port
     *
     * @param port
     */

    public Server(int port, int centralPort) throws RemoteException {

        try {

            this.port = port;
            this.centralPort = centralPort;
            this.DATA_DIR = "./server_data_" + port + "/";

            createDataDirectory();

            doctorData = initDoctorDB();
            documentData = initDocumentDB();
            activeDoctorData = new ActiveDoctorData();
            messageManager = new MessageManager();

//            tempStorage = new ConcurrentHashMap<>();
//            prepareResponseMap = new ConcurrentHashMap<>();
//            commitResponseMap = new ConcurrentHashMap<>();


            System.setProperty("java.net.preferIPv4Stack", "true");
            // store memory database when shutting down with shutdown hook
            doctorData = initDoctorDB();
            documentData = initDocumentDB();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Log.info("Server " + port + " is shutting down...");
                storeDoctorDB();
                storeDocumentsDB();
            }));


            proposer = new Proposer();
            acceptor = new Acceptor(port);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Acceptor gAcceptor() {
        return this.acceptor;
    }


    /**
     * Create data directory according to current server DATA_DIR
     */
    private void createDataDirectory() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.isDirectory() || !dataDir.exists()) dataDir.mkdirs();
    }


    /**
     * initialize doctor database
     *
     * @return userDB
     */
    private DoctorData initDoctorDB() {
        DoctorData loadedDoctorDB = loadDoctorDB();
        return loadedDoctorDB == null ? new DoctorData() : loadedDoctorDB;
    }

    /**
     * load doctor database
     *
     * @return userDB
     */
    private DoctorData loadDoctorDB() {
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(DATA_DIR + USER_DB_NAME))) {
            return (DoctorData) input.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * initialize document database
     *
     * @return documentDB
     */
    private DocumentData initDocumentDB() {
        DocumentData loadedDocumentsDB = loadDocumentDB();
        return loadedDocumentsDB == null ? new DocumentData() : loadedDocumentsDB;
    }

    /**
     * load document database
     *
     * @return documentDB
     */
    private DocumentData loadDocumentDB() {
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(DATA_DIR + DOC_DB_NAME))) {
            return (DocumentData) input.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }


    /**
     * Stores DoctorDB object through serialization.
     */
    private boolean storeDoctorDB() {
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(DATA_DIR + USER_DB_NAME))) {
            output.writeObject(doctorData);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Stores DocumentsDatabase object through serialization.
     */
    private boolean storeDocumentsDB() {
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(DATA_DIR + DOC_DB_NAME))) {
            output.writeObject(documentData);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Create a user. Start the paxos process to sync with other servers, and update the local user
     *
     * @param doctor
     * @return
     * @throws RemoteException
     */

    @Override
    public Result createUser(Doctor doctor) throws RemoteException {
        String username = doctor.getDoctorName();
        if (doctorData.isDoctorNameAvailable(username)) {
            CommitParams commitParams = new CommitParams();
            commitParams.setDoctor(doctor);
            commitParams.setCommitEnum(CommitEnum.CREATE_USER);
            commitParams.setSectionNum(-1);
            commitParams.setDoctorDatabase(doctorData);

            commitParams.setProposalMsg("CreateUser: " + username);

            // paxos

            Result result = paxos(commitParams);

            if (result.getStatus() == 1) {
                Log.info("Server " + port + CommitEnum.CREATE_USER + ": SUCCESS");
                Log.info("Create user succeed");

                return new Result(1, "Create user succeed");
            } else return new Result(0, "Request aborted.");
        } else {
            return new Result(0, "Username already exists");
        }
    }

    /**
     * Login a user. Start the 2PC process to sync with other servers, and update the local alive doctor
     * database
     *
     * @param user
     * @return 2PC result: status 0-> fail, 1-> success, and message
     * @throws RemoteException
     */
    @Override
    public Result login(Doctor user) throws RemoteException {
        if (activeDoctorData.isLoggedIn(user.getDoctorName())) {
            return new Result(0, "Already logged in.");
        } else {
            // check username and password
            Doctor loggedInUser = doctorData.doLogin(user.getDoctorName(), user.getPassword());
            if (loggedInUser != null) {
                CommitParams commitParams = new CommitParams();
                commitParams.setDoctor(user);
                commitParams.setCommitEnum(CommitEnum.LOGIN);
                commitParams.setSectionNum(-1);
                commitParams.setActiveDoctorDatabase(activeDoctorData);


                commitParams = updateCommitParamsDatabase(commitParams);
                commitParams.setProposalMsg("login" + user.getDoctorName());

                Result result = paxos(commitParams);


                if (result.getStatus() == 0) {
                    return new Result(0, "Request aborted.");
                }
                String token = activeDoctorData.getTokenByDoctor(user.getDoctorName());
                executeCommit(commitParams);
                CentralServer.learn(commitParams, port);

                if (token != null) {
                    Log.info("Server " + port + CommitEnum.LOGIN + ": SUCCESS");
                    Log.info("New user logged in: " + user.getDoctorName());
                    return new Result(1, token);
                } else {
                    return new Result(0, "Token generation failure while logging in.");
                }
            } else {
                return new Result(0, "Unregistered or password do not match.");
            }
        }
    }

    /**
     * Logout a user. Start the paxos process to sync with other servers, and update the local alive
     * user database
     *
     * @param user
     * @return 2pc result: status 0-> fail, 1-> success, and message
     * @throws RemoteException
     */
    @Override
    public Result logout(Doctor user) throws RemoteException {
        CommitParams commitParams = new CommitParams();
        commitParams.setDoctor(user);
        commitParams.setCommitEnum(CommitEnum.LOGOUT);
        commitParams.setSectionNum(-1);
        commitParams.setActiveDoctorDatabase(activeDoctorData);

        commitParams.setProposalMsg("Logout: " + user.getDoctorName());
        Result result = paxos(commitParams);

        if (result.getStatus() == 1) {
            Log.info("Server " + port + CommitEnum.LOGOUT + ": SUCCESS");
            Log.info("User logged out: " + user.getDoctorName());
            return new Result(1, "succeed");
        } else {
            return new Result(0, "Request aborted.");
        }

    }

    /**
     * Edit a document. Need to update user status and document database. Start the 2PC process to
     * sync with other servers.
     *
     * @param user
     * @param request
     * @return 2PC result: status 0-> fail, 1-> success, and message
     * @throws RemoteException
     */
    @Override
    public Result edit(Doctor user, Request request) throws RemoteException {
        if (!activeDoctorData.isLoggedIn(user.getDoctorName())) {
            return new Result(0, "Not logged in.");
        }

        if (!user.equals(activeDoctorData.getDoctorByToken(request.getToken()))) {
            return new Result(0, "User does not match token.");
        }

        PatientFile document = documentData.getDocumentByName(request.getDocName());
        if (document == null) {
            return new Result(0, "Document does not exist.");
        }

        if (!document.hasPermit(user)) {
            return new Result(0, "You do not have access.");
        }

        Section section = document.getSectionByIndex(request.getSectionNum());
        if (section == null) {
            return new Result(0, "Section does not exist.");
        }

        Doctor editingUser = section.getOccupant();
        if (editingUser != null) {
            return new Result(0, "The section is being edited");
        }

        CommitParams commitParams = new CommitParams();
        commitParams.setDoctor(user);
        commitParams.setCommitEnum(CommitEnum.EDIT);
        commitParams.setDocName(request.getDocName());
        commitParams.setSectionNum(request.getSectionNum());
        commitParams.setDocumentDatabase(documentData);
        commitParams.setMessageManager(messageManager);
        long nextAvailableAddress = messageManager.getNextAvailableAddress();
        commitParams.setMulticastAddress(nextAvailableAddress);

        // To be updated later

        commitParams.setProposalMsg("Edit: ");

        Result result = paxos(commitParams);

        if (result.getStatus() == 0) {
            return new Result(0, "Request aborted.");
        }

        try {
            FileChannel fileChannel = FileChannel.open(Paths.get(section.getPath()), StandardOpenOption.READ);
            InputStream stream = Channels.newInputStream(fileChannel);
            SimpleRemoteInputStream remoteInputStream = new SimpleRemoteInputStream(stream);
            result.setRemoteInputStream(remoteInputStream);
            Log.info("Server " + port + CommitEnum.EDIT + ": SUCCESS");
            return new Result(1, String.valueOf(messageManager.getResultAddress(document.getPatientName())), remoteInputStream);
        } catch (Exception ioe) {
            return new Result(0, "Exception while accessing the section");
        }
    }

    /**
     * Complete editing a document. Need to commit the document update. Start the 2PC process to sync
     * with other servers.
     *
     * @param user
     * @param request
     * @return 2pc result: status 0-> fail, 1-> success, and message
     * @throws RemoteException
     */
    @Override
    public Result editEnd(Doctor user, Request request) throws RemoteException {
        try {
            if (!activeDoctorData.isLoggedIn(user.getDoctorName())) {
                return new Result(0, "Not logged in.");
            }

            if (!user.equals(activeDoctorData.getDoctorByToken(request.getToken()))) {
                return new Result(0, "User does not match token.");
            }

            PatientFile document = documentData.getDocumentByName(request.getDocName());
            if (document == null) {
                return new Result(0, "Document does not exists.");
            }

            if (!document.hasPermit(user)) {
                return new Result(0, "You do not have access.");
            }

            Section section = document.getSectionByIndex(request.getSectionNum());
            if (section == null) {
                return new Result(0, "Section does not exist.");
            }

            Doctor editingUser = section.getOccupant();
            if (!editingUser.equals(user)) {
                return new Result(0, "The section is being edited by other");
            }

            CommitParams commitParams = new CommitParams();
            commitParams.setDoctor(user);
            commitParams.setCommitEnum(CommitEnum.EDIT_END);
            commitParams.setDocName(request.getDocName());
            commitParams.setSectionNum(request.getSectionNum());
            RemoteInputStream remoteInputStream = request.getRemoteInputStream();
            commitParams.setBytes(RemoteInputStreamUtils.toBytes(remoteInputStream));

            commitParams.setDocumentDatabase(documentData);
            commitParams.setMessageManager(messageManager);

            commitParams.setProposalMsg("Edit_end: " + user.getDoctorName());

            Result result = paxos(commitParams);

            if (result.getStatus() == 0) {
                return new Result(0, "Request aborted");
            } else {
                Log.info("Server " + port + CommitEnum.EDIT_END + ": SUCCESS");
                return new Result(1, "Succeed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.info("Server " + port + " request aborted");
            return new Result(0, "Request aborted");

        }

    }

    /**
     * Create a new document. Start the 2PC process to sync with other servers.
     *
     * @param user
     * @param request
     * @return 2pc result: status 0-> fail, 1-> success, and message
     * @throws RemoteException
     */
    @Override
    public Result createDocument(Doctor user, Request request) throws RemoteException {
        if (!activeDoctorData.isLoggedIn(user.getDoctorName())) {
            return new Result(0, "Not logged in.");
        }

        if (!user.equals(activeDoctorData.getDoctorByToken(request.getToken()))) {
            return new Result(0, "User does not match token.");
        }

        PatientFile document = documentData.getDocumentByName(request.getDocName());
        if (document != null) {
            return new Result(0, "Document already exists.");
        }

        if (request.getSectionNum() <= 0) {
            return new Result(0, "Section number must be positive.");
        }

        CommitParams commitParams = new CommitParams();
        commitParams.setDoctor(user);
        commitParams.setCommitEnum(CommitEnum.CREATE_DOCUMENT);
        commitParams.setDocName(request.getDocName());
        commitParams.setSectionNum(request.getSectionNum());

        commitParams.setProposalMsg("Create_document: " + user.getDoctorName());

        Result result = paxos(commitParams);
        if (result.getStatus() == 1) {
            Log.info("Server " + port + CommitEnum.CREATE_DOCUMENT + ": SUCCESS");
            Log.info("File successfully created: " + commitParams.getDocName());
            return new Result(1, "Succeed");
        } else {
            return new Result(0, "Request aborted.");
        }
    }


    /**
     * List all docs that current user has access to
     * no 2pc
     *
     * @param user
     * @param request
     * @return result: status 0-> fail, 1-> success, and message
     * @throws RemoteException
     */
    @Override
    public Result listOwnedDocs(Doctor user, Request request) throws RemoteException {
        if (!activeDoctorData.isLoggedIn(user.getDoctorName())) {
            return new Result(0, "Not logged in.");
        }

        if (!user.getDoctorName().equals(activeDoctorData.getDoctorByToken(request.getToken()).getDoctorName())) {
            return new Result(0, "User does not match token.");
        }

        String[] docs = documentData.getAllDocumentsNames(user);

        if (docs == null || docs.length == 0) {
            Log.info("Server " + port + CommitEnum.LIST + ": SUCCESS");
            return new Result(1, "None");
        }

        String names = String.join(",", docs);
        Log.info("Server " + port + CommitEnum.LIST + ": SUCCESS");
        return new Result(1, names);
    }

    /**
     * Share doc to another user to let him/her have the access to edit the doc. Only the doc creator
     * has the access to share.
     * <p>
     * 2pc
     *
     * @param user    the user who sends the shared doc
     * @param request
     * @return result: status 0-> fail, 1-> success, and message
     * @throws RemoteException
     */
    @Override
    public Result shareDoc(Doctor user, Request request) throws RemoteException {
        if (!activeDoctorData.isLoggedIn(user.getDoctorName())) {
            return new Result(0, "Not logged in.");
        }

        if (!user.equals(activeDoctorData.getDoctorByToken(request.getToken()))) {
            return new Result(0, "User does not match token.");
        }

        PatientFile document = documentData.getDocumentByName(request.getDocName());
        if (document == null) {
            return new Result(0, "Document does not exist.");
        }

        if (!document.getCreator().equals(user)) {
            return new Result(0, "You do not have access.");
        }

        if (doctorData.getDoctorByDoctorName(request.getTargetUser().getDoctorName()) == null) {
            return new Result(0, "The target user does not exist.");
        }

        CommitParams commitParams = new CommitParams();
        commitParams.setDoctor(user);
        commitParams.setCommitEnum(CommitEnum.SHARE);
        commitParams.setDocName(request.getDocName());
        commitParams.setSectionNum(request.getSectionNum());
        commitParams.setDocumentDatabase(documentData);
        commitParams.setDoctorDatabase(doctorData);
        commitParams.setTargetUser(request.getTargetUser().getDoctorName());

        commitParams.setProposalMsg("ShareDoc: " + user.getDoctorName());

        Result result = paxos(commitParams);

        if (result.getStatus() == 1) {
            Log.info("Server " + port + CommitEnum.SHARE + ": SUCCESS");
            return new Result(1, "Succeed");
        } else {
            return new Result(0, "Request aborted");
        }
    }


    /**
     * Paxos steps: Phase 1 prepare, if it is successfully prepared, the server
     * *      will execute this request and trigger learner for other servers to update accordingly.
     *
     * @param commitParams commit parameters
     * @return 2pc result: status 0-> abort, 1-> commit, and message
     */
    private Result paxos(CommitParams commitParams) throws RemoteException {
        // update database stored in commitParams
        commitParams = updateCommitParamsDatabase(commitParams);
        boolean isPrepare = CentralServer.paxosPrepare(commitParams.getProposalMsg());
        if (!isPrepare) {
            return new Result(0, "Request Aborted.");
        } else {
            // current server execute
            executeCommit(commitParams);
            // other servers learned
            CentralServer.learn(commitParams, port);
            return new Result(1, "Request Committed.");
        }
    }

    /**
     * This function is paxos Phase one : prepare commitparams database for commit
     * Update commitparams database with doctor DB or activeDoctorDB (not documentDB)
     *
     * @param commitParams
     * @return commitParams
     */
    CommitParams updateCommitParamsDatabase(CommitParams commitParams) {
        // documentDB should not update in this step
        switch (commitParams.getCommitEnum()) {
            // create user: add new user to userDatabase
            case CREATE_USER:
                commitParams.getDoctorDatabase().addNewDoctor(commitParams.getDoctor().getDoctorName(), commitParams.getDoctor().getPassword());
                break;
            // login: create new OnlineUserRecord (generate token) and put into aliveUserDatabase
            case LOGIN:
                commitParams.getActiveDoctorDatabase().login(commitParams.getDoctor());
                break;
            // logout: set token to null
            case LOGOUT:
                String username = commitParams.getDoctor().getDoctorName();
                commitParams.getActiveDoctorDatabase().getActiveDoctorInfo(username).setToken(null);
                break;
            // edit: set occupant of the section in documentDatabase
            // cannot update db here because section path are different in different servers
            case EDIT:
                String docName = commitParams.getDocName();
                commitParams.getMessageManager().getMessageDatabase().put(docName, commitParams.getMulticastAddress());
                break;
            // edit end: write input stream into section path
            // set occupant to null in documentDatabase
            // cannot update db here because section path are different in different servers
            case EDIT_END:
                docName = commitParams.getDocName();
                commitParams.getMessageManager().getMessageDatabase().remove(docName);
                break;
            // create document: create a new document in documentDatabase
            case CREATE_DOCUMENT:

                break;
            // share doc: add user to authors of a document in documentDatabase
            case SHARE:
                Doctor sharedUser = commitParams.getDoctorDatabase().getDoctorByDoctorName(commitParams.getTargetUser());
                sharedUser.pushNewNotification(commitParams.getDocName());
                break;
            case GET_NOTIFICATIONS:
                commitParams.getDoctorDatabase().getDoctorByDoctorName(commitParams.getDoctor().getDoctorName())
                        .getNotifications().clear();
                break;
        }
        return commitParams;
    }

    /**
     * paxos phase two:  executeCommit().
     * The function is the second phase of paxos to ask a server to execute a commit
     * The requests include: CREATE_USER /LOGIN /LOGOUT EDIT/ SHARE /CREATE_DOCUMENT /EDIT_END GET_NOTIFICATIONS
     *
     * @param commitParams
     */
    @Override
    public void executeCommit(CommitParams commitParams) {
        switch (commitParams.getCommitEnum()) {
            case CREATE_USER:
                this.doctorData = commitParams.getDoctorDatabase();
                break;
            case LOGIN:
            case LOGOUT:
                this.activeDoctorData = commitParams.getActiveDoctorDatabase();
                break;
            case EDIT:
                // set occupant
                String docName = commitParams.getDocName();
                int sectionNum = commitParams.getSectionNum();
                this.documentData.getDocumentByName(docName).
                        getSectionByIndex(sectionNum).occupy(commitParams.getDoctor());
                this.messageManager = commitParams.getMessageManager();
                break;
            case SHARE:
                // add author
                this.documentData.getDocumentByName(commitParams.getDocName()).
                        addAuthor(new Doctor(commitParams.getTargetUser()));
                this.doctorData = commitParams.getDoctorDatabase();
                break;
            case CREATE_DOCUMENT:
                this.documentData.createNewPatientFile(DATA_DIR,
                        commitParams.getSectionNum(),
                        commitParams.getDocName(),
                        commitParams.getDoctor());
                break;
            case EDIT_END:
                // set occupant to null
                docName = commitParams.getDocName();
                sectionNum = commitParams.getSectionNum();
                this.documentData.getDocumentByName(docName).
                        getSectionByIndex(sectionNum).occupy(null);

                OutputStream fileStream = null;
                try {
                    Section editingSection = documentData.
                            getDocumentByName(commitParams.getDocName()).
                            getSectionByIndex(commitParams.getSectionNum());
                    fileStream = this.getWriteStream(editingSection.getPath());

                    byte[] bytes = commitParams.getBytes();
                    String str = new String(bytes, StandardCharsets.UTF_8);

                    fileStream.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.debug("Server " + port + e.getMessage());
                }
                if (fileStream != null) {
                    try {
                        fileStream.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        Log.debug("Server " + port + ex.getMessage());
                    }
                }

                PatientFile doc = documentData.getDocumentByName(commitParams.getDocName());
                if (doc.getOccupiedSections().size() == 0) {
                    messageManager = commitParams.getMessageManager();
                }
                break;
            case GET_NOTIFICATIONS:
                doctorData = commitParams.getDoctorDatabase();
                break;
        }
    }

    public OutputStream getWriteStream(String path) throws IOException {
        FileChannel fileChannel = FileChannel.open(Paths.get(path), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        return Channels.newOutputStream(fileChannel);
    }


    /**
     * Get notifications from other server
     *
     * @param user
     * @return result: status 0-> fail, 1-> success, and message
     * @throws RemoteException
     */
    @Override
    public Result getNotifications(Doctor user) throws RemoteException {
        Result ret = new Result();
        Doctor userDB = doctorData.getDoctorByDoctorName(user.getDoctorName());
        List<String> curNoti = userDB.getNotifications();
        if (curNoti.size() != 0) {
            CommitParams commitParams = new CommitParams();
            commitParams.setDoctor(user);
            commitParams.setCommitEnum(CommitEnum.GET_NOTIFICATIONS);
            commitParams.setDoctorDatabase(doctorData);
            ret.setUnreadNotifications(new ArrayList<>(curNoti));
            commitParams.setProposalMsg(curNoti.toString());
            boolean result = CentralServer.paxosPrepare(commitParams.getProposalMsg());


            if (!result) {
                ret.setUnreadNotifications(new ArrayList<>());
            }
        } else {
            ret.setUnreadNotifications(new ArrayList<>());
        }
        return ret;
    }

    /**
     * Recover data from backup data
     *
     * @param backupData
     * @return true-> success, false-> fail
     */
    @Override
    public boolean recoverData(BackUpData backupData) {
        this.documentData = backupData.getDocumentDatabase();
        this.doctorData = backupData.getDoctorDatabase();
        this.activeDoctorData = backupData.getActiveDoctorDatabase();
        this.messageManager = backupData.getMessageManager();

        // clear previous data
        try {
            FileUtils.deleteDirectory(new File(DATA_DIR));
            createDataDirectory();
        } catch (IOException e) {
            e.printStackTrace();
            Log.debug(e.getMessage());
        }

        for (PatientFile doc : documentData.getDocuments()) {
            for (Section section : doc.getSections()) {
                String previousPath = section.getPath();
                String pattern = "(.*data_)([0-9]+)(/.*)";
                String currPath = previousPath.replaceAll(pattern, "$1" + port + "$3");
                section.setPath(currPath);
            }
        }

        Map<String, byte[]> fileStreamMap = backupData.getFileStreamMap();
        for (String path : fileStreamMap.keySet()) {
            try {
//        System.out.println(path);
                File file = new File(path);
                file.getParentFile().mkdirs();
                FileUtils.writeByteArrayToFile(file, fileStreamMap.get(path));
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * Help target server recover the data from a live server
     *
     * @param targetPort port# of the target server
     * @return true-> success, false-> fail
     */
    @Override
    public boolean helpRecoverData(int targetPort) {
        DocumentData documentDatabase = this.documentData;
        DoctorData userDatabase = this.doctorData;
        ActiveDoctorData activeDoctorData = this.activeDoctorData;
        MessageManager chatManager = this.messageManager;
        Map<String, byte[]> fileStreamMap = new HashMap<>();

        // user file
        // DATA_DIR + "DocDB.dat"
        String targetDataDir = "./server_data_" + targetPort + "/";
        // put userDatabase dat file
        File file = new File(DATA_DIR + USER_DB_NAME);
        if (file.isFile()) {
            fileStreamMap.put(targetDataDir + USER_DB_NAME, getBytes(DATA_DIR + USER_DB_NAME));
        }
        file = new File(DATA_DIR + DOC_DB_NAME);
        if (file.isFile()) {
            // put DocumentDatabase dat file
            fileStreamMap.put(targetDataDir + DOC_DB_NAME, getBytes(DATA_DIR + DOC_DB_NAME));
        }

        // put section files
        for (PatientFile doc : documentDatabase.getDocuments()) {
            for (Section section : doc.getSections()) {
                String currPath = section.getPath();
//        System.out.println("origin path " + currPath);
                String pattern = "(.*data_)([0-9]+)(/.*)";
                // replace port in the path
                String targetPath = currPath.replaceAll(pattern, "$1" + targetPort + "$3");
                // change path stored in database
//        System.out.println("target path " + currPath);
                file = new File(currPath);
                if (file.isFile()) {
//          System.out.println("put path " + currPath);
                    fileStreamMap.put(targetPath, getBytes(currPath));
                }
            }
        }
        BackUpData backupData = new BackUpData(documentDatabase, userDatabase, activeDoctorData, messageManager, fileStreamMap);

        try {
            Registry registry = LocateRegistry.getRegistry(targetPort);
            ServerInterface stub = (ServerInterface) registry.lookup(Server.class.getSimpleName() + targetPort);
            stub.recoverData(backupData);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.debug("Server " + port + e.getMessage());
        }
        return false;
    }

    /**
     * Get byte array from file
     *
     * @param filePath
     * @return byte array
     */
    private byte[] getBytes(String filePath) {
        try (FileChannel fileChannel = FileChannel.open(Paths.get(filePath), StandardOpenOption.READ);
             InputStream stream = Channels.newInputStream(fileChannel)) {
            return IOUtils.toByteArray(stream);
        } catch (Exception e) {
            Log.debug("Exception: " + e.getMessage());
        }
        return null;
    }

}
