package document_database;

import doctor_database.Doctor;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is to store all patient files
 *
 */
public class DocumentData implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<PatientFile> patientFiles;

    /**
     * Initializes the internal document's ArrayList.
     */
    public DocumentData() {
        patientFiles = new ArrayList<>();
    }

    /**
     * Creates a new {@code Document} adding it directly to the DocumentsDatabase.
     *
     * @param path           new document file path
     * @param name           new document's name which is usually a patient's name
     * @param creator        new document's owner which is a doctor object
     * @throws IOException if an I/O error occurs
     */
    public void createNewPatientFile(String path, int sectionsNumber, String name, Doctor creator) {
        try {
            PatientFile patientFile;
            if (alreadyExists(name)) throw new IOException("Document already exists");
            else patientFile = PatientFile.create(creator, path, sectionsNumber, name);
            synchronized (patientFiles) {
                patientFiles.add(patientFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the input Document's name already exists or not.
     *
     * @return true if the document already exists, false otherwise
     */
    public boolean alreadyExists(String name) {
        return getDocumentByName(name) != null;
    }

    /**
     * Gets the Document by its name.
     *
     * @param documentName document's name
     * @return the document reference or null if it does not exists yet
     */
    public PatientFile getDocumentByName(String documentName) {
        synchronized (patientFiles) {
            for (PatientFile d : patientFiles)
                if (d.getPatientName().compareTo(documentName) == 0) return d;
            return null;
        }
    }

    /**
     * Collects all the documents with a given doctor object who has access to edit.
     *
     * @param doctor doctor object
     * @return accessible file names
     */
    public String[] getAllDocumentsNames(Doctor doctor) {
        List<String> nameList = new ArrayList<>();
        synchronized (patientFiles) {
            for (PatientFile d : patientFiles)
                if (d.hasPermit(doctor))
                    nameList.add(d.getPatientName());
        }
        return nameList.toArray(new String[0]);
    }

    public List<PatientFile> getDocuments() {
        return this.patientFiles;
    }

    public void setDocuments(List<PatientFile> patientFiles) {
        this.patientFiles = patientFiles;
    }
}
