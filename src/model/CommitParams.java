package model;

import doctor_database.ActiveDoctorData;
import doctor_database.Doctor;
import doctor_database.DoctorData;
import document_database.DocumentData;
import com.healthmarketscience.rmiio.RemoteInputStream;
import message_database.MessageManager;

import java.io.Serializable;

public class CommitParams implements Serializable {

    private Doctor doctor;
    private CommitEnum commitEnum;
    private RemoteInputStream inputStream;
    private byte[] bytes;

    private String docName;
    private int sectionNum;
    private String targetUser;
    private long multicastAddress;

    // in memory database
    private DocumentData documentDatabase;
    private ActiveDoctorData activeDoctorDatabase;
    private MessageManager messageManager;
    private DoctorData doctorDatabase;

    private String proposalMsg;

    public CommitParams() {
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public Doctor getDoctor() {
        return this.doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public CommitEnum getCommitEnum() {
        return this.commitEnum;
    }

    public void setCommitEnum(CommitEnum commitEnum) {
        this.commitEnum = commitEnum;
    }

    public RemoteInputStream getInputStream() {
        return this.inputStream;
    }

    public void setInputStream(RemoteInputStream inputStream) {
        this.inputStream = inputStream;
    }

    public String getDocName() {
        return this.docName;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public int getSectionNum() {
        return this.sectionNum;
    }

    public void setSectionNum(int sectionNum) {
        this.sectionNum = sectionNum;
    }

    public DocumentData getDocumentDatabase() {
        return this.documentDatabase;
    }

    public void setDocumentDatabase(DocumentData documentDatabase) {
        this.documentDatabase = documentDatabase;
    }

    public ActiveDoctorData getActiveDoctorDatabase() {
        return this.activeDoctorDatabase;
    }

    public void setActiveDoctorDatabase(ActiveDoctorData activeDoctorDatabase) {
        this.activeDoctorDatabase = activeDoctorDatabase;
    }

    public MessageManager getMessageManager() {
        return this.messageManager;
    }

    public void setMessageManager(MessageManager messageManager) {
        this.messageManager = messageManager;
    }

    public DoctorData getDoctorDatabase() {
        return this.doctorDatabase;
    }

    public void setDoctorDatabase(DoctorData doctorDatabase) {
        this.doctorDatabase = doctorDatabase;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(String targetUser) {
        this.targetUser = targetUser;
    }

    public long getMulticastAddress() {
        return multicastAddress;
    }

    public void setMulticastAddress(long multicastAddress) {
        this.multicastAddress = multicastAddress;
    }

    public void setProposalMsg(String msg){this.proposalMsg = msg;};

    public String getProposalMsg(){return this.proposalMsg;};

}
