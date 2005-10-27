/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.messages.IMessage;

public interface Analyzer {
    public IMessage[] analyzeDocument(PythonNature nature, SourceModule module, IAnalysisPreferences prefs, IDocument document);
}
