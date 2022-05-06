package document_database;

import doctor_database.Doctor;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PatientFile implements Serializable {
    private static final long serialVersionUID = -5250135537941945156L;

    private String patientName;
    private Doctor creator;
    private List<Doctor> authors;
    private List<Section> sections;

    /**
     * Constructor
     * @param patientName name of a patient
     * @param creator doctor who creates this patient file
     * @param sections list of sections that a patient file contains
     */
    // This constructor is only used in create() in this class
    // 换而言之，我们创建一个Patient File是通过 create()创建，而不是利用constructor来创建的
    public PatientFile(String patientName, Doctor creator, List<Section> sections) {
        this.patientName = patientName;
        this.creator = creator;
        this.sections = sections;
        this.authors = new ArrayList<>();
    }

    /**
     * Creates a new document with path.
     */
    public static PatientFile create(Doctor creator, String directory, int sectionsNumber, String name) throws IOException {
        String path = directory + name;
        File document = new File(path);
        if (!document.exists() || !document.isDirectory()) {
            boolean mkdir = document.mkdir();
            if (!mkdir) throw new RuntimeException("Unable to create document.");
        }
        List<Section> sections = new ArrayList<>();
        for (int i = 0; i < sectionsNumber; i++) {
            Section sec = new Section(path, String.valueOf(i));
            sections.add(sec);
            File sectionFile = new File(sec.getPath());
            sectionFile.createNewFile();
        }
        return new PatientFile(name, creator, sections);
    }

    public Section getSectionByIndex(int index) {
        if (index < 0 || index >= sections.size()) return null;
        return sections.get(index);
    }



    public void addAuthor(Doctor user) {
        synchronized (authors) {
            if (!authors.contains(user)) authors.add(user);
        }
    }

    public boolean hasPermit(Doctor user) {
        return creator.equals(user) || authors.contains(user);
    }

    /**
     * Gets all the sections currently being edited.
     */
    public List<String> getOccupiedSections() {
        List<String> editing = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            if (section.getOccupant() != null) {
                editing.add(String.valueOf(i));
            }
        }
        return editing;
    }


    public String getPatientName() {
        return this.patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Doctor getCreator() {
        return this.creator;
    }

    public void setCreator(Doctor creator) {
        this.creator = creator;
    }

    public List<Doctor> getAuthors() {
        return this.authors;
    }

    public void setAuthors(List<Doctor> authors) {
        this.authors = authors;
    }

    public List<Section> getSections() {
        return this.sections;
    }

    public void setSections(List<Section> sections) {
        this.sections = sections;
    }
}