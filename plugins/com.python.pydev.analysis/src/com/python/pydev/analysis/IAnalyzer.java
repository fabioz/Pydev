/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;

import com.python.pydev.analysis.messages.IMessage;

public interface IAnalyzer {
    public IMessage[] analyzeDocument(IPythonNature nature, SourceModule module, IAnalysisPreferences prefs, IDocument document);
}
