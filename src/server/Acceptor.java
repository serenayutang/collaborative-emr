package server;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Acceptor {
    // the last proposal
    public Proposal last = new Proposal();
    public int port;

    public static Logger Log = LogManager.getLogger(Acceptor.class);

    // constructor
    public Acceptor(int port) {
        this.port = port;
    }

    /**
     * Phase 1: PREPARE-PROMISE
     * This function is that when acceptor receive a prepare msg from proposer , to
     * decide if should respond with a promise of success or fails message.
     * Is this ID bigger than any round I have previously received?
     * If yes
     * store the ID number, max_id = ID
     * respond with a PROMISE message with true
     * If no
     * respond with a PROMISE message with false
     *
     * @param p proposal recieved
     * @return a promise
     */
    public Promise onPrepare(Proposal p) {

//        // kill off the thread at random time
//        if (Math.random() < 0.2) {
//            Log.debug("Server " + port + " NO response: PREPARE REJECTED");
//            return null;
//        }

        if (p == null) {
            Log.debug("null proposal!");
            throw new IllegalArgumentException("null proposal!");

        }

        // If this proposal number bigger than last round I have previously received
        // return a promise with true
        // assign the last proposal as this proposal
        if (p.getVoteNo() > last.getVoteNo()) {
            Promise response = new Promise(true, last);
            last = p;
            Log.info("Server " + port + " is successfully PREPARED");
            return response;

        } else {
            Log.info("Server " + port + " is REJECTED to PREPARE");
            return new Promise(false, null);

        }

    }

    /**
     * Phase 2: PROPOSE-ACCEPT
     * This function is to return true if the argument is the same proposal as the
     * last one
     * Otherwise return false
     *
     * @param p
     * @return
     */
    public boolean onAccept(Proposal p) {
        // kill off the thread at random time
        String failmsg = "Server " + port + " NO response: ACCEPT REJECTED";
        String sucmsg = "Server " + port + " ACCEPT";
//        if (Math.random() < 0.2) {
//            Log.debug(failmsg);
//            return false;
//        }

        if (last.equals(p)) {
            Log.info(sucmsg);
            return true;

        }

        Log.debug(failmsg);
        return false;

    }
}
