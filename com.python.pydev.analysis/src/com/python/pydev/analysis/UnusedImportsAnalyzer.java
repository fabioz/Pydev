/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManager;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * This class should analyze unused imports.
 * 
 * @author Fabio
 */
public class UnusedImportsAnalyzer implements Analyzer {

    public IMessage[] analyzeDocument(PythonNature nature, SourceModule module) {
        List messages = new ArrayList();
        
        ICodeCompletionASTManager astManager = nature.getAstManager();
        
        //get the imports
        IToken[] tokenImportedModules = module.getTokenImportedModules();
        IToken[] wildImportedModules = module.getWildImportedModules();
        
        CompletionState state = new CompletionState(0,0,"",nature);
        state.builtinsGotten = true; //we don't want to get builtins here...
        
        
        return null;
    }

}
