package doctor_database;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class keep track of the active users.
 * As there can be multiple active users, we use ConcurrentHashMap to store username to its userInfo
 * ReadWriteLock is included to maintain a pair of associated locks, one for read-only operations and one for writing.
 * The read lock may be held simultaneously by multiple reader threads, so long as there are no writers.
 *
 */
public class ActiveDoctorData implements Serializable {
    ConcurrentHashMap<String, ActiveDoctorInfo> activeDoctors;
    private ReentrantReadWriteLock rwlock;

    public ActiveDoctorData() {
        activeDoctors = new ConcurrentHashMap<>();
        rwlock = new ReentrantReadWriteLock();
    }

    /***
     * Update the activeUserData when a client has logged in and return the token of such login.
     * @param doctor a doctor who is logging in
     * @return the token of this login of a given doctor
     */
    public String login(Doctor doctor) {
        if (doctor == null) return null;
        ActiveDoctorInfo info = new ActiveDoctorInfo(doctor);
        rwlock.writeLock().lock();
        activeDoctors.put(doctor.getDoctorName(), info);
        rwlock.writeLock().unlock();
        return info.getToken();
    }

    /***
     * Get the Doctor object with token
     * @param token the known token of an active doctor
     * @return Doctor object
     */
    public Doctor getDoctorByToken(String token) {
        for (ActiveDoctorInfo info : activeDoctors.values()) {
            if (info.tokenIsEqualTo(token)) {
                return info.getDoctor();
            }
        }
        return null;
    }

    /***
     * Gets the token with a given active doctor
     * @param doctorName the doctorName of the active doctor
     * @return token
     */
    public String getTokenByDoctor(String doctorName) {
        return activeDoctors.get(doctorName).getToken();
    }

    /**
     * To know whether a doctor is logged in or not
     * @param doctorName the doctorName of a doctor
     * @return true if the given doctor is logged in and false otherwise
     */
    public boolean isLoggedIn(String doctorName) {
        return activeDoctors.containsKey(doctorName) && activeDoctors.get(doctorName).getToken() != null; // get activeUserInfo.getToken();
    }

    /**
     * get the information of an active user
     * @param username the username of an active user
     * @return the info of this active user including the user object and token
     */
    //原名是getOnlineUserRecord
    public ActiveDoctorInfo getActiveDoctorInfo(String username) {
        return activeDoctors.get(username);
    }
}
