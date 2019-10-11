/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.python.pydev.ast.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.ast.codecompletion.revisited.visitors.HeuristicFindAttrs;
import org.python.pydev.ast.item_pointer.ItemPointer;
import org.python.pydev.ast.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IInfo;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.shared_core.structure.Location;

import com.python.pydev.analysis.mypy.MypyPrefInitializer;
import com.python.pydev.analysis.pylint.PyLintPrefInitializer;

/**
 * The main plugin class to be used in the desktop.
 */
public class AnalysisPlugin extends Plugin {

    //The shared instance.
    private static AnalysisPlugin plugin;

    public static IPath stateLocation;
    //    private IWorkbenchWindow activeWorkbenchWindow;

    /**
     * The constructor.
     */
    public AnalysisPlugin() {
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        // As it starts things in the org.python.pydev node for backward-compatibility, we must
        // initialize it now.
        PyLintPrefInitializer.initializeDefaultPreferences();
        MypyPrefInitializer.initializeDefaultPreferences();
        stateLocation = AnalysisPlugin.getDefault().getStateLocation();

        // Leaving code around to know when we get to the PyDev perspective in the active window (may be
        // useful in the future).
        //        Display.getDefault().asyncExec(new Runnable() {
        //            public void run() {
        //                IWorkbench workbench = PlatformUI.getWorkbench();
        //
        //                activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
        //                handleActivePage();
        //
        //                workbench.addWindowListener(new IWindowListener() {
        //
        //                    public void windowOpened(IWorkbenchWindow window) {
        //                    }
        //
        //                    public void windowDeactivated(IWorkbenchWindow window) {
        //                    }
        //
        //                    public void windowClosed(IWorkbenchWindow window) {
        //                    }
        //
        //                    public void windowActivated(IWorkbenchWindow window) {
        //                        //When a window is activated, remove from the previous and add to the new one.
        //                        if(activeWorkbenchWindow != null){
        //                            activeWorkbenchWindow.removePerspectiveListener(perspectiveObserver);
        //                        }
        //                        activeWorkbenchWindow = window;
        //                        handleActivePage();
        //                    }
        //                });
        //            }
        //
        //            protected void handleActivePage() {
        //                if(activeWorkbenchWindow != null){
        //                    activeWorkbenchWindow.addPerspectiveListener(perspectiveObserver);
        //                    IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        //                    if(activePage != null){
        //                        IPerspectiveDescriptor perspective = activePage.getPerspective();
        //                        if(perspective != null){
        // TO KNOW ABOUT PYDEV PERSPECTIVE: perspectiveId.indexOf(PythonPerspectiveFactory.PERSPECTIVE_ID) > -1
        //                            perspectiveObserver.handleStateChange(perspective.getId());
        //                        }
        //                    }
        //                }
        //            }
        //        });
    }

    /**
     * This method is called when the plug-in is stopped
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    /**
     * @param pointers the list where the pointers will be added (if null, a new one will be created).
     * @param manager the manager to be used to get the definition.
     * @param nature the nature to be used.
     * @param info the info that we are looking for.
     * @param force whether we should force getting the ItemPointer if it's not readily available.
     * @return whether we actually tried to look for a completion or just bailed out due to force being == false.
     */
    public static boolean getDefinitionFromIInfo(List<ItemPointer> pointers, ICodeCompletionASTManager manager,
            IPythonNature nature, IInfo info, ICompletionCache completionCache, boolean requireIDefinition,
            boolean force) {
        if (pointers == null) {
            pointers = new ArrayList<>();
        }
        if (!requireIDefinition) {
            String file = info.getFile();
            if (file != null) {
                File f = new File(file);
                int line = info.getLine();
                int col = info.getCol();
                if (line > 0 && col > 0) { // 0 is invalid.
                    ItemPointer itemPointer = new ItemPointer(f, new Location(line - 1, col - 1),
                            new Location(line - 1, col - 1), null, null, f.toURI());
                    pointers.add(itemPointer);
                    return true;
                }

            }
        }
        if (!force) {
            return false;
        }
        IModule mod;
        String tok;
        mod = manager.getModule(info.getDeclaringModuleName(), nature, true);
        if (mod != null) {
            if (info.getType() == IInfo.MOD_IMPORT_TYPE) {
                Definition definition = new Definition(1, 1, "", null, null, mod);
                PyRefactoringFindDefinition.getAsPointers(pointers, new Definition[] { definition });
                return true;
            }
            //ok, now that we found the module, we have to get the actual definition
            tok = "";
            String path = info.getPath();

            if (path != null && path.length() > 0) {
                tok = path + ".";
            }
            tok += info.getName();
            try {
                IDefinition[] definitions = mod.findDefinition(
                        CompletionStateFactory.getEmptyCompletionState(tok, nature, completionCache), -1, -1, nature);

                if ((definitions == null || definitions.length == 0) && path != null && path.length() > 0) {
                    //this can happen if we have something as an attribute in the path:

                    //class Bar(object):
                    //    def __init__(self):
                    //        self.xxx = 10
                    //
                    //so, we'de get a find definition for Bar.__init__.xxx which is something we won't find
                    //for now, let's simply return a match in the correct context (although the correct way of doing
                    //it would be analyzing that context to find the match)
                    IDefinition[] contextDefinitions = mod.findDefinition(
                            CompletionStateFactory.getEmptyCompletionState(path, nature, completionCache), -1, -1,
                            nature);

                    if (contextDefinitions != null && contextDefinitions.length > 0) {
                        for (IDefinition iDefinition : contextDefinitions) {
                            if (iDefinition instanceof Definition) {
                                Definition definition = (Definition) iDefinition;
                                if (definition.ast instanceof FunctionDef) {
                                    FunctionDef functionDef = (FunctionDef) definition.ast;
                                    if (functionDef.args != null) {
                                        exprType[] args = functionDef.args.args;
                                        if (args != null && args.length > 0) {
                                            //I.e.: only analyze functions with at least one argument (for self or cls).
                                            Map<String, SourceToken> repToTokenWithArgs = new HashMap<String, SourceToken>();
                                            HeuristicFindAttrs heuristicFindAttrs = new HeuristicFindAttrs(
                                                    HeuristicFindAttrs.WHITIN_ANY, HeuristicFindAttrs.IN_ASSIGN, "",
                                                    definition.module.getName(), null, repToTokenWithArgs, nature);
                                            heuristicFindAttrs.visitFunctionDef(functionDef);

                                            List<IToken> tokens = heuristicFindAttrs.getTokens();
                                            List<IDefinition> newDefs = new ArrayList<>();
                                            for (IToken iToken : tokens) {
                                                if (info.getName().equals(iToken.getRepresentation())) {
                                                    newDefs.add(new Definition(iToken, definition.scope,
                                                            definition.module));
                                                }
                                            }
                                            definitions = newDefs.toArray(new IDefinition[newDefs.size()]);
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
                PyRefactoringFindDefinition.getAsPointers(pointers, definitions);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    /**
     * Returns the shared instance.
     */
    public static AnalysisPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the directory that can be used to store things for some project
     */
    public static File getStorageDirForProject(IProject p) {
        if (AnalysisPlugin.getDefault() == null) {
            return new File(".");
        }
        IPath location = p.getWorkingLocation("com.python.pydev.analysis");
        IPath path = location;

        File file = new File(path.toOSString());
        return file;
    }

    public static String getPluginID() {
        return getDefault().getBundle().getSymbolicName();
    }

}
