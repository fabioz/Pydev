/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 14, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.shared_core.parsing;

public class ParserScheduler {

    /**
     * used to do parsings in a thread - is null when not doing parsings
     */
    public volatile ParsingThread parsingThread;

    /**
     * indicates that currently nothing is happening
     */
    public static final int STATE_WAITING = 0;

    /**
     * indicates whether some parse later has been requested
     */
    public static final int STATE_PARSE_LATER = 1;

    /**
     * indicates if a thread is currently waiting for an elapse cycle to end
     */
    public static final int STATE_WAITING_FOR_ELAPSE = 2;

    /**
     * indicates if a thread is currently doing a parse action
     */
    public static final int STATE_DOING_PARSE = 3;

    /**
     * initially we're waiting
     */
    volatile int state = STATE_WAITING;

    /**
     * this is the exact time a parse later was requested
     */
    private volatile long timeParseLaterRequested = 0;

    /**
     * this is the exact time the last parse was requested
     */
    private volatile long timeLastParse = 0;

    private volatile IParser parser;

    private BaseParserManager parserManager;

    public ParserScheduler(IParser parser, BaseParserManager parserManager) {
        super();
        this.parser = parser;
        this.parserManager = parserManager;
    }

    public void parseNow() {
        parseNow(false);
    }

    /**
     * The arguments passed in argsToReparse will be passed to the reparseDocument, and then on to fireParserChanged / fireParserError
     * 
     * @return false if we asked a forced reparse and it will not be scheduled because a reparse is already in action.
     */
    public boolean parseNow(boolean force, Object... argsToReparse) {
        if (!force) {
            if (state != STATE_WAITING_FOR_ELAPSE && state != STATE_DOING_PARSE) {
                //waiting or parse later
                state = STATE_WAITING_FOR_ELAPSE; // the parser will reset it later
                timeLastParse = System.currentTimeMillis();
                checkCreateAndStartParsingThread();
            } else {
                //another request... we keep waiting until the user stops adding requests
                boolean created = checkCreateAndStartParsingThread();
                if (!created) {
                    parsingThread.okToGo = false;
                }
            }
        } else {
            ParsingThread parserThreadLocal = parsingThread; //if it dies suddenly, we don't want to get it as null...
            if (state == ParserScheduler.STATE_DOING_PARSE) {
                //a parse is already in action
                return false;
            } else {
                if (parserThreadLocal == null) {
                    parserThreadLocal = new ParsingThread(parserManager, this, argsToReparse);
                    parsingThread = parserThreadLocal;
                    parserThreadLocal.force = true;
                    parserThreadLocal.setPriority(Thread.NORM_PRIORITY - 1); //parsing is lower than normal priority
                    parserThreadLocal.start();
                } else {
                    //force it to run
                    if (argsToReparse.length > 0) {
                        parserThreadLocal.updateArgsToReparse(argsToReparse);
                    }
                    parserThreadLocal.force = true;
                    parserThreadLocal.interrupt();
                }
            }
        }
        return true;
    }

    /**
     * @return whether we really created the thread (returns false if the thread already exists)
     */
    private boolean checkCreateAndStartParsingThread() {
        ParsingThread p = parsingThread;
        if (p == null) {
            p = new ParsingThread(parserManager, this);
            p.setPriority(Thread.MIN_PRIORITY); //parsing is low priority
            p.start();
            parsingThread = p;
            return true;
        }
        return false;
    }

    public void parseLater() {
        if (state != STATE_WAITING_FOR_ELAPSE && state != STATE_PARSE_LATER) {
            state = STATE_PARSE_LATER;
            //ok, the time for this request is:
            timeParseLaterRequested = System.currentTimeMillis();
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        sleep(parserManager.getElapseMillisBeforeAnalysis());
                    } catch (Exception e) {
                        //that's ok
                    }
                    //ok, no parse happened while we were sleeping
                    if (state == STATE_PARSE_LATER && timeLastParse < timeParseLaterRequested) {
                        parseNow();
                    }
                }
            };
            thread.setName("ParserScheduler");
            thread.start();
        }

    }

    /**
     * this should call back to the parser itself for doing a parse
     * 
     * The argsToReparse will be passed to the IParserObserver2
     */
    public void reparseDocument(Object... argsToReparse) {
        IParser p = parser;
        if (p != null) {
            p.reparseDocument(argsToReparse);
        }
    }

    public void dispose() {
        ParsingThread p = this.parsingThread;
        if (p != null) {
            p.dispose();
        }
        this.parser = null;
    }

}
