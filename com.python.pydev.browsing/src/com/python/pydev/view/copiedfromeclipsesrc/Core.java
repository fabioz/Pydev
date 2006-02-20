package com.python.pydev.view.copiedfromeclipsesrc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Plugin;

public final class Core extends Plugin {
	
	public static IResource create(IResource resource, IProject project) {
		if (resource == null) {
			return null;
		}
		/*int type = resource.getType();
		switch (type) {
			case IResource.PROJECT :
				return create((IProject) resource);
			case IResource.FILE :
				return create((IFile) resource, project);
			case IResource.FOLDER :
				return create((IFolder) resource, project);
			case IResource.ROOT :
				return create((IWorkspaceRoot) resource);
			default :
				return null;
		}*/
		return resource;
	}
		
	/**
	 * Returns the Java element corresponding to the given file, or
	 * <code>null</code> if unable to associate the given file
	 * with a Java element.
	 *
	 * <p>The file must be one of:<ul>
	 *	<li>a <code>.java</code> file - the element returned is the corresponding <code>ICompilationUnit</code></li>
	 *	<li>a <code>.class</code> file - the element returned is the corresponding <code>IClassFile</code></li>
	 *	<li>a <code>.jar</code> file - the element returned is the corresponding <code>IPackageFragmentRoot</code></li>
	 *	</ul>
	 * <p>
	 * Creating a Java element has the side effect of creating and opening all of the
	 * element's parents if they are not yet open.
	 * 
	 * @param file the given file
	 * @return the Java element corresponding to the given file, or
	 * <code>null</code> if unable to associate the given file
	 * with a Java element
	 */	
	public static IFile create(IFile file) {
		return file;
	}
	/**
	 * Returns the package fragment or package fragment root corresponding to the given folder, or
	 * <code>null</code> if unable to associate the given folder with a Java element.
	 * <p>
	 * Note that a package fragment root is returned rather than a default package.
	 * <p>
	 * Creating a Java element has the side effect of creating and opening all of the
	 * element's parents if they are not yet open.
	 * 
	 * @param folder the given folder
	 * @return the package fragment or package fragment root corresponding to the given folder, or
	 * <code>null</code> if unable to associate the given folder with a Java element
	 */
	public static IFolder create(IFolder folder) {
		return folder;
	}
	/**
	 * Returns the Java project corresponding to the given project.
	 * <p>
	 * Creating a Java Project has the side effect of creating and opening all of the
	 * project's parents if they are not yet open.
	 * <p>
	 * Note that no check is done at this time on the existence or the java nature of this project.
	 * 
	 * @param project the given project
	 * @return the Java project corresponding to the given project, null if the given project is null
	 */
	public static IProject create(IProject project) {
		if (project == null) {
			return null;
		}
		return project;
	}
	/**
	 * Returns the Java element corresponding to the given resource, or
	 * <code>null</code> if unable to associate the given resource
	 * with a Java element.
	 * <p>
	 * The resource must be one of:<ul>
	 *	<li>a project - the element returned is the corresponding <code>IJavaProject</code></li>
	 *	<li>a <code>.java</code> file - the element returned is the corresponding <code>ICompilationUnit</code></li>
	 *	<li>a <code>.class</code> file - the element returned is the corresponding <code>IClassFile</code></li>
	 *	<li>a <code>.jar</code> file - the element returned is the corresponding <code>IPackageFragmentRoot</code></li>
	 *  <li>a folder - the element returned is the corresponding <code>IPackageFragmentRoot</code>
	 *    	or <code>IPackageFragment</code></li>
	 *  <li>the workspace root resource - the element returned is the <code>IJavaModel</code></li>
	 *	</ul>
	 * <p>
	 * Creating a Java element has the side effect of creating and opening all of the
	 * element's parents if they are not yet open.
	 * 
	 * @param resource the given resource
	 * @return the Java element corresponding to the given resource, or
	 * <code>null</code> if unable to associate the given resource
	 * with a Java element
	 */
	public static IResource create(IResource resource) {
		return resource;
	}
	/**
	 * Returns the Java model.
	 * 
	 * @param root the given root
	 * @return the Java model, or <code>null</code> if the root is null
	 */
	public static IWorkspaceRoot create(IWorkspaceRoot root) {
		if (root == null) {
			return null;
		}
		return root;
	}	
}
