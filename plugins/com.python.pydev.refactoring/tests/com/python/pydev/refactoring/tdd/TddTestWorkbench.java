/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.io.ByteArrayInputStream;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.parser.IParserObserver;
import org.python.pydev.core.parser.ISimpleNode;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.editorinput.PyOpenEditor;
import org.python.pydev.parser.PyParser;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.analysis.AnalysisRequestsTestWorkbench;
import com.python.pydev.analysis.builder.AnalysisParserObserver;
import com.python.pydev.analysis.builder.AnalysisRunner;

public class TddTestWorkbench extends AbstractWorkbenchTestCase implements IParserObserver {

    public static Test suite() {
        TestSuite suite = new TestSuite(TddTestWorkbench.class.getName());
        
        suite.addTestSuite(TddTestWorkbench.class); 
        
        if (suite.countTestCases() == 0) {
            throw new Error("There are no test cases to run");
        } else {
            return suite;
        }
    }
    
    private PyParser parser;
    private int parserNotified;
    
    public void testCheckTddQuickFixes() throws Exception {
        //We have to wait a bit until the info is setup for the tests to work...
        waitForModulesManagerSetup();
        
        checkCreateClass();

        checkCreateClassWithParams();
        
        checkCreateClassAtOtherModule();
        
        checkCreateMethod();
        
        checkCreateMethodAtOtherModule();
        
        checkCreateBoundMethod();
        
        checkCreateMethodAtOtherModule2();
        
    }

    
    
    protected void checkCreateBoundMethod() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents = "" +
        		"class Foo(object):\n" +
        		"    def m1(self):\n" +
        		"        self.bar()";
        setContentsAndWaitReparseAndError(mod1Contents, false); //no error here
        
