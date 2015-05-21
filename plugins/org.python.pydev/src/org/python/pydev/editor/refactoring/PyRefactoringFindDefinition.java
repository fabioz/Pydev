/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.refactoring;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.Location;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;

/**
 * @note This class was refactored and moved from the Pydev Extensions version to be able to provide a context-sensitive
 * find definition that can be properly used in the open source version of Pydev.
 */
public class PyRefactoringFindDefinition {

    /**
     * @param request OUT: the request to be used in the find definition. It'll be prepared inside this method, and if it's not
     * a suitable request for the find definition, the return of this function will be null, otherwise, it was correctly
     * prepared for a find definition action.
     * 
     * @param completionCache the completion cache to be used
     * @param selected OUT: fills the array with the found definitions
     * 
     * @return an array with 2 strings: the activation token and the qualifier used. The return may be null, in which case
     *      the refactoring request is not valid for a find definition.
     * @throws CompletionRecursionException 
     * @throws BadLocationException 
     */
    public static String[] findActualDefinition(RefactoringRequest request, CompletionCache completionCache,
            ArrayList<IDefinition> selected) throws CompletionRecursionException, BadLocationException {
        //ok, let's find the definition.
        request.getMonitor().beginTask("Find actual definition", 5);
        String[] tokenAndQual;
        try {
            //1. we have to know what we're looking for (activationToken)
            request.communicateWork("Finding Definition");
            IModule mod = prepareRequestForFindDefinition(request);
            if (mod == null) {
                return null;
            }
            String modName = request.moduleName;
            request.communicateWork("Module name found:" + modName);
            tokenAndQual = PySelection.getActivationTokenAndQual(request.getDoc(),
                    request.ps.getAbsoluteCursorOffset(), true);
            String tok = tokenAndQual[0] + tokenAndQual[1];
            //2. check findDefinition (SourceModule)
            try {
                int beginLine = request.getBeginLine();
                int beginCol = request.getBeginCol() + 1;
                IPythonNature pythonNature = request.nature;

                PyRefactoringFindDefinition.findActualDefinition(request.getMonitor(), mod, tok, selected, beginLine,
                        beginCol, pythonNature, completionCache);
            } catch (OperationCanceledException e) {
                throw e;
            } catch (CompletionRecursionException e) {
                throw e;
            } catch (BadLocationException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            request.getMonitor().done();
        }
        return tokenAndQual;
    }

    /**
     * Prepares a request to a find definition operation.
     *  
     * @param request IN/OUT the request that's being used for a find definition operation. Will change it so that
     * a find definition can be done.
     * 
     * @return the module to be used or null if the given request is not suitable for a find definition operation.
     */
    private static IModule prepareRequestForFindDefinition(RefactoringRequest request) {
        String modName = null;

        //all that to try to give the user a 'default' interpreter manager, for whatever he is trying to search
        //if it is in some pythonpath, that's easy, but if it is some file somewhere else in the computer, this
        //might turn out a little tricky.
        if (request.nature == null) {
            //the request is not associated to any project. It is probably a system file. So, let's check it...
            Tuple<IPythonNature, String> infoForFile = PydevPlugin.getInfoForFile(request.file);
            if (infoForFile != null) {
                modName = infoForFile.o2;
                request.nature = infoForFile.o1;
                request.inputName = modName;
            } else {
                return null;
            }
        }

        if (modName == null) {
            modName = request.resolveModule();
        }

        if (request.nature == null) {
            Log.logInfo(
                    "Unable to resolve nature for find definition request (python or jython interpreter may not be configured).");
            return null;
        }

        IModule mod = request.getModule();
        if (mod == null) {
            Log.logInfo("Unable to resolve module for find definition request.");
            return null;
        }

        if (modName == null) {
            if (mod.getName() == null) {
                if (mod instanceof SourceModule) {
                    SourceModule m = (SourceModule) mod;
                    modName = "__module_not_in_the_pythonpath__";
                    m.setName(modName);
                }
            }
            if (modName == null) {
                Log.logInfo("Unable to resolve module for find definition request (modName == null).");
                return null;
            }
        }
        request.moduleName = modName;
        return mod;
    }

    /**
     * This method will try to find the actual definition given all the needed parameters (but it will not try to find
     * matches in the whole workspace if it's not able to find an exact match in the context)
     * 
     * Note that the request must be already properly configured to be used in this function. Otherwise, the
     * function that should be used is {@link #findActualDefinition(RefactoringRequest, CompletionCache, ArrayList)}
     * 
     * 
     * @param request: used only to communicateWork and checkCancelled
     * @param mod this is the module where we should find the definition
     * @param tok the token we're looking for (complete with dots)
     * @param selected OUT: this is where the definitions should be added
     * @param beginLine starts at 1
     * @param beginCol starts at 1
     * @param pythonNature the nature that we should use to find the definition
     * @param completionCache cache to store completions
     * 
     * @throws Exception
     */
    public static void findActualDefinition(IProgressMonitor monitor, IModule mod, String tok,
            ArrayList<IDefinition> selected, int beginLine, int beginCol, IPythonNature pythonNature,
            ICompletionCache completionCache) throws Exception, CompletionRecursionException {

        ICompletionState completionState = CompletionStateFactory.getEmptyCompletionState(tok, pythonNature,
                beginLine - 1, beginCol - 1, completionCache);
        IDefinition[] definitions = mod.findDefinition(completionState, beginLine, beginCol, pythonNature);

        if (monitor != null) {
            monitor.setTaskName("Found:" + definitions.length + " definitions");
            monitor.worked(1);
            if (monitor.isCanceled()) {
                return;
            }
        }

        int len = definitions.length;
        for (int i = 0; i < len; i++) {
            IDefinition definition = definitions[i];
            boolean doAdd = true;
            if (definition instanceof Definition) {
                Definition d = (Definition) definition;
                doAdd = !findActualTokenFromImportFromDefinition(pythonNature, tok, selected, d, completionCache);
            }
            if (monitor != null && monitor.isCanceled()) {
                return;
            }
            if (doAdd) {
                selected.add(definition);
            }
        }
    }

    /** 
     * Given some definition, find its actual token (if that's possible)
     * @param request the original request
     * @param tok the token we're looking for
     * @param lFindInfo place to store info
     * @param selected place to add the new definition (if found)
     * @param d the definition found before (this function will only work if this definition
     * maps to an ImportFrom)
     *  
     * @return true if we found a new definition (and false otherwise)
     * @throws Exception
     */
    private static boolean findActualTokenFromImportFromDefinition(IPythonNature nature, String tok,
            ArrayList<IDefinition> selected, Definition d, ICompletionCache completionCache) throws Exception {
        boolean didFindNewDef = false;

        Set<Tuple3<String, Integer, Integer>> whereWePassed = new HashSet<Tuple3<String, Integer, Integer>>();

        tok = FullRepIterable.getLastPart(tok); //in an import..from, the last part will always be the token imported 

        while (d.ast instanceof ImportFrom) {
            Tuple3<String, Integer, Integer> t1 = getTupFromDefinition(d);
            if (t1 == null) {
                break;
            }
            whereWePassed.add(t1);

            Definition[] found = (Definition[]) d.module
                    .findDefinition(CompletionStateFactory.getEmptyCompletionState(tok, nature, completionCache),
                            d.line, d.col, nature);
            if (found != null && found.length == 1) {
                Tuple3<String, Integer, Integer> tupFromDefinition = getTupFromDefinition(found[0]);
                if (tupFromDefinition == null) {
                    break;
                }
                if (!whereWePassed.contains(tupFromDefinition)) { //avoid recursions
                    didFindNewDef = true;
                    d = found[0];
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        if (didFindNewDef) {
            selected.add(d);
        }

        return didFindNewDef;
    }

    /**
     * @return a tuple with the absolute path to the definition, its line and col.
     */
    private static Tuple3<String, Integer, Integer> getTupFromDefinition(Definition d) {
        if (d == null) {
            return null;
        }
        File file = d.module.getFile();
        if (file == null) {
            return null;
        }
        return new Tuple3<String, Integer, Integer>(FileUtils.getFileAbsolutePath(file), d.line, d.col);
    }

    /**
     * @param pointers: OUT: list where the pointers will be added
     * @param definitions the definitions that will be gotten as pointers
     */
    public static void getAsPointers(List<ItemPointer> pointers, IDefinition[] definitions) {
        for (IDefinition definition : definitions) {
            ItemPointer itemPointer = createItemPointer(definition);
            pointers.add(itemPointer);
        }
    }

    public static ItemPointer createItemPointer(IDefinition definition) {
        File file = definition.getModule().getFile();
        int line = definition.getLine();
        int col = definition.getCol();

        ItemPointer itemPointer = new ItemPointer(file, new Location(line - 1, col - 1),
                new Location(line - 1, col - 1), (Definition) definition, definition.getModule().getZipFilePath());
        return itemPointer;
    }
}
