/*
 * Author: ggheorghiu
 * Created: Sept. 10, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.actions;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionDelegate;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.pyunit.ITestRunListener;

/**
 * Implements "pyUnit Test..." extension for org.eclipse.ui.popupMenus.
 * 
 * <p>Launches python process and runs pyUnit's unittest 
 * 
 */
public class PythonTestActionDelegate extends ActionDelegate
	implements IObjectActionDelegate {

	/**
	 * @author ggheorg
	 *
	 * TODO To change the template for this generated type comment go to
	 * Window - Preferences - Java - Code Style - Code Templates
	 */
	public static class Listener implements ITestRunListener {

		private boolean passed = true;
		public void testsStarted(int testCount) {
			System.out.println("STARTING TEST RUN: " + String.valueOf(testCount) + " tests");
		}

		public void testsFinished() {
			String message = passed ? "PASS": "FAIL";
			System.out.println("END TEST RUN");
			String messageLong = "TEST RUN STATUS: " + message;
			System.out.println(messageLong);

			//MessageDialog.openInformation(null, "Test Results", messageLong);
		}

		public void testStarted(String klass, String method) {
		}

		public void testFailed(String klass, String method, String trace) {
			System.out.println("TEST FAILED!");
			System.out.println(klass + " " + method);
			System.out.println(trace);
			passed = false;
		}
	}
	private IFile selectedFile;
	private IWorkbenchPart part;

	public void run(IAction action) {
		if (part != null && selectedFile != null) {
	        try {
			    // get the location of the workspace root
				/*
	            IWorkspace workspace = ResourcesPlugin.getWorkspace();
	            IPath wsRootPath = workspace.getRoot().getLocation();
	            String wsRootDir = wsRootPath.toString();
	            */

				// get the full physical path for the selected file
				IPath fullPath = selectedFile.getLocation(); 
				int segmentCount = fullPath.segmentCount();

				// get the test module name and path so that we can import it in Python
				IPath noextPath = fullPath.removeFileExtension();
				String moduleName =  noextPath.lastSegment();
				IPath modulePath = fullPath.uptoSegment(segmentCount-1);
				String moduleDir = modulePath.toString();

	        	// get the project the file belongs to
			    IProject selectedProject = selectedFile.getProject();

				ITestRunListener listener = new Listener();
				PydevPlugin.getDefault().addTestListener(listener);
				try {
					PydevPlugin.getDefault().runTests(moduleDir, moduleName, selectedProject);
				} catch (CoreException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				PydevPlugin.getDefault().removeTestListener(listener);
				
/*	        	Process p = Runtime.getRuntime().exec(new String[]{"python"});
	            
	            //we have the process...
	            OutputStreamWriter writer = new OutputStreamWriter(p.getOutputStream());
	            int bufsize = 64; // small bufsize so that we can see the progress
	            BufferedReader in = new BufferedReader(new InputStreamReader(p
	                    .getInputStream()), bufsize);
	            BufferedReader eIn = new BufferedReader(new InputStreamReader(
	                    p.getErrorStream()), bufsize);

	            writer.write("import sys, os, unittest\n");
	            writer.write("os.chdir('" + wsRootDir + moduleDir + "')\n");
	            String arg = "sys.path.append('" + wsRootDir + moduleDir + "')\n";
				writer.write(arg);
	            arg = "unittest.TestProgram('" + moduleName + "', argv=['" + moduleName + "', '--verbose'])\n";
	            //arg = "unittest.TestProgram('" + moduleName + "')\n";
	            writer.write(arg);
	            writer.flush();
	            writer.close();
	            
	            String str;
	            ArrayList testResults = new ArrayList();
	            while ((str = eIn.readLine()) != null) {
	                //System.out.println("STDERR: " + str);
	                //System.out.println(str);
	                testResults.add(str);
	            }
	            eIn.close();
	            while ((str = in.readLine()) != null) {
	                //System.out.println("STDOUT: " + str);
	                System.out.println(str);
	            }
	            in.close();
	            p.waitFor();
	            System.out.println("testResults list:");
	            Iterator it = testResults.iterator();
	            while (it.hasNext()) {
	                String line = it.next().toString();
	                
	                System.out.println(line);
	            }	            
*/	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		selectedFile = null;
		if (selection instanceof IStructuredSelection) {
			if (((IStructuredSelection) selection).size() == 1) {
				Object selectedResource = ((IStructuredSelection) selection).getFirstElement();
				if (selectedResource instanceof IFile)
					selectedFile = (IFile) selectedResource;
			}
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.part = targetPart;
	}

}
