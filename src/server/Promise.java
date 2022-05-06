package server;

//// This class is to construct a promise object
public class Promise {
    protected final boolean ack;
    protected final Proposal proposal;

    public Promise(boolean ack, Proposal proposal) {
        this.ack = ack;
        this.proposal = proposal;
    }

    public boolean isAck() {
        return ack;
    }

    public Proposal getProposal() {
        return proposal;
    }
}
