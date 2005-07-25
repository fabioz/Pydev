/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis;

import org.python.parser.SimpleNode;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.messages.IMessage;
import com.python.pydev.analysis.visitors.OcurrencesVisitor;

/**
 * This class should analyze unused imports.
 * 
 * @author Fabio
 */
public class OcurrencesAnalyzer implements Analyzer {


    public IMessage[] analyzeDocument(PythonNature nature, SourceModule module, IAnalysisPreferences prefs) {
        OcurrencesVisitor visitor = new OcurrencesVisitor(nature, module.getName(), module, prefs);
        try {
            SimpleNode ast = module.getAst();
            if(ast != null){
                ast.accept(visitor);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor.getMessages();
    }

}
