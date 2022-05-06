package doctor_database;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/***
 * This class represents a doctor who has or is accessing our program
 */

public class Doctor implements Serializable {
    private static final long serialVersionUID = -7351729135012380019L;

    private String doctorName;
    private String password;
    private List<String> notifications = new ArrayList<>();

    /**
     * Constructor
     * @param doctorName name of the doctor
     * @param password password
     */
    public Doctor(String doctorName, String password) {
        try {
            this.doctorName = doctorName;
            this.password = getEncrypted(password);
            notifications = new ArrayList<>();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Doctor(String doctorName) {
        this.doctorName = doctorName;
        notifications = new ArrayList<>();
    }

    /**
     * Verify password
     */
    public boolean checkPassword(String pwd) throws Exception {
        return this.password.equals(getEncrypted(pwd));
    }

    /**
     * Generate token for an active doctor.
     */
    public String generateToken() throws Exception {
        String tokenString = password + System.currentTimeMillis();
        return getEncrypted(tokenString);
    }

//
//    public String generateToken(long timestamp) throws Exception {
//        String tokenString = password + timestamp;
//        return getEncrypted(tokenString);
//    }


    /**
     * Encrypts the given password
     */
    public String getEncrypted(String str) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] bytes = md5.digest(str.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Retrieves all unread notifications to the given doctor .
     *
     * @return strings array of notifications
     */
    public List<String> getUnreadNotifications() {
        if (null != notifications) {
            synchronized (notifications) {
                List<String> unreadNotifications = new ArrayList<>(notifications);
                this.notifications.clear();
                return unreadNotifications;
            }
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Add a new notification value to the unread ones.
     *
     * @param doc new document which doctor has access to
     */
    public void pushNewNotification(String doc) {
        if (notifications == null) {
            notifications = new ArrayList<>();
        }
        synchronized (notifications) {
            notifications.add(doc);
        }
    }

    /**
     * Check if two Doctor object is equal to each other
     * @param  o an object
     * @return true if two objects are equal and false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Doctor doctor = (Doctor) o;
        return Objects.equals(doctorName, doctor.doctorName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(doctorName, password, notifications);
    }

    /**
     * Gets the doctor's name from the doctor object
     * @return doctor's name
     */
    public String getDoctorName() {
        return this.doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getNotifications() {
        return this.notifications;
    }

    public void setNotifications(List<String> notifications) {
        this.notifications = notifications;
    }
}
