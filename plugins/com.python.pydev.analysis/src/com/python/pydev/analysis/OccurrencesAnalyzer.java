/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.pep8.Pep8Visitor;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.SimpleNode;

import com.python.pydev.analysis.messages.IMessage;
import com.python.pydev.analysis.tabnanny.TabNanny;
import com.python.pydev.analysis.visitors.OccurrencesVisitor;

/**
 * This class is responsible for starting the analysis of a given module.
 * 
 * @author Fabio
 */
public class OccurrencesAnalyzer {

    public IMessage[] analyzeDocument(IPythonNature nature, final SourceModule module,
            final IAnalysisPreferences prefs,
            final IDocument document, final IProgressMonitor monitor, IIndentPrefs indentPrefs) {

        //Do pep8 in a thread.
        final List<IMessage> pep8Messages = new ArrayList<>();
        Thread t = new Thread() {
            @Override
            public void run() {
                pep8Messages.addAll(new Pep8Visitor().getMessages(module, document, monitor, prefs));
            }
        };
        t.start();
        OccurrencesVisitor visitor = new OccurrencesVisitor(nature, module.getName(), module, prefs, document, monitor);
        try {
            SimpleNode ast = module.getAst();
            if (ast != null) {
                if (nature.startRequests()) {
                    try {
                        ast.accept(visitor);
                    } finally {
                        nature.endRequests();
                    }
                }
            }
        } catch (OperationCanceledException e) {
            throw e;
        } catch (Exception e) {
            Log.log(IStatus.ERROR, ("Error while visiting " + module.getName() + " (" + module.getFile() + ")"), e);
        }

        List<IMessage> messages = new ArrayList<IMessage>();
        if (!monitor.isCanceled()) {
            messages = visitor.getMessages();
            try {
                messages.addAll(TabNanny.analyzeDoc(document, prefs, module.getName(), indentPrefs, monitor));
            } catch (Exception e) {
                Log.log(e); //just to be safe... (could happen if the document changes during the process).
            }
        }

        if (!monitor.isCanceled()) {
            try {
                t.join();
                messages.addAll(pep8Messages);
            } catch (InterruptedException e) {
                //If interrupted keep on going as it is.
            }
        }

        return messages.toArray(new IMessage[messages.size()]);
    }

}
