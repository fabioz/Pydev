/*
 * Author: ggheorghiu
 * Created: Sept. 10, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionDelegate;
import org.python.pydev.plugin.PydevPrefs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Implements "pyUnit Test..." extension for org.eclipse.ui.popupMenus.
 * 
 * <p>Launches python process and runs pyUnit's unittest 
 * 
 */
public class PythonTestActionDelegate extends ActionDelegate
	implements IObjectActionDelegate {

	private IFile selectedFile;
	private IWorkbenchPart part;

	public void run(IAction action) {
		if (part != null && selectedFile != null) {
	        try {
	            Process p = Runtime.getRuntime().exec(new String[]{PydevPrefs.getDefaultInterpreter()});
	            
	            //we have the process...
	            OutputStreamWriter writer = new OutputStreamWriter(p.getOutputStream());
	            int bufsize = 64; // small bufsize so that we can see the progress
	            BufferedReader in = new BufferedReader(new InputStreamReader(p
	                    .getInputStream()), bufsize);
	            BufferedReader eIn = new BufferedReader(new InputStreamReader(
	                    p.getErrorStream()), bufsize);

	            // get the location of the workspace root
	            IWorkspace workspace = ResourcesPlugin.getWorkspace();
	            IWorkspaceRoot wsRoot = workspace.getRoot();
	            IPath wsRootPath = wsRoot.getLocation();
	            String wsRootDir = wsRootPath.toString();
	            
	            // get the location of the selected file relative to the workspace
	            IPath fullPath = selectedFile.getFullPath();
				int segmentCount = fullPath.segmentCount();

				// get the test module name and path so that we can import it in Python
				IPath noextPath = fullPath.removeFileExtension();
				String moduleName =  noextPath.lastSegment();
				IPath modulePath = fullPath.uptoSegment(segmentCount-1);
				String moduleDir = modulePath.toString();

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
	            while ((str = eIn.readLine()) != null) {
	                //System.out.println("STDERR: " + str);
	                System.out.println(str);
	            }
	            eIn.close();
	            while ((str = in.readLine()) != null) {
	                //System.out.println("STDOUT: " + str);
	                System.out.println(str);
	            }
	            in.close();
	            p.waitFor();
	        } catch (IOException e) {
	            e.printStackTrace();
	        } catch (InterruptedException e) {
	        	e.printStackTrace();
	        }
		}
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		selectedFile = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			if (structuredSelection.size() == 1) {
				Object selectedResource = structuredSelection.getFirstElement();
				if (selectedResource instanceof IFile)
					selectedFile = (IFile) selectedResource;
			}
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.part = targetPart;
	}

}
