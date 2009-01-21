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
    private Object[] argsToReparse;
    
    /**
     * Identifies whether this parsing thread is disposed.
     */
    private boolean disposed;

    ParsingThread(ParserScheduler parser, Object ... argsToReparse) {
        super();
        this.parser = parser;
        this.argsToReparse = argsToReparse;
    }

    public void run() {
        try {
            if(force == false){
                makeOkAndSleepUntilIdleTimeElapses();
            }
            
            while(!okToGo && force == false && !disposed){
                makeOkAndSleepUntilIdleTimeElapses();
            }

            if(disposed){
                return;
            }
            
            //ok, now we parse it... if we have not been requested to stop it
            try {
                parser.state = ParserScheduler.STATE_DOING_PARSE;
                parser.reparseDocument(argsToReparse);
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
            sleep(PyParserManager.getPyParserManager(null).getElapseMillisBeforeAnalysis()); 
        } catch (Exception e) {
        }
    }

    public void dispose() {
        this.disposed = true;
    }

}