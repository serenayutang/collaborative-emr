package back_up_database;

import message_database.MessageManager;
import doctor_database.ActiveDoctorData;
import document_database.DocumentData;
import doctor_database.DoctorData;

import java.io.Serializable;
import java.util.Map;

/***
 * This class is the third party storage getting the database from a working server, and to transfer
 * its data to a to-be recovered server.
 */
public class BackUpData implements Serializable {
    private static final long serialVersionUID = 1L;
    DocumentData documentDatabase;
    DoctorData doctorDatabase;
    ActiveDoctorData activeDoctorDatabase;
    MessageManager messageManager;
    Map<String, byte[]> fileStreamMap;

    /**
     * Constructor
     * @param documentDatabase
     * @param doctorDatabase
     * @param activeDoctorDatabase
     * @param messageManager
     * @param fileStreamMap
     */
    public BackUpData(DocumentData documentDatabase, DoctorData doctorDatabase, ActiveDoctorData activeDoctorDatabase,MessageManager messageManager, Map<String, byte[]> fileStreamMap) {
        this.doctorDatabase = doctorDatabase;
        this.documentDatabase = documentDatabase;
        this.activeDoctorDatabase = activeDoctorDatabase;
        this.messageManager = messageManager;
        this.fileStreamMap = fileStreamMap;
    }

    /**
     * getter for document database
     * @return document database
     */
    public DocumentData getDocumentDatabase() {
        return documentDatabase;
    }

    /**
     * setter for document database
     * @param documentDatabase
     */
    public void setDocumentDatabase(DocumentData documentDatabase) {
        this.documentDatabase = documentDatabase;
    }

    /**
     * getter for doctor database
     * @return doctor database
     */
    public DoctorData getDoctorDatabase() {
        return doctorDatabase;
    }

    /**
     * setter for doctor database
     * @param doctorDatabase doctor database
     */
    public void setDoctorDatabase(DoctorData doctorDatabase) {
        this.doctorDatabase = doctorDatabase;
    }

    /**
     * getter for active doctor database
     * @return active doctor database
     */
    public ActiveDoctorData getActiveDoctorDatabase() {
        return activeDoctorDatabase;
    }

    /**
     * setter for active doctor database
     * @param activeDoctorDatabase
     */
    public void setActiveDoctorDatabase(ActiveDoctorData activeDoctorDatabase) {
        this.activeDoctorDatabase = activeDoctorDatabase;
    }

    /**
     * getter for message manager
     * @return message manager
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * setter for message manager
     * @param messageManager
     */
    public void setMessageManager(MessageManager messageManager) {
        this.messageManager = messageManager;
    }

    /**
     * getter for fileStreamMap
     * @return fileStreamMap
     */
    public Map<String, byte[]> getFileStreamMap() {
        return fileStreamMap;
    }

    /**
     * setter for fileStreamMap
     * @param fileStreamMap
     */
    public void setFileStreamMap(Map<String, byte[]> fileStreamMap) {
        this.fileStreamMap = fileStreamMap;
    }
}
