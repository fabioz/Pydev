/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.ImportHandle;
import org.python.pydev.core.docutils.ImportHandle.ImportHandleInfo;
import org.python.pydev.core.docutils.PyImportsHandling;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.builder.AnalysisRunner;
import com.python.pydev.analysis.ctrl_1.IAnalysisMarkersParticipant;

/**
 * This participant will add a suggestion to create class/methods/attributes when an undefined variable error is found.
 */
public class TddQuickFixParticipant implements IAnalysisMarkersParticipant {

    /*default*/Image imageClass;
    /*default*/Image imageMethod;
    /*default*/Image imageModule;

    public TddQuickFixParticipant() {
        ImageCache imageCache = PydevPlugin.getImageCache();
        if (imageCache != null) { //making tests
            imageClass = imageCache.get(UIConstants.CREATE_CLASS_ICON);
            imageMethod = imageCache.get(UIConstants.CREATE_METHOD_ICON);
            imageModule = imageCache.get(UIConstants.CREATE_MODULE_ICON);
        }
    }

    public void addProps(MarkerAnnotationAndPosition markerAnnotation, IAnalysisPreferences analysisPreferences,
            String line, PySelection ps, int offset, IPythonNature nature, PyEdit edit, List<ICompletionProposal> props)
                    throws BadLocationException, CoreException {
        if (nature == null) {
            return;
        }

        ICodeCompletionASTManager astManager = nature.getAstManager();
        if (astManager == null) {
            return;
        }

        if (markerAnnotation.position == null) {
            return;
        }
        IMarker marker = markerAnnotation.markerAnnotation.getMarker();
        Integer id = (Integer) marker.getAttribute(AnalysisRunner.PYDEV_ANALYSIS_TYPE);
        int start = markerAnnotation.position.offset;
        int end = start + markerAnnotation.position.length;
        ps.setSelection(start, end);
        String markerContents;
        try {
            markerContents = ps.getSelectedText();
        } catch (Exception e1) {
            return; //Selection may be wrong.
        }

        IDocument doc = ps.getDoc();
        List<String> parametersAfterCall = ps.getParametersAfterCall(end);

        switch (id) {

            case IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE:

                addCreateClassOption(ps, edit, props, markerContents, parametersAfterCall);

                addCreateMethodOption(ps, edit, props, markerContents, parametersAfterCall);
                break;

            case IAnalysisPreferences.TYPE_UNDEFINED_IMPORT_VARIABLE:
                //Say we had something as:
                //import sys
                //sys.Bar
                //in which case 'Bar' is undefined
                //in this situation, the activationTokenAndQual would be "sys." and "Bar" 
                //and we want to get the definition for "sys"
                String[] activationTokenAndQual = ps.getActivationTokenAndQual(true);

                if (activationTokenAndQual[0].endsWith(".")) {
                    ArrayList<IDefinition> selected = findDefinitions(nature, edit, start - 2, doc);

                    for (IDefinition iDefinition : selected) {

                        IModule module = iDefinition.getModule();
                        if (module.getFile() != null) {
                            Definition definition = (Definition) iDefinition;
                            File file = module.getFile();
                            if (definition.ast == null) {
                                //if we have no ast in the definition, it means the module itself was found (global scope)

                                //Add option to create class at the given module!
                                addCreateClassOption(ps, edit, props, markerContents, parametersAfterCall, file);

                                addCreateMethodOption(ps, edit, props, markerContents, parametersAfterCall, file);
                            } else if (definition.ast instanceof ClassDef) {
                                ClassDef classDef = (ClassDef) definition.ast;
                                //Ok, we should create a field or method in this case (accessing a classmethod or staticmethod)
                                PyCreateMethodOrField pyCreateMethod = new PyCreateMethodOrField();
                                String className = NodeUtils.getNameFromNameTok(classDef.name);
                                pyCreateMethod.setCreateInClass(className);
                                pyCreateMethod.setCreateAs(PyCreateMethodOrField.CLASSMETHOD);
                                addCreateClassmethodOption(ps, edit, props, markerContents, parametersAfterCall,
                                        pyCreateMethod, file, className);
                            }
                        }
                    }
                }
                break;

            case IAnalysisPreferences.TYPE_UNRESOLVED_IMPORT:
                //This case is the following: from other_module4 import Foo
                //with 'Foo' being undefined.
                //So, we have to suggest creating a Foo class/method in other_module4
                PyImportsHandling importsHandling = new PyImportsHandling(ps.getDoc(), false);
                int offsetLine = ps.getLineOfOffset(start);
                String selectedText = ps.getSelectedText();

                Tuple<IModule, String> found = null;
                String foundFromImportStr = null;
                boolean isImportFrom = false;
                OUT: for (ImportHandle handle : importsHandling) {
                    if (handle.startFoundLine == offsetLine || handle.endFoundLine == offsetLine
                            || (handle.startFoundLine < offsetLine && handle.endFoundLine > offsetLine)) {
                        List<ImportHandleInfo> importInfo = handle.getImportInfo();
                        for (ImportHandleInfo importHandleInfo : importInfo) {
                            String fromImportStr = importHandleInfo.getFromImportStr();
                            List<String> importedStr = importHandleInfo.getImportedStr();
                            for (String imported : importedStr) {
                                if (selectedText.equals(imported)) {
                                    if (fromImportStr != null) {
                                        foundFromImportStr = fromImportStr +
                                                "." + imported;
                                        isImportFrom = true;
                                    } else {
                                        //if fromImportStr == null, it's not a from xxx import yyy (i.e.: simple import)
                                        foundFromImportStr = imported;
                                    }
                                    try {
                                        String currentModule = nature.resolveModule(edit.getEditorFile());
                                        ICompletionState state = CompletionStateFactory.getEmptyCompletionState(nature,
                                                new CompletionCache());

                                        found = nature.getAstManager().findModule(
                                                foundFromImportStr,
                                                currentModule,
                                                state,
                                                new SourceModule(currentModule, edit.getEditorFile(), edit.getAST(),
                                                        null));
                                    } catch (Exception e) {
                                        Log.log(e);
                                    }
                                    break OUT;
                                }
                            }
                        }
                        break OUT;
                    }
                }

                boolean addOptionToCreateClassOrMethod = isImportFrom;

                if (found != null && found.o1 != null) {
                    //Ok, we found a module, now, it may be that we still have to create some intermediary modules
                    //or just create a class or method at the end.
                    if (found.o1 instanceof SourceModule) {

                        //if all was found, there's nothing left to create.
                        if (found.o2 != null && found.o2.length() > 0) {
                            SourceModule sourceModule = (SourceModule) found.o1;
                            File file = sourceModule.getFile();

                            if (found.o2.indexOf('.') != -1) {
                                //We have to create some intermediary structure.
                                if (!addOptionToCreateClassOrMethod) {

                                    //Cannot create class or method from the info (only the module structure).
                                    if (sourceModule.getName().endsWith(".__init__")) {
                                        File f = getFileStructure(file.getParentFile(), found.o2);
                                        addCreateModuleOption(ps, edit, props, markerContents, f);
                                    }

                                } else {
                                    //Ok, the leaf may be a class or method.
                                    if (sourceModule.getName().endsWith(".__init__")) {
                                        String moduleName = FullRepIterable.getWithoutLastPart(sourceModule.getName());
                                        String withoutLastPart = FullRepIterable.getWithoutLastPart(found.o2);
                                        moduleName += "." + withoutLastPart;

                                        String classOrMethodName = FullRepIterable.getLastPart(found.o2);

                                        File f = getFileStructure(file.getParentFile(), withoutLastPart);
                                        addCreateClassInNewModuleOption(ps, edit, props, classOrMethodName, moduleName,
                                                parametersAfterCall, f);
                                        addCreateMethodInNewModuleOption(ps, edit, props, classOrMethodName,
                                                moduleName, parametersAfterCall, f);
                                    }

                                }

                            } else {
                                //Ok, it's all there, we just have to create the leaf.
                                if (!addOptionToCreateClassOrMethod || sourceModule.getName().endsWith(".__init__")) {
                                    //Cannot create class or method from the info (only the module structure).
                                    if (sourceModule.getName().endsWith(".__init__")) {
                                        File f = new File(file.getParent(), found.o2
                                                + FileTypesPreferencesPage.getDefaultDottedPythonExtension());
                                        addCreateModuleOption(ps, edit, props, markerContents, f);
                                    }
                                } else {
                                    //Ok, the leaf may be a class or method.
                                    addCreateClassOption(ps, edit, props, markerContents, parametersAfterCall, file);
                                    addCreateMethodOption(ps, edit, props, markerContents, parametersAfterCall, file);
                                }
                            }
                        }
                    }

                } else if (foundFromImportStr != null) {
                    //We couldn't find anything there, so, we have to create the modules structure as needed and
                    //maybe create a class or module at the end (but only if it's an import from).
                    //Ok, it's all there, we just have to create the leaf.

                    //Discover the source folder where we should create the structure.

                    File editorFile = edit.getEditorFile();
                    String onlyProjectPythonPathStr = nature.getPythonPathNature().getOnlyProjectPythonPathStr(false);
                    List<String> split = StringUtils.splitAndRemoveEmptyTrimmed(onlyProjectPythonPathStr, '|');
                    for (int i = 0; i < split.size(); i++) {
                        String fullPath = FileUtils.getFileAbsolutePath(split.get(i));
                        fullPath = PythonPathHelper.getDefaultPathStr(fullPath);
                        split.set(i, fullPath);
                    }
                    HashSet<String> projectSourcePath = new HashSet<String>(split);
                    if (projectSourcePath.size() == 0) {
                        return; //No source folder for editor... this shouldn't happen (code analysis wouldn't even run on it).
                    }
                    String fullPath = FileUtils.getFileAbsolutePath(editorFile);
                    fullPath = PythonPathHelper.getDefaultPathStr(fullPath);
                    String foundSourceFolderFullPath = null;
                    if (projectSourcePath.size() == 1) {
                        foundSourceFolderFullPath = projectSourcePath.iterator().next();
                    } else {
                        for (String string : projectSourcePath) {
                            if (fullPath.startsWith(string)) {
                                //Use this as the source folder
                                foundSourceFolderFullPath = string;
                                break;
                            }
                        }
                    }
                    if (foundSourceFolderFullPath != null) {

                        if (!addOptionToCreateClassOrMethod) {
                            //Cannot create class or method from the info (only the module structure).

                            File f = getFileStructure(new File(foundSourceFolderFullPath), foundFromImportStr);
                            addCreateModuleOption(ps, edit, props, foundFromImportStr, f);

                        } else {
                            //Ok, the leaf may be a class or method.
                            String moduleName = FullRepIterable.getWithoutLastPart(foundFromImportStr);
                            File file = getFileStructure(new File(foundSourceFolderFullPath), moduleName);
                            String lastPart = FullRepIterable.getLastPart(foundFromImportStr);
                            addCreateClassInNewModuleOption(ps, edit, props, lastPart, moduleName, parametersAfterCall,
                                    file);
                            addCreateMethodInNewModuleOption(ps, edit, props, lastPart, moduleName,
                                    parametersAfterCall, file);
                        }
                    }
                }
                break;

        }

    }

