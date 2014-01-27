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

import org.python.pydev.shared_core.log.Log;

public class ParsingThread extends Thread {
    volatile boolean okToGo;
    volatile boolean force = false;

    private final ParserScheduler parser;
    private volatile Object[] argsToReparse;

    /**
     * Identifies whether this parsing thread is disposed.
     */
    private volatile boolean disposed;
    private final BaseParserManager parserManager;

    protected ParsingThread(BaseParserManager parserManager, ParserScheduler parser, Object... argsToReparse) {
        super();
        this.parser = parser;
        this.argsToReparse = argsToReparse;
        this.parserManager = parserManager;
    }

    @Override
    public void run() {
        try {
            if (force == false) {
                makeOkAndSleepUntilIdleTimeElapses();
            }

            while (!okToGo && force == false && !disposed) {
                makeOkAndSleepUntilIdleTimeElapses();
            }

            if (disposed) {
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

        } finally {
            parser.parsingThread = null;
        }
    }

    private void makeOkAndSleepUntilIdleTimeElapses() {
        try {
            okToGo = true;
            sleep(parserManager.getElapseMillisBeforeAnalysis());
        } catch (Exception e) {
        }
    }

    public void dispose() {
        this.disposed = true;
    }

    public void updateArgsToReparse(Object[] newArgsToReparse) {
        argsToReparse = newArgsToReparse;
    }

}