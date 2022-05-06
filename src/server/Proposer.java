package server;

import java.util.*;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

// This class is to construct a Proposer object
public class Proposer {

    public static Logger Log = LogManager.getLogger(Proposer.class);

    /**
     * Phase 1: PREPARE-PROMISE
     * This function is to check if the proposer receives a PROMISE with true
     * response from a majority of acceptors, it will return true.
     * 
     * @param p
     * @param acceptors
     * @return
     */
    public static boolean prepare(Proposal p, List<Acceptor> acceptors) {

        Log.info("Reqeust Received: " + p.getContent());
        int majorityNo = acceptors.size() / 2 + 1;

        // loop through each acceptor to check if it is prepared or not
        // if it is prepared, add to the count
        int countOfPrepared = 0;
        for (Acceptor a : acceptors) {
            Promise promise = a.onPrepare(p);
            if (promise != null && promise.isAck()) {
                countOfPrepared++;
            }
        }

        Log.info("Prepared count: " + countOfPrepared);

        // if the number of prepared acceptor is less than the majority, then return
        // false
        if (countOfPrepared < majorityNo) {
            Log.info("Proposal: " + p.getContent() + " NOT PREPARED");
            return false;
        }

        // Phase two: check the accept phase
        int acceptCount = 0;
        for (Acceptor a : acceptors) {
            if (a.onAccept(p)) {
                acceptCount++;
            }
        }
        Log.info("Acceptor count: " + acceptCount);

        if (acceptCount < majorityNo) {
            Log.info("Proposal: " + p.getContent() + " NOT ACCEPTED");
            return false;
        }

        Log.info("Proposal: " + p.getContent() + " SUCCESS PREPARED and ACCEPTED");
        return true;

    }

}