    protected File getFileStructure(File file, String withoutLastPart) {
        File f = file;
        List<String> split = StringUtils.dotSplit(withoutLastPart);
        int size = split.size();
        for (int i = 0; i < size; i++) {
            if (i == size - 1) {
                f = new File(f, split.get(i) + FileTypesPreferencesPage.getDefaultDottedPythonExtension());

            } else {
                f = new File(f, split.get(i));
            }
        }
        return f;
    }

    private void addCreateClassmethodOption(PySelection ps, PyEdit edit, List<ICompletionProposal> props,
            String markerContents, List<String> parametersAfterCall, PyCreateMethodOrField pyCreateMethod, File file,
            String className) {
        props.add(new TddRefactorCompletionInModule(markerContents, imageMethod, "Create " + markerContents +
                " classmethod at " + className +
                " in " + file.getName(), null, "Create " + markerContents +
                        " classmethod at class: " + className +
                        " in " + file,
                IPyCompletionProposal.PRIORITY_CREATE, edit,
                file, parametersAfterCall, pyCreateMethod, ps));
    }

    private void addCreateMethodOption(PySelection ps, PyEdit edit, List<ICompletionProposal> props,
            String markerContents, List<String> parametersAfterCall, File file) {
        props.add(new TddRefactorCompletionInModule(markerContents, imageMethod, "Create " + markerContents +
                " method at " + file.getName(), null, "Create " + markerContents +
                        " method at " + file,
                IPyCompletionProposal.PRIORITY_CREATE, edit, file, parametersAfterCall, new PyCreateMethodOrField(),
                ps));
    }

