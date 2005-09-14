/*
 * License: Common Public License v1.0
 * Created on Sep 14, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.parser;

import org.python.pydev.core.log.Log;

public class ParsingThread extends Thread {
    boolean okToGo;
    boolean force = false;

    private ParserScheduler parser;

    ParsingThread(ParserScheduler parser) {
        super();
        this.parser = parser;
    }

    public void run() {
        try {
            makeOkAndSleepUntilIdleTimeElapses();
            while(!okToGo && force == false){
                makeOkAndSleepUntilIdleTimeElapses();
            }

            //ok, now we parse it... if we have not been requested to stop it
            try {
                parser.state = ParserScheduler.STATE_DOING_PARSE;
                parser.reparseDocument();
            } catch (Throwable e) {
                Log.log(e);
            }
            //remove the force state
            force = false;
            //reset the state
            parser.state = ParserScheduler.STATE_WAITING;
            
        } finally{
            parser.parsingThread = null;
        }
    }

    private void makeOkAndSleepUntilIdleTimeElapses() {
        try {
            okToGo = true;
            sleep(getIdleTimeRequested()); //one sec
        } catch (Exception e) {
        }
    }

    /**
     * @return the idle time to make a parse... this should probably be on the interface
     */
    private int getIdleTimeRequested() {
        return parser.getIdleTimeRequested();
    }
}