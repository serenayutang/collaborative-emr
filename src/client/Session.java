package client;

import doctor_database.Doctor;

/**
 * Session class, track current user and document.
 */
public class Session {
  private final String sessionToken;
  private Doctor doctor;
  private String occupiedFilePath;
  private String occupiedFileName;
  private int sectionIndex;

  /**
   * construct a session with given token and doctor.
   * @param sessionToken the given token.
   * @param doctor the doctor.
   */
  Session(String sessionToken, Doctor doctor) {
    this.sessionToken = sessionToken;
    this.doctor = doctor;
    occupiedFilePath = null;
  }

  boolean isEditing() {
    return occupiedFilePath != null;
  }

  String getSessionToken() {
    return this.sessionToken;
  }

  public Doctor getDoctor() {
    return this.doctor;
  }

  public void setDoctor(Doctor doctor) {
    this.doctor = doctor;
  }

  String getOccupiedFilePath() {
    return occupiedFilePath;
  }

  void setOccupiedFilePath(String occupiedFilePath) {
    this.occupiedFilePath = occupiedFilePath;
  }

  String getOccupiedFileName() {
    return occupiedFileName;
  }

  void setOccupiedFileName(String occupiedFileName) {
    this.occupiedFileName = occupiedFileName;
  }

  int getSectionIndex() {
    return sectionIndex;
  }

  void setSectionIndex(int sectionIndex) {
    this.sectionIndex = sectionIndex;
  }
}