    private void addCreateClassOption(PySelection ps, PyEdit edit, List<ICompletionProposal> props,
            String markerContents, List<String> parametersAfterCall, File file) {
        props.add(new TddRefactorCompletionInModule(markerContents, imageClass, "Create " + markerContents +
                " class at " + file.getName(), null, "Create " + markerContents +
                        " class at " + file,
                IPyCompletionProposal.PRIORITY_CREATE, edit, file, parametersAfterCall, new PyCreateClass(), ps));
    }

    private void addCreateClassInNewModuleOption(PySelection ps, PyEdit edit, List<ICompletionProposal> props,
            String markerContents, String moduleName, List<String> parametersAfterCall, File file) {
        props.add(new TddRefactorCompletionInInexistentModule(markerContents, imageClass, "Create " + markerContents +
                " class at new module " + moduleName, null, "Create " + markerContents +
                        " class at new module "
                        + file,
                IPyCompletionProposal.PRIORITY_CREATE, edit, file, new ArrayList<String>(),
                new PyCreateClass(), ps));
    }

    private void addCreateMethodInNewModuleOption(PySelection ps, PyEdit edit, List<ICompletionProposal> props,
            String markerContents, String moduleName, List<String> parametersAfterCall, File file) {
        props.add(new TddRefactorCompletionInInexistentModule(markerContents, imageMethod, "Create " + markerContents +
                " method at new module " + moduleName, null, "Create " + markerContents +
                        " method at new module "
                        + file,
                IPyCompletionProposal.PRIORITY_CREATE, edit, file, new ArrayList<String>(),
                new PyCreateMethodOrField(), ps));
    }

