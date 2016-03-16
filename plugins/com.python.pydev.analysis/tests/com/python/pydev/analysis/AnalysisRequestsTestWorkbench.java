/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.editorinput.PyOpenEditor;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.PyParser.ParserInfo;
import org.python.pydev.parser.fastparser.FastDefinitionsParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;

import com.python.pydev.analysis.actions.AnalyzeOnRequestSetter;
import com.python.pydev.analysis.actions.AnalyzeOnRequestSetter.AnalyzeOnRequestAction;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.builder.AnalysisBuilderRunnable;
import com.python.pydev.analysis.builder.AnalysisRunner;

/**
 * This test is used to see if the code-analysis is correctly requested on a refresh of some file.
 * 
 * @author Fabio
 */
public class AnalysisRequestsTestWorkbench extends AbstractWorkbenchTestCase {

    private Object lock = new Object();
    private List<Tuple3<ISimpleNode, Throwable, ParserInfo>> parsesDone = new ArrayList<Tuple3<ISimpleNode, Throwable, ParserInfo>>();
    private List<Tuple<String, SimpleNode>> fastParsesDone = new ArrayList<Tuple<String, SimpleNode>>();

    //gives both, a syntax and analysis error!
    private String invalidMod1Contents = "import java.lang.Class\njava.lang.Class\nkkk invalid kkk\nprint kkk";
    private String validMod1Contents = "import java.lang.Class\njava.lang.Class";
    private String validMod1ContentsWithToken = "class Foo:\n    pass\n";
    private ICallback<Object, Tuple3<ISimpleNode, Throwable, ParserInfo>> addParsesToListListener;
    private IFile mod2;
    private PyEdit editor2;

    public static final int TIME_FOR_ANALYSIS = 2000;

    @Override
    protected void setUp() throws Exception {
        addParsesToListListener = getAddParsesToListListener();
        PyParser.successfulParseListeners.add(addParsesToListListener);
        resourcesAnalyzed = new ArrayList<IResource>();

        //analyze all files
        PyDevBuilderPrefPage.setAnalyzeOnlyActiveEditor(false);
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        if (mod2 != null) {
            mod2.delete(true, null);
        }
        if (editor2 != null) {
            editor2.close(false);
        }
        super.tearDown();
        PyParser.successfulParseListeners.remove(addParsesToListListener);
        //analyze only files open in the editor
        //restore default on tearDown
        PyDevBuilderPrefPage.setAnalyzeOnlyActiveEditor(PyDevBuilderPrefPage.DEFAULT_ANALYZE_ONLY_ACTIVE_EDITOR);
    }

