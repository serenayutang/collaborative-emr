package doctor_database;

import java.io.Serializable;

/**
 * This class represents the info of an active doctor with his/her specific token
 */
public class ActiveDoctorInfo implements Serializable {
    private Doctor doctor;
    private String token;

    /***
     * Constructor
     * @param doc the active doc
     */
    ActiveDoctorInfo(Doctor doc) {
        try {
            this.doctor = doc;
            this.token = doc.generateToken();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * getter
     * @return active doctor
     */
    public Doctor getDoctor() {
        return doctor;
    }

    /***
     * getter
     * @return the token of the active doctor
     */
    public String getToken() {
        return token;
    }

    /***
     * setter
     * @param token set the active user's token
     */
    public void setToken(String token) {
        this.token = token;
    }

    /***
     * check if two tokens are equal
     * @param token
     * @return true if they are equal and false otherwise
     */
    public boolean tokenIsEqualTo(String token) {
        if (token == null) {
            return this.token == null;
        }

        return (this.token.compareTo(token) == 0);
    }
}