        TddCodeGenerationQuickFixParticipant quickFix = new TddCodeGenerationQuickFixParticipant();
        IDocument doc = editor.getDocument();
        PySelection ps = new PySelection(doc, doc.getLength()-"r()".length());
        assertTrue(quickFix.isValid(ps, "", editor, doc.getLength()-"r()".length()));
        List<ICompletionProposal> props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, 0);
        try {
            findCompletion(props, "Create bar method at Foo").apply(editor.getISourceViewer(), '\n', 0, 0);
            assertContentsEqual("" +
                    "class Foo(object):\n" +
                    "\n"+
                    "    def bar(self):\n" +
                    "        pass\n" +
                    "    \n"+
                    "    \n"+
                    "    def m1(self):\n" +
                    "        self.bar()"+
                    "", editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }
    
    protected void checkCreateMethod() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents = "Foo";
        setContentsAndWaitReparseAndError(mod1Contents);
        
        TddCodeGenerationQuickFixParticipant quickFix = new TddCodeGenerationQuickFixParticipant();
        PySelection ps = new PySelection(editor.getDocument(), 0);
        assertTrue(quickFix.isValid(ps, "", editor, 0));
        List<ICompletionProposal> props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, 0);
        try {
            findCompletion(props, "Create Foo method").apply(editor.getISourceViewer(), '\n', 0, 0);
            assertContentsEqual("" +
                    "def Foo():\n" +
                    "    pass\n" +
                    "\n" +
                    "\n" +
                    "Foo" +
                    "", editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }

    protected void checkCreateMethodAtOtherModule() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        IFile mod2 = initFile.getParent().getFile(new Path("other_module2.py"));
        mod2.create(new ByteArrayInputStream("".getBytes()), true, null);
        PyEdit editor2 = (PyEdit) PyOpenEditor.doOpenEditor(mod2);
        try {
            goToManual(AnalysisRequestsTestWorkbench.TIME_FOR_ANALYSIS); //give it a bit more time...
            mod1Contents = "" +
            "import other_module2\n" +
            "other_module2.Foo(a, b)";
            setContentsAndWaitReparseAndError(mod1Contents);
            
            quickFix = new TddCodeGenerationQuickFixParticipant();
            int offset = mod1Contents.length()-"o(a, b)".length();
            ps = new PySelection(editor.getDocument(), offset);
            assertTrue(quickFix.isValid(ps, "", editor, offset));
            props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
            
            findCompletion(props, "Create Foo method at other_module2.py").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertContentsEqual("" +
                    "def Foo(a, b):\n" +
                    "    pass\n" +
                    "\n" +
                    "\n" +
                    "", editor2.getDocument().get());
            
        } finally {
            editor2.close(false);
            mod2.delete(true, null);
        }
    }
    protected void checkCreateMethodAtOtherModule2() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        IFile mod2 = initFile.getParent().getFile(new Path("other_module3.py"));
        String str ="" +
        "class Bar(object):\n" +
        "    pass\n" +
        "";
        mod2.create(new ByteArrayInputStream(str.getBytes()), true, null);
        PyEdit editor2 = (PyEdit) PyOpenEditor.doOpenEditor(mod2);
        try {
            goToManual(AnalysisRequestsTestWorkbench.TIME_FOR_ANALYSIS); //give it a bit more time...
            mod1Contents = "" +
            "import other_module3\n" +
            "other_module3.Bar.Foo(10, 20)";
            setContentsAndWaitReparseAndError(mod1Contents);
            
            quickFix = new TddCodeGenerationQuickFixParticipant();
            int offset = mod1Contents.length()-"o(a, b)".length();
            ps = new PySelection(editor.getDocument(), offset);
            assertTrue(quickFix.isValid(ps, "", editor, offset));
            props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
            
            findCompletion(props, "Create Foo classmethod at Bar in other_module3.py").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertContentsEqual("" +
                    "class Bar(object):\n" +
                    "    pass\n" +
                    "\n" +
                    "    @classmethod\n" +
                    "    def Foo(cls, param1, param2):\n" +
                    "        pass\n" +
                    "    \n" +
                    "    \n" +
                    "", editor2.getDocument().get());
            
        } finally {
            editor2.close(false);
            mod2.delete(true, null);
        }
    }
    
    
    protected void checkCreateClass() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents = "Foo";
        setContentsAndWaitReparseAndError(mod1Contents);
        
        TddCodeGenerationQuickFixParticipant quickFix = new TddCodeGenerationQuickFixParticipant();
        PySelection ps = new PySelection(editor.getDocument(), 0);
        assertTrue(quickFix.isValid(ps, "", editor, 0));
        List<ICompletionProposal> props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, 0);
        try {
            findCompletion(props, "Create Foo class").apply(editor.getISourceViewer(), '\n', 0, 0);
            assertContentsEqual("" +
                    "class Foo(object):\n" +
                    "    pass\n" +
                    "\n" +
                    "\n" +
                    "Foo" +
                    "", editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }
    
    protected void checkCreateClassWithParams() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        mod1Contents = "Foo(call1(ueo), 'aa,bb', 10, cc)";
        setContentsAndWaitReparseAndError(mod1Contents);
        quickFix = new TddCodeGenerationQuickFixParticipant();
        ps = new PySelection(editor.getDocument(), 0);
        assertTrue(quickFix.isValid(ps, "", editor, 0));
        props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, 0);
        try {
            findCompletion(props, "Create Foo class").apply(editor.getISourceViewer(), '\n', 0, 0);
            assertContentsEqual("" + "class Foo(object):\n" + "    \n" + "    def __init__(self, call_1, param1, param2, cc):\n"
                    + "        pass\n" + "\n" + "\n" + "Foo(call1(ueo), 'aa,bb', 10, cc)" + "", editor.getDocument().get());
        } finally {
            editor.doRevertToSaved();
        }
    }


    
    protected void checkCreateClassAtOtherModule() throws CoreException, BadLocationException, MisconfigurationException {
        String mod1Contents;
        TddCodeGenerationQuickFixParticipant quickFix;
        PySelection ps;
        List<ICompletionProposal> props;
        IFile mod2 = initFile.getParent().getFile(new Path("other_module.py"));
        mod2.create(new ByteArrayInputStream("".getBytes()), true, null);
        PyEdit editor2 = (PyEdit) PyOpenEditor.doOpenEditor(mod2);
        try {
            goToManual(AnalysisRequestsTestWorkbench.TIME_FOR_ANALYSIS); //give it a bit more time...
            mod1Contents = "" +
            "import other_module\n" +
            "other_module.Foo";
            setContentsAndWaitReparseAndError(mod1Contents);
            
            quickFix = new TddCodeGenerationQuickFixParticipant();
            int offset = mod1Contents.length()-1;
            ps = new PySelection(editor.getDocument(), offset);
            assertTrue(quickFix.isValid(ps, "", editor, offset));
            props = quickFix.getProps(ps, PydevPlugin.getImageCache(), editor.getEditorFile(), editor.getPythonNature(), editor, offset);
            
            findCompletion(props, "Create Foo class at other_module.py").apply(editor.getISourceViewer(), '\n', 0, offset);
            assertContentsEqual("" +
                    "class Foo(object):\n" +
                    "    pass\n" +
                    "\n" +
                    "\n" +
                    "", editor2.getDocument().get());
            
        } finally {
            editor2.close(false);
            mod2.delete(true, null);
        }
    }

    protected ICompletionProposalExtension2 findCompletion(List<ICompletionProposal> props, String expectedCompletion) {
        FastStringBuffer buf = new FastStringBuffer("Available: ",300);
        for (ICompletionProposal iCompletionProposal : props) {
            if(iCompletionProposal.getDisplayString().equals(expectedCompletion)){
                ICompletionProposalExtension2 p = (ICompletionProposalExtension2) iCompletionProposal;
                return p;
            }
            buf.append("\n");
            buf.append(iCompletionProposal.getDisplayString());
        }
        throw new AssertionError("Could not find completion: "+expectedCompletion+"\n"+buf);
    }
    

    protected void setContentsAndWaitReparseAndError(String mod1Contents) throws CoreException {
        setContentsAndWaitReparseAndError(mod1Contents, true);
    }
    
    protected void setContentsAndWaitReparseAndError(String mod1Contents, boolean waitForError) throws CoreException {
        setFileContents(mod1Contents);
        
        parser = editor.getParser();
        parser.addParseListener(this);
        
        ICallback<Boolean, Object> parseHappenedCondition = getParseHappenedCondition();
        
        parser.forceReparse(new Tuple<String, Boolean>(
                AnalysisParserObserver.ANALYSIS_PARSER_OBSERVER_FORCE, true));
        goToIdleLoopUntilCondition(parseHappenedCondition);

        if(waitForError){
            goToIdleLoopUntilCondition(getHasBothErrorMarkersCondition(editor.getIFile()));
        }
    }
    

    protected void assertContentsEqual(String expected, String generated) {
        assertEquals(StringUtils.replaceNewLines(expected, "\n"), StringUtils.replaceNewLines(generated, "\n"));
    }


    public void parserChanged(ISimpleNode root, IAdaptable file, IDocument doc) {
        parser.removeParseListener(this);
        this.parserNotified += 1;
    }

    public void parserError(Throwable error, IAdaptable file, IDocument doc) {
        parser.removeParseListener(this);
        this.parserNotified += 1;
    }
    
    
    private ICallback<Boolean, Object> getParseHappenedCondition() {
        final int currentNotified = this.parserNotified;
        return new ICallback<Boolean, Object>(){
            
            public Boolean call(Object arg) {
                return parserNotified > currentNotified;
            }
        };
    }
    
    /**
     * Callback that'll check if there are error markers in the mod1.py resource
     */
    private ICallback<Boolean, Object> getHasBothErrorMarkersCondition(final IFile file) {
        return new ICallback<Boolean, Object>(){
            
            public Boolean call(Object arg) {
                try {
                    //must have both problems: syntax and analysis error!!
                    IMarker[] markers = file.findMarkers(AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
                    return markers.length > 0;
                } catch (CoreException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

}