    private void addCreateModuleOption(PySelection ps, PyEdit edit, List<ICompletionProposal> props,
            String markerContents, File file) {
        props.add(new TddRefactorCompletionInInexistentModule(markerContents, imageModule, "Create " + markerContents +
                " module", null, "Create " + markerContents +
                        " module (" + file +
                        ")",
                IPyCompletionProposal.PRIORITY_CREATE, edit, file, new ArrayList<String>(), new NullPyCreateAction(),
                ps));
    }

    private void addCreateMethodOption(PySelection ps, PyEdit edit, List<ICompletionProposal> props,
            String markerContents, List<String> parametersAfterCall) {
        props.add(new TddRefactorCompletion(markerContents, imageMethod, "Create " + markerContents +
                " method", null,
                null, IPyCompletionProposal.PRIORITY_CREATE, edit, PyCreateClass.LOCATION_STRATEGY_BEFORE_CURRENT,
                parametersAfterCall, new PyCreateMethodOrField(), ps));
    }

    private void addCreateClassOption(PySelection ps, PyEdit edit, List<ICompletionProposal> props,
            String markerContents, List<String> parametersAfterCall) {
        props.add(new TddRefactorCompletion(markerContents, imageClass, "Create " + markerContents +
                " class", null,
                null, IPyCompletionProposal.PRIORITY_CREATE, edit, PyCreateClass.LOCATION_STRATEGY_BEFORE_CURRENT,
                parametersAfterCall, new PyCreateClass(), ps));
    }

    private ArrayList<IDefinition> findDefinitions(IPythonNature nature, PyEdit edit, int start, IDocument doc) {
        CompletionCache completionCache = new CompletionCache();
        ArrayList<IDefinition> selected = new ArrayList<IDefinition>();

        RefactoringRequest request = new RefactoringRequest(edit.getEditorFile(), new PySelection(doc,
                new TextSelection(doc, start, 0)), new NullProgressMonitor(), nature, edit);

        try {
            PyRefactoringFindDefinition.findActualDefinition(request, completionCache, selected);
        } catch (CompletionRecursionException | BadLocationException e1) {
            Log.log(e1);
        }
        return selected;
    }

}