    private void print(String... msg) {
        if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
            for (String string : msg) {
                System.out.println(string);
            }
        }
    }

    public void testRefreshAnalyzesFiles() throws Exception {
        editor.close(false);
        goToIdleLoopUntilCondition(getInitialParsesCondition(), getParsesDone(), false); //just to have any parse events consumed
        goToManual(TIME_FOR_ANALYSIS); //give it a bit more time...

        PythonNature nature = PythonNature.getPythonNature(mod1);
        AbstractAdditionalTokensInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature);
        //all modules are empty
        assertEquals(new HashSet<String>(), info.getAllModulesWithTokens());

        ICallback<Object, IResource> analysisCallback = getAnalysisCallback();
        AnalysisBuilderRunnable.analysisBuilderListeners.add(analysisCallback);

        try {
            checkSetInvalidContents();

            checkSetValidContents(info);

            checkSetValidContentsWithFooToken(info);

            checkRename(info);

            checkSetValidContents(info);

            //can analyze when editor is opened
            resourcesAnalyzed.clear();
            print("-------- Opening editor ----------");
            editor = (PyEdit) PyOpenEditor.doOpenEditor(mod1);
            goToIdleLoopUntilCondition(get1ResourceAnalyzed(), getResourcesAnalyzed());
            assertEquals(1, resourcesAnalyzed.size());
            //wait for it to complete (if it's too close it may consider it being the same analysis request even with a different time)   
            goToManual(TIME_FOR_ANALYSIS);

            //analyze when forced
            resourcesAnalyzed.clear();
            AnalyzeOnRequestAction analyzeOnRequestAction = new AnalyzeOnRequestSetter.AnalyzeOnRequestAction(editor);
            analyzeOnRequestAction.run();
            goToManual(TIME_FOR_ANALYSIS); //in 1 seconds, 1 analysis should happen

            assertEquals(1, resourcesAnalyzed.size());

        } finally {
            AnalysisBuilderRunnable.analysisBuilderListeners.remove(analysisCallback);
        }

        CheckRefreshAnalyzesFilesOnlyOnActiveEditor();
    }

    private void checkSetValidContentsWithFooToken(AbstractAdditionalTokensInfo info) throws CoreException {
        print("-------- Setting valid contents with some token -------------");
        resourcesAnalyzed.clear();
        synchronized (lock) {
            parsesDone.clear();
        }
        setFileContents(validMod1ContentsWithToken);
        goToIdleLoopUntilCondition(get1ResourceAnalyzed(), getResourcesAnalyzed());
        goToIdleLoopUntilCondition(getNoErrorMarkersCondition(), getMarkers());
        goToManual(TIME_FOR_ANALYSIS); //in 1 seconds, only 1 parse/analysis should happen
        assertEquals(1, parsesDone.size());
        assertEquals(new HashSet<String>(Arrays.asList(new String[] { "pack1.pack2.mod1" })),
                info.getAllModulesWithTokens());
    }

    private void checkRename(final AbstractAdditionalTokensInfo info) throws CoreException {
        print("-------- Renaming and checking if tokens are OK -------------");
        resourcesAnalyzed.clear();
        synchronized (lock) {
            parsesDone.clear();
        }
        IPath initialPath = mod1.getFullPath();
        IPath newPath = initialPath.removeLastSegments(1).append("new_mod.py");

        mod1.move(newPath, true, new NullProgressMonitor());

        //        goToIdleLoopUntilCondition(get1ResourceAnalyzed(), getResourcesAnalyzed());
        goToIdleLoopUntilCondition(

                new ICallback<Boolean, Object>() {
                    @Override
                    public Boolean call(Object arg) {
                        return new HashSet<String>(Arrays.asList(new String[] { "pack1.pack2.new_mod" })).equals(info
                                .getAllModulesWithTokens());
                    }
                },

                new ICallback<String, Object>() {
                    @Override
                    public String call(Object arg) {
                        return "Was expecting only: 'pack1.pack2.new_mod'. Found: " + info.getAllModulesWithTokens();
                    }
                });

        goToManual(TIME_FOR_ANALYSIS); //in 1 seconds, only 1 parse/analysis should happen
        assertEquals(1, parsesDone.size());

        //now, go back to what it was...
        IFile new_mod = initFile.getParent().getFile(new Path("new_mod.py"));
        new_mod.move(initialPath, true, new NullProgressMonitor());
        //        goToIdleLoopUntilCondition(get1ResourceAnalyzed(), getResourcesAnalyzed());
        goToIdleLoopUntilCondition(

                new ICallback<Boolean, Object>() {
                    @Override
                    public Boolean call(Object arg) {
                        return new HashSet<String>(Arrays.asList(new String[] { "pack1.pack2.mod1" })).equals(info
                                .getAllModulesWithTokens());
                    }
                },

                new ICallback<String, Object>() {
                    @Override
                    public String call(Object arg) {
                        return "Was expecting only: 'pack1.pack2.mod1'. Found: " + info.getAllModulesWithTokens();
                    }
                });
    }

    private void checkSetValidContents(AbstractAdditionalTokensInfo info) throws CoreException {
        print("-------- Setting valid contents -------------");
        resourcesAnalyzed.clear();
        synchronized (lock) {
            parsesDone.clear();
        }
        setFileContents(validMod1Contents);
        goToIdleLoopUntilCondition(get1ResourceAnalyzed(), getResourcesAnalyzed());
        goToIdleLoopUntilCondition(getNoErrorMarkersCondition(), getMarkers());
        goToManual(TIME_FOR_ANALYSIS); //in 1 seconds, only 1 parse/analysis should happen
        assertEquals(1, parsesDone.size());
        assertEquals(new HashSet<String>(), info.getAllModulesWithTokens());
    }

    private void checkSetInvalidContents() throws CoreException {
        print("------------- Setting INvalid contents -------------");
        resourcesAnalyzed.clear();
        synchronized (lock) {
            parsesDone.clear();
        }
        setFileContents(invalidMod1Contents);
        goToManual(TIME_FOR_ANALYSIS); //in 1 seconds, only 1 parse/analysis should happen
        goToIdleLoopUntilCondition(get1ResourceAnalyzed(), getResourcesAnalyzed());
        goToIdleLoopUntilCondition(getHasBothErrorMarkersCondition(), getMarkers());
        assertEquals(1, parsesDone.size());
    }

    private ICallback<String, Object> getResourcesAnalyzed() {
        return new ICallback<String, Object>() {

            @Override
            public String call(Object arg) {
                return resourcesAnalyzed.toString();
            }

        };
    }

    private ICallback<Boolean, Object> get1ResourceAnalyzed() {
        return new ICallback<Boolean, Object>() {

            @Override
            public Boolean call(Object arg) {
                return resourcesAnalyzed.size() == 1;
            }

        };
    }

    /**
     * Checks what happens now if the user wants to hear only notifications on opened editors.
     * 
     * @throws Exception
     */
    public void CheckRefreshAnalyzesFilesOnlyOnActiveEditor() throws Exception {
        print("----------- CheckRefreshAnalyzesFilesOnlyOnActiveEditor ---------");
        //analyze all files
        PyDevBuilderPrefPage.setAnalyzeOnlyActiveEditor(true);

        print("----------- CLOSING EDITOR ---------");
        editor.close(false);
        goToManual(TIME_FOR_ANALYSIS); //wait a bit for the current things to clear

        ICallback<Object, Tuple<String, SimpleNode>> parseFastDefinitionsCallback = getParseFastDefinitionsCallback();
        FastDefinitionsParser.parseCallbacks.add(parseFastDefinitionsCallback);

        ICallback<Object, IResource> analysisErrorCallback = getAnalysisErrorCallback();
        AnalysisBuilderRunnable.analysisBuilderListeners.add(analysisErrorCallback);

        try {
            //no active editor, no analysis in this mode!

            synchronized (lock) {
                fastParsesDone.clear();
            }

            print("----------- Setting invalid contents ---------");
            setFileContents(invalidMod1Contents);
            goToIdleLoopUntilCondition(getFastModulesParsedCondition("pack1.pack2.mod1"));
            goToManual(TIME_FOR_ANALYSIS); //2 seconds would be enough for errors to appear
            goToIdleLoopUntilCondition(getNoErrorMarkersCondition(), getMarkers());

            synchronized (lock) {
                fastParsesDone.clear();
            }

            print("----------- Setting valid contents ---------");
            setFileContents(validMod1Contents);
            goToManual(TIME_FOR_ANALYSIS); //2 seconds would be enough for errors to appear
            goToIdleLoopUntilCondition(getNoErrorMarkersCondition(), getMarkers());
            assertEquals(1, fastParsesDone.size());
        } finally {
            FastDefinitionsParser.parseCallbacks.remove(parseFastDefinitionsCallback);
            AnalysisBuilderRunnable.analysisBuilderListeners.remove(analysisErrorCallback);
        }

        ICallback<Object, IResource> analysisCallback = getAnalysisCallback();
        AnalysisBuilderRunnable.analysisBuilderListeners.add(analysisCallback);

        try {
            //ok, now, let's check it
            print("----------- Opening editor ---------");
            resourcesAnalyzed.clear();
            editor = (PyEdit) PyOpenEditor.doOpenEditor(mod1);
            //in 3 seconds, 1 analysis should happen (because we've just opened the editor and the markers are only
            //computed when it's opened)
            goToManual(TIME_FOR_ANALYSIS);
            assertEquals("Expected 1 resource analyzed. Found: " + resourcesAnalyzed, 1, resourcesAnalyzed.size());

            print("----------- Setting invalid contents ---------");
            setFileContents(invalidMod1Contents);
            goToIdleLoopUntilCondition(get1ResourceAnalyzed(), getResourcesAnalyzed());
            goToManual(TIME_FOR_ANALYSIS);
            goToIdleLoopUntilCondition(getHasSyntaxErrorMarkersCondition(mod1), getMarkers());

            goToManual(TIME_FOR_ANALYSIS);
            resourcesAnalyzed.clear();
            print("------------- Requesting analysis -------------");
            AnalyzeOnRequestAction analyzeOnRequestAction = new AnalyzeOnRequestSetter.AnalyzeOnRequestAction(editor);
            analyzeOnRequestAction.run();
            goToIdleLoopUntilCondition(get1ResourceAnalyzed(), getResourcesAnalyzed());

            assertEquals(1, resourcesAnalyzed.size());

            print("----------- Reopening editor ---------");
            resourcesAnalyzed.clear();
            editor.close(false);
            //removes the markers when the editor is closed
            goToManual(TIME_FOR_ANALYSIS);
            goToIdleLoopUntilCondition(getNoErrorMarkersCondition(), getMarkers());
            editor = (PyEdit) PyOpenEditor.doOpenEditor(mod1);
            goToManual(TIME_FOR_ANALYSIS);
            goToIdleLoopUntilCondition(get1ResourceAnalyzed(), getResourcesAnalyzed());
            goToIdleLoopUntilCondition(getHasBothErrorMarkersCondition(), getMarkers());

            print("----------- Changing editor contents and saving ---------");
            resourcesAnalyzed.clear();
            editor.getDocument().set(invalidMod1Contents + "\n");
            editor.doSave(null);
            goToManual(TIME_FOR_ANALYSIS);
            goToIdleLoopUntilCondition(get1ResourceAnalyzed(), getResourcesAnalyzed());
            goToIdleLoopUntilCondition(getHasBothErrorMarkersCondition(), getMarkers());

            print("----------- Changing editor input ---------");
            IPath mod2Path = mod1.getFullPath().removeLastSegments(1).append("mod2.py");
            mod1.copy(mod2Path, true, null);
            mod2 = (IFile) ResourcesPlugin.getWorkspace().getRoot().findMember(mod2Path);
            editor.setInput(new FileEditorInput(mod2));

            //give it some time
            goToManual(TIME_FOR_ANALYSIS);
            goToIdleLoopUntilCondition(getNoErrorMarkersCondition(), getMarkers());
            goToIdleLoopUntilCondition(getHasBothErrorMarkersCondition(mod2), getMarkers());

            print("----------- Create new editor with same input ---------");
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            IWorkbenchPage page = window.getActivePage();
            editor2 = (PyEdit) page.openEditor(editor.getEditorInput(), editor.getSite().getId(), true,
                    IWorkbenchPage.MATCH_NONE);

            //give it some time
            goToManual(TIME_FOR_ANALYSIS);
            goToIdleLoopUntilCondition(getHasBothErrorMarkersCondition(mod2), getMarkers());
            editor2.close(false);
            editor2 = null;

            goToIdleLoopUntilCondition(getHasBothErrorMarkersCondition(mod2), getMarkers());
            editor.close(false);
            goToManual(TIME_FOR_ANALYSIS);
            goToIdleLoopUntilCondition(getNoErrorMarkersCondition(mod2), getMarkers());
            editor = (PyEdit) PyOpenEditor.doOpenEditor(mod1); //leave it open

        } finally {
            AnalysisBuilderRunnable.analysisBuilderListeners.remove(analysisCallback);
        }
    }

    private ICallback<Object, IResource> getAnalysisErrorCallback() {
        return new ICallback<Object, IResource>() {

            @Override
            public Object call(IResource arg) {
                throw new RuntimeException("Should not be called in this case!!");
            }

        };
    }

    private ICallback<Object, Tuple<String, SimpleNode>> getParseFastDefinitionsCallback() {
        return new ICallback<Object, Tuple<String, SimpleNode>>() {

            @Override
            public Object call(Tuple<String, SimpleNode> arg) {
                synchronized (lock) {
                    fastParsesDone.add(arg);
                }
                return null;
            }

        };
    }

    private ICallback<String, Object> getMarkers() {
        return new ICallback<String, Object>() {

            @Override
            public String call(Object arg) {
                try {
                    StringBuffer buf = new StringBuffer();

                    buf.append("Contents:");
                    buf.append(FileUtilsFileBuffer.getDocFromResource(mod1).get() + "\n");

                    IMarker[] markers = mod1.findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
                    for (IMarker marker : markers) {
                        buf.append(marker.getAttribute(IMarker.MESSAGE) + "\n");
                    }
                    markers = mod1.findMarkers(AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER, false,
                            IResource.DEPTH_ZERO);
                    for (IMarker marker : markers) {
                        buf.append(marker.getAttribute(IMarker.MESSAGE) + "\n");
                    }
                    return buf.toString();
                } catch (CoreException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private List<IResource> resourcesAnalyzed;

    private ICallback<Object, IResource> getAnalysisCallback() {
        return new ICallback<Object, IResource>() {

            @Override
            public Object call(IResource arg) {
                resourcesAnalyzed.add(arg);
                return null;
            }
        };
    }

    private static boolean initialConditionAlreadySatisfied = false;

    /**
     * @return a condition that'll check if all the needed modules were already checked 
     */
    private ICallback<Boolean, Object> getInitialParsesCondition() {
        if (!initialConditionAlreadySatisfied) {
            initialConditionAlreadySatisfied = true;
            return getModulesParsedCondition("pack1.pack2.mod1", "pack1.pack2.__init__", "pack1.__init__");
        } else {
            return new ICallback<Boolean, Object>() {
                @Override
                public Boolean call(Object arg) {
                    return true;
                }
            };
        }

    }

    private ICallback<String, Object> getParsesDone() {
        return new ICallback<String, Object>() {

            @Override
            public String call(Object arg) {
                HashSet<String> hashSet = new HashSet<String>();
                synchronized (lock) {
                    for (Tuple3<ISimpleNode, Throwable, ParserInfo> tup : parsesDone) {
                        hashSet.add(tup.o3.moduleName);
                    }
                }

                return hashSet.toString();
            }
        };
    }

    /**
     * @return a condition that'll check if all the needed modules were already checked 
     */
    protected ICallback<Boolean, Object> getModulesParsedCondition(final String... modulesParsed) {
        return new ICallback<Boolean, Object>() {

            @Override
            public Boolean call(Object arg) {
                HashSet<String> hashSet = new HashSet<String>();
                synchronized (lock) {
                    for (Tuple3<ISimpleNode, Throwable, ParserInfo> tup : parsesDone) {
                        hashSet.add(tup.o3.moduleName);
                    }
                }
                for (String o : modulesParsed) {
                    if (!hashSet.contains(o)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * @return a condition that'll check if all the needed modules were already checked 
     */
    private ICallback<Boolean, Object> getFastModulesParsedCondition(final String... modulesParsed) {
        return new ICallback<Boolean, Object>() {

            @Override
            public Boolean call(Object arg) {
                HashSet<String> hashSet = new HashSet<String>();
                synchronized (lock) {
                    for (Tuple<String, SimpleNode> tup : fastParsesDone) {
                        hashSet.add(tup.o1);
                    }
                }
                for (String o : modulesParsed) {
                    if (!hashSet.contains(o)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * Will add the arguments received in a parse to the 'parsesDone' list
     * 
     * @return null
     */
    private ICallback<Object, Tuple3<ISimpleNode, Throwable, ParserInfo>> getAddParsesToListListener() {
        return new ICallback<Object, Tuple3<ISimpleNode, Throwable, ParserInfo>>() {

            @Override
            public Object call(Tuple3<ISimpleNode, Throwable, ParserInfo> arg) {
                //                if(arg.o3.moduleName == null){
                //                    print("null");
                //                }
                //                if(arg.o3.initial.trim().length() == 0){
                //                    print("Parsed file with no contents");
                //                }else{
                //                    print("Parsed:");
                //                    print(arg.o3.moduleName);
                //                    print(arg.o3.file);
                //                    print(arg.o3.document.get());
                //                    print("\n\n-------------------");
                //                }
                synchronized (lock) {
                    parsesDone.add(arg);
                }
                return null;
            }
        };
    }

    private ICallback<Boolean, Object> getHasBothErrorMarkersCondition() {
        return getHasBothErrorMarkersCondition(mod1);
    }

    /**
     * Callback that'll check if there are error markers in the mod1.py resource
     */
    private ICallback<Boolean, Object> getHasBothErrorMarkersCondition(final IFile file) {
        return new ICallback<Boolean, Object>() {

            @Override
            public Boolean call(Object arg) {
                try {
                    //must have both problems: syntax and analysis error!!
                    IMarker[] markers = file.findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
                    if (markers.length > 0) {
                        markers = file.findMarkers(AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER, false,
                                IResource.DEPTH_ZERO);
                        if (markers.length > 0) {
                            return true;
                        }
                    }
                    return false;
                } catch (CoreException e) {
                    throw new RuntimeException(e);
                }
            }

        };
    }

    /**
     * Callback that'll check if there are error markers in the mod1.py resource
     */
    private ICallback<Boolean, Object> getHasSyntaxErrorMarkersCondition(final IFile file) {
        return new ICallback<Boolean, Object>() {

            @Override
            public Boolean call(Object arg) {
                try {
                    //must have only syntax error
                    IMarker[] markers = file.findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
                    if (markers.length > 0) {
                        //analysis error can be there or not
                        //                        markers = file.findMarkers(AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
                        //                        if(markers.length == 0){
                        return true;
                        //                        }
                    }
                    return false;
                } catch (CoreException e) {
                    throw new RuntimeException(e);
                }
            }

        };
    }

    private ICallback<Boolean, Object> getNoErrorMarkersCondition() {
        return getNoErrorMarkersCondition(mod1);
    }

    /**
     * Callback that'll check if there are NO error markers in the mod1.py resource
     */
    private ICallback<Boolean, Object> getNoErrorMarkersCondition(final IFile file) {
        return new ICallback<Boolean, Object>() {

            @Override
            public Boolean call(Object arg) {
                try {
                    IMarker[] markers = file.findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
                    if (markers.length != 0) {
                        return false;
                    }
                    markers = file.findMarkers(AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER, false,
                            IResource.DEPTH_ZERO);
                    if (markers.length != 0) {
                        return false;
                    }
                    return true;
                } catch (CoreException e) {
                    throw new RuntimeException(e);
                }
            }

        };
    }
}
