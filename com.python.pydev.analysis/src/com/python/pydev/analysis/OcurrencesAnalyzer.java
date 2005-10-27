/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.IDocument;
import org.python.parser.SimpleNode;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.messages.IMessage;
import com.python.pydev.analysis.visitors.OcurrencesVisitor;

/**
 * This class should analyze unused imports.
 * 
 * @author Fabio
 */
public class OcurrencesAnalyzer implements Analyzer {


    public IMessage[] analyzeDocument(PythonNature nature, SourceModule module, IAnalysisPreferences prefs, IDocument document) {
        OcurrencesVisitor visitor = new OcurrencesVisitor(nature, module.getName(), module, prefs, document);
        try {
            SimpleNode ast = module.getAst();
            if(ast != null){
                ast.accept(visitor);
            }
        } catch (Exception e) {
            PydevPlugin.log(IStatus.ERROR, "Error while visiting "+module.getName()+" ("+module.getFile()+")",e);
        }
        return visitor.getMessages();
    }

}
