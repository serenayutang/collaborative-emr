package server;
import back_up_database.BackUpData;
import doctor_database.Doctor;
import model.CommitParams;
import model.Request;
import model.Result;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface ServerInterface extends Remote{


    void executeCommit(CommitParams commitParams) throws RemoteException;

    Result createUser(Doctor doctor) throws RemoteException;

    Result login(Doctor doctor) throws RemoteException;

    Result logout(Doctor doctor) throws RemoteException;


    Result edit(Doctor doctor, Request request) throws RemoteException;

    Result editEnd(Doctor doctor, Request request) throws RemoteException;

    Result createDocument(Doctor doctor, Request request) throws RemoteException;

    Result listOwnedDocs(Doctor doctor, Request request) throws RemoteException;

    Result shareDoc(Doctor doctor, Request request) throws RemoteException;

    Result getNotifications(Doctor doctor) throws RemoteException;

    boolean recoverData(BackUpData backupData) throws RemoteException;

    boolean helpRecoverData(int targetPort) throws RemoteException;

}
