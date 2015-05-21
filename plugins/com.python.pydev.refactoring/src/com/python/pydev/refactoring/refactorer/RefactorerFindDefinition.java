/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Dec 9, 2006
 * @author Fabio
 */
package com.python.pydev.refactoring.refactorer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.editor.refactoring.TooManyMatchesException;
import org.python.pydev.shared_core.string.StringUtils;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
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
     * @throws BadLocationException 
     *
     * @see org.python.pydev.editor.refactoring.IPyRefactoring#findDefinition(org.python.pydev.editor.refactoring.RefactoringRequest)
     */
    public ItemPointer[] findDefinition(RefactoringRequest request) throws BadLocationException {
        try {
            request.getMonitor().beginTask("Find definition", 100);
            List<ItemPointer> pointers = new ArrayList<ItemPointer>();
            CompletionCache completionCache = new CompletionCache();
            ArrayList<IDefinition> selected = new ArrayList<IDefinition>();

            String[] tokenAndQual;
            try {
                tokenAndQual = PyRefactoringFindDefinition.findActualDefinition(request, completionCache, selected);
            } catch (CompletionRecursionException e1) {
                Log.log(e1);
                return new ItemPointer[0];
            }
            if (tokenAndQual == null) {
                return new ItemPointer[0];
            }

            PyRefactoringFindDefinition.getAsPointers(pointers, selected.toArray(new Definition[0]));

            if (pointers.size() == 0
                    && ((Boolean) request.getAdditionalInfo(
                            RefactoringRequest.FIND_DEFINITION_IN_ADDITIONAL_INFO, true))) {
                String lookForInterface = tokenAndQual[1];
                List<IInfo> tokensEqualTo;
                try {
                    tokensEqualTo = AdditionalProjectInterpreterInfo.getTokensEqualTo(lookForInterface, request.nature,
                            AbstractAdditionalTokensInfo.TOP_LEVEL | AbstractAdditionalTokensInfo.INNER);
                    ICodeCompletionASTManager manager = request.nature.getAstManager();
                    if (manager == null) {
                        return new ItemPointer[0];
                    }
                    if (tokensEqualTo.size() > 50) {
                        //too many matches for that...
                        throw new TooManyMatchesException("Too Many matches (" + tokensEqualTo.size()
                                + ") were found for the requested token:" + lookForInterface, tokensEqualTo.size());
                    }
                    request.communicateWork(StringUtils.format(
                            "Found: %s possible matches.", tokensEqualTo.size()));
                    IPythonNature nature = request.nature;
                    for (IInfo info : tokensEqualTo) {
                        AnalysisPlugin.getDefinitionFromIInfo(pointers, manager, nature, info, completionCache);
                        request.checkCancelled();
                    }
                } catch (MisconfigurationException e) {
                    Log.log(e);
                    return new ItemPointer[0];
                }

            }
            request.communicateWork(StringUtils.format("Found: %s matches.",
                    pointers.size()));

            return pointers.toArray(new ItemPointer[0]);
        } catch (BadLocationException e) {
            throw e;
        } catch (OperationCanceledException e) {
            //that's ok... it was cancelled
            throw e;
        } finally {
            request.getMonitor().done();
        }
    }

}
