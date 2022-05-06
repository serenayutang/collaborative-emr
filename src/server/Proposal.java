package server;

// This class is to construct a proposal object
public class Proposal {
    private int voteNo;
    private String content;

    // constructor
    public Proposal() {
        this.voteNo = 0;
        this.content = null;
    }

    // constructor
    public Proposal(int number, String msg) {
        this.voteNo = number;
        this.content = msg;
    }

    public String getContent() {
        return this.content;
    }

    public int getVoteNo() {
        return this.voteNo;
    }

    /**
     * function to compare if two proposals are equal or not
     * two proposals are equal only if their vote number and content are both the
     * same
     * 
     * @param p
     * @return true or false
     */
    public boolean equals(Proposal p) {
        if (p == null) {
            return false;
        }
        return this.voteNo == p.voteNo && this.content.equalsIgnoreCase(p.content);

    }

}
