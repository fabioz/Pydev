/*
 * Created on Dec 9, 2006
 * @author Fabio
 */
package com.python.pydev.refactoring.refactorer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.OperationCanceledException;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.editor.refactoring.TooManyMatchesException;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

/**
 * Class used to find the definition for some refactoring request.
 * 
 * @author Fabio
 */
public class RefactorerFindDefinition {


    /**
     * This function is used to find the definition for some token.
     * It may return a list of ItemPointer because the actual definition may not be
     * easy to find (so, multiple places that could be the definitions for
     * the given token may be returned... and it may be up to the user to actually
     * choose the best match).
     * 
     * @see org.python.pydev.editor.refactoring.IPyRefactoring#findDefinition(org.python.pydev.editor.refactoring.RefactoringRequest)
     */
    public ItemPointer[] findDefinition(RefactoringRequest request) {
        try{
            List<ItemPointer> pointers = new ArrayList<ItemPointer>();
            CompletionCache completionCache = new CompletionCache();
            ArrayList<IDefinition> selected = new ArrayList<IDefinition>();
            
            String[] tokenAndQual = PyRefactoringFindDefinition.findActualDefinition(request, completionCache, selected);
            if(tokenAndQual == null){
                return new ItemPointer[0];
            }
            
            PyRefactoringFindDefinition.getAsPointers(pointers, selected.toArray(new Definition[0]));
            
            if(pointers.size() == 0 && ((Boolean)request.getAdditionalInfo(AstEntryRefactorerRequestConstants.FIND_DEFINITION_IN_ADDITIONAL_INFO, true))){
                String lookForInterface = tokenAndQual[1];
                List<IInfo> tokensEqualTo = AdditionalProjectInterpreterInfo.getTokensEqualTo(lookForInterface, request.nature,
                        AbstractAdditionalInterpreterInfo.TOP_LEVEL | AbstractAdditionalInterpreterInfo.INNER);
                
                ICodeCompletionASTManager manager = request.nature.getAstManager();
                if (tokensEqualTo.size() > 100){
                    //too many matches for that...
                    throw new TooManyMatchesException("Too Many matches ("+tokensEqualTo.size()+") were found for the requested token:"+lookForInterface, tokensEqualTo.size());
                }
                request.communicateWork(StringUtils.format("Found: %s possible matches.", tokensEqualTo.size()));
                IPythonNature nature = request.nature;
                for (IInfo info : tokensEqualTo) {
                    AnalysisPlugin.getDefinitionFromIInfo(pointers, manager, nature, info, completionCache);
                    request.checkCancelled();
                }
            }
            request.communicateWork(StringUtils.format("Found: %s matches.", pointers.size()));
            
            return pointers.toArray(new ItemPointer[0]);
        }catch(OperationCanceledException e){
            //that's ok... it was cancelled
            throw e;
        }finally{
            request.getMonitor().done();
            request.popMonitor();
        }
    }




}
