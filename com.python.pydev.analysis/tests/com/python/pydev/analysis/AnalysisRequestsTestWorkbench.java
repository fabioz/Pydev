package com.python.pydev.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.Tuple3;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.editorinput.PyOpenEditor;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.PyParser.ParserInfo;
import org.python.pydev.parser.jython.SimpleNode;

import com.python.pydev.analysis.builder.AnalysisRunner;

/**
 * This test is used to see if the code-analysis is correctly requested on a refresh of some file.
 * 
 * @author Fabio
 */
public class AnalysisRequestsTestWorkbench extends AbstractWorkbenchTestCase{

	private List<Tuple3<SimpleNode, Throwable, ParserInfo>> parsesDone = new ArrayList<Tuple3<SimpleNode,Throwable,ParserInfo>>();
	private String invalidMod1Contents = "import java.lang.Class\njava.lang.Class\nkkk invalid kkk\nprint kkk";
	private String validMod1Contents = "import java.lang.Class\njava.lang.Class";
	private ICallback<Object, Tuple3<SimpleNode, Throwable, ParserInfo>> addParsesToListListener;
	
	@Override
	protected void setUp() throws Exception {
		addParsesToListListener = getAddParsesToListListener();
		PyParser.successfulParseListeners.add(addParsesToListListener);
		super.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		PyParser.successfulParseListeners.remove(addParsesToListListener);
	}
	
	
	
	public void testRefreshAnalyzesFiles() throws Exception {
		editor.close(false);
		goToIdleLoopUntilCondition(getInitialParsesCondition()); //just to have any parse events consumed
		
		try {
			parsesDone.clear();
			setFileContents(invalidMod1Contents);
			goToIdleLoopUntilCondition(getHasErrorMarkersCondition());
			
			parsesDone.clear();
			setFileContents(validMod1Contents);
			goToIdleLoopUntilCondition(getNoErrorMarkersCondition());
			assertEquals(1, parsesDone.size());
			
		} finally {
			//leave the editor open for other tests
			editor = (PyEdit) PyOpenEditor.doOpenEditor(mod1);
		}
	}


	/**
	 * @return a condition that'll check if all the needed modules were already checked 
	 */
	private ICallback<Boolean, Object> getInitialParsesCondition() {
		return new ICallback<Boolean, Object>(){

			public Boolean call(Object arg) {
				HashSet<String> hashSet = new HashSet<String>();
				for(Tuple3<SimpleNode, Throwable, ParserInfo> tup:parsesDone){
					hashSet.add(tup.o3.moduleName);
				}
				if(hashSet.contains("pack1.pack2.mod1") && hashSet.contains("pack1.pack2.__init__") && hashSet.contains("pack1.__init__")){
					return true;
				}
				return false;
			}};
	}

	
	/**
	 * Will add the arguments received in a parse to the 'parsesDone' list
	 * 
	 * @return null
	 */
	private ICallback<Object, Tuple3<SimpleNode, Throwable, ParserInfo>> getAddParsesToListListener() {
		return new ICallback<Object, Tuple3<SimpleNode,Throwable,ParserInfo>>(){
			
			public Object call(Tuple3<SimpleNode, Throwable, ParserInfo> arg) {
//				if(arg.o3.initial.trim().length() == 0){
//					System.out.println("Parsed file with no contents");
//				}else{
//					System.out.println("Parsed:");
//					System.out.println(arg.o3.moduleName);
//					System.out.println(arg.o3.file);
//					System.out.println(arg.o3.document.get());
//					System.out.println("\n\n-------------------");
//				}
				parsesDone.add(arg);
				return null;
			}};
	}


	/**
	 * Callback that'll check if there are error markers in the mod1.py resource
	 */
	private ICallback<Boolean, Object> getHasErrorMarkersCondition() {
		return new ICallback<Boolean, Object>(){

			@Override
			public Boolean call(Object arg) {
				try {
					IMarker[] markers = mod1.findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
					if(markers.length == 0){
						return false;
					}
					markers = mod1.findMarkers(AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
					if(markers.length == 0){
						return false;
					}
					return true;
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
			}
			
		};
	}


	/**
	 * Callback that'll check if there are NO error markers in the mod1.py resource
	 */
	private ICallback<Boolean, Object> getNoErrorMarkersCondition() {
		return new ICallback<Boolean, Object>(){
			
			@Override
			public Boolean call(Object arg) {
				try {
					IMarker[] markers = mod1.findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
					if(markers.length != 0){
						return false;
					}
					markers = mod1.findMarkers(AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
					if(markers.length != 0){
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
