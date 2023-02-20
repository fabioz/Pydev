/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.pythonpathconf;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.python.pydev.ast.interpreter_managers.InterpreterInfo;
import org.python.pydev.core.IInterpreterManager;

/**
 * Creates the interpreter info in a separate operation.
 */
public class ObtainInterpreterInfoOperation implements IRunnableWithProgress {

    static class OperationMonitor extends ProgressMonitorWrapper {

        private PrintWriter logger;

        protected OperationMonitor(IProgressMonitor monitor, PrintWriter logger) {
            super(monitor);
            this.logger = logger;
        }

        @Override
        public void beginTask(String name, int totalWork) {
            super.beginTask(name, totalWork);
            logger.print("- Beggining task:");
            logger.print(name);
            logger.print(" totalWork:");
            logger.println(totalWork);
        }

        @Override
        public void setTaskName(String name) {
            super.setTaskName(name);
            logger.print("- Setting task name:");
            logger.println(name);
        }

        @Override
        public void subTask(String name) {
            super.subTask(name);
            logger.print("- Sub Task:");
            logger.println(name);
        }
    }

    public InterpreterInfo result;
    public String file;
    public Exception e;
    private PrintWriter logger;
    private IInterpreterManager interpreterManager;
    private boolean autoSelect;
    private boolean isConda;

    /**
     * @param file2
     * @param logger 
     * @param isConda 
     */
    public ObtainInterpreterInfoOperation(String file2, PrintWriter logger, IInterpreterManager interpreterManager,
            boolean autoSelect, boolean isConda) {
        this.file = file2;
        this.logger = logger;
        this.interpreterManager = interpreterManager;
        this.autoSelect = autoSelect;
        this.isConda = isConda;
    }

    /**
     * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        monitor = new OperationMonitor(monitor, logger);
        monitor.beginTask("Getting libs", 100);
        try {
            InterpreterInfo interpreterInfo = (InterpreterInfo) interpreterManager.createInterpreterInfo(file, monitor,
                    !autoSelect);
            if (interpreterInfo != null) {
                interpreterInfo.setActivateCondaEnv(isConda);
                result = interpreterInfo;
            }
        } catch (Exception e) {
            logger.println("Exception detected: " + e.getMessage());
            this.e = e;
        }
        monitor.done();
    }

}