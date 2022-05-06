package doctor_database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class implements data structure and methods for doctors. Uses ReadWriteLock when
 * updating database.
 */
public class DoctorData implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Doctor> doctors;
    private ReentrantReadWriteLock rwlock;

    /**
     * Initializes doctor database
     */
    public DoctorData() {
        doctors = new ArrayList<>();
        rwlock = new ReentrantReadWriteLock();
    }

    /**
     * Logs the doctor into system through its doctorName and password credentials, retrieving its object
     * reference.
     *
     * @param doctorName doctor's name
     * @param password doctor's password
     * @return the doctor object if exists and the credentials are valid, null otherwise
     */
    public Doctor doLogin(String doctorName, String password) {
        try {
            Doctor doctor = getDoctorByDoctorName(doctorName);
            if (doctor != null) {
                return doctor.checkPassword(password) ? doctor : null;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Registers a new doctor storing its data into the doctor database if the input credentials do not
     * exist yet.
     *
     * @param doctorName doctor's name
     * @param password doctor's password
     * @return new doctor reference or null if that doctorName is not available
     */
    public Doctor addNewDoctor(String doctorName, String password) {
        if (!isDoctorNameAvailable(doctorName)) return null;
        Doctor newDoctor = new Doctor(doctorName, password);
        rwlock.writeLock().lock();
        doctors.add(newDoctor);
        rwlock.writeLock().unlock();
        return newDoctor;
    }

    /**
     * Checks if the input doctorName is available or not.
     *
     * @param doctorName
     * @return true if does not exist any doctor with that doctor'sname, false otherwise
     */
    public boolean isDoctorNameAvailable(String doctorName) {
        return getDoctorByDoctorName(doctorName) == null;
    }

    /**
     * Gets the doctor object from its doctorName.
     *
     * @param doctorName
     * @return doctor's object if exists, null otherwise
     */
    public Doctor getDoctorByDoctorName(String doctorName) {
        rwlock.readLock().lock();
        for (Doctor doctor : doctors) {
            if (doctor.getDoctorName().compareTo(doctorName) == 0) {
                rwlock.readLock().unlock();
                return doctor;
            }
        }
        rwlock.readLock().unlock();
        return null;
    }

}
