/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.editor.autoedit.IIndentPrefs;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.analysis.messages.IMessage;
import com.python.pydev.analysis.tabnanny.TabNanny;
import com.python.pydev.analysis.visitors.OccurrencesVisitor;

/**
 * This class should analyze unused imports.
 * 
 * @author Fabio
 */
public class OccurrencesAnalyzer implements Analyzer {


    public IMessage[] analyzeDocument(IPythonNature nature, SourceModule module, IAnalysisPreferences prefs, IDocument document) {
        return analyzeDocument(nature, module, prefs, document, new NullProgressMonitor());
    }
    
    public IMessage[] analyzeDocument(IPythonNature nature, SourceModule module, IAnalysisPreferences prefs, IDocument document, IProgressMonitor monitor) {
        return analyzeDocument(nature, module, prefs, document, monitor, DefaultIndentPrefs.get());
    }
    
    public IMessage[] analyzeDocument(IPythonNature nature, SourceModule module, IAnalysisPreferences prefs, IDocument document, 
            IProgressMonitor monitor, IIndentPrefs indentPrefs) {
        
        OccurrencesVisitor visitor = new OccurrencesVisitor(nature, module.getName(), module, prefs, document, monitor);
        try {
            SimpleNode ast = module.getAst();
            if(ast != null){
                ast.accept(visitor);
            }
        } catch (OperationCanceledException e) {
            throw e;
        } catch (Exception e) {
            PydevPlugin.log(IStatus.ERROR, "Error while visiting "+module.getName()+" ("+module.getFile()+")",e);
        }
        
        List<IMessage> messages = visitor.getMessages();
        try{
            messages.addAll(TabNanny.analyzeDoc(document, prefs, module.getName(), indentPrefs));
        }catch(Exception e){
            PydevPlugin.log(e); //just to be safe... (shouldn't happen).
        }
        return messages.toArray(new IMessage[messages.size()]);
    }

}
