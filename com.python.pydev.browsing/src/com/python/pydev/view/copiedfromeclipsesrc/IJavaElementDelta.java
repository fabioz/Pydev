/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.python.pydev.view.copiedfromeclipsesrc;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IAdaptable;

/**
 * A Java element delta describes changes in Java element between two discrete
 * points in time.  Given a delta, clients can access the element that has 
 * changed, and any children that have changed.
 * <p>
 * Deltas have a different status depending on the kind of change they represent.  
 * The list below summarizes each status (as returned by <code>getKind</code>)
 * and its meaning (see individual constants for a more detailled description):
 * <ul>
 * <li><code>ADDED</code> - The element described by the delta has been added.</li>
 * <li><code>REMOVED</code> - The element described by the delta has been removed.</li>
 * <li><code>CHANGED</code> - The element described by the delta has been changed in some way.  
 * Specification of the type of change is provided by <code>getFlags</code> which returns the following values:
 * <ul>
 * <li><code>F_ADDED_TO_CLASSPATH</code> - A classpath entry corresponding to the element
 * has been added to the project's classpath. This flag is only valid if the element is an 
 * <code>IPackageFragmentRoot</code>.</li>
 * <li><code>F_ARCHIVE_CONTENT_CHANGED</code> - The contents of an archive
 * has changed in some way. This flag is only valid if the element is an <code>IPackageFragmentRoot</code>
 * which is an archive.</li>
 * <li><code>F_CHILDREN</code> - A child of the element has changed in some way.  This flag
 * is only valid if the element is an <code>IParent</code>.</li>
 * <li><code>F_CLASSPATH_REORDER</code> - A classpath entry corresponding to the element
 * has changed position in the project's classpath. This flag is only valid if the element is an 
 * <code>IPackageFragmentRoot</code>.</li>
 * <li><code>F_CLOSED</code> - The underlying <code>IProject</code>
 * has been closed. This flag is only valid if the element is an <code>IJavaProject</code>.</li>
 * <li><code>F_CONTENT</code> - The contents of the element have been altered.  This flag
 * is only valid for elements which correspond to files.</li>
 *<li><code>F_FINE_GRAINED</code> - The delta is a fine-grained delta, that is, an analysis down
 * to the members level was done to determine if there were structural changes to members of the element.</li>
 * <li><code>F_MODIFIERS</code> - The modifiers on the element have changed in some way. 
 * This flag is only valid if the element is an <code>IMember</code>.</li>
 * <li><code>F_OPENED</code> - The underlying <code>IProject</code>
 * has been opened. This flag is only valid if the element is an <code>IJavaProject</code>.</li>
 * <li><code>F_REMOVED_FROM_CLASSPATH</code> - A classpath entry corresponding to the element 
 * has been removed from the project's classpath. This flag is only valid if the element is an 
 * <code>IPackageFragmentRoot</code>.</li>
 * <li><code>F_SOURCEATTACHED</code> - The source attachment path or the source attachment root path
 * of a classpath entry corresponding to the element was added. This flag is only valid if the element is an 
 * <code>IPackageFragmentRoot</code>.</li>
 * <li><code>F_SOURCEDETACHED</code> - The source attachment path or the source attachment root path
 * of a classpath entry corresponding to the element was removed. This flag is only valid if the element is an 
 * <code>IPackageFragmentRoot</code>.</li>
 * <li><code>F_SUPER_TYPES</code> - One of the supertypes of an <code>IType</code> has changed</li>.
 * </ul>
 * </li>
 * </ul>
 * </p>
 * <p>
 * Move operations are indicated by other change flags, layered on top
 * of the change flags described above. If element A is moved to become B,
 * the delta for the  change in A will have status <code>REMOVED</code>,
 * with change flag <code>F_MOVED_TO</code>. In this case,
 * <code>getMovedToElement</code> on delta A will return the handle for B.
 * The  delta for B will have status <code>ADDED</code>, with change flag
 * <code>F_MOVED_FROM</code>, and <code>getMovedFromElement</code> on delta
 * B will return the handle for A. (Note, the handle to A in this case represents
 * an element that no longer exists).
 * </p>
 * <p>
 * Note that the move change flags only describe the changes to a single element, they
 * do not imply anything about the parent or children of the element.
 * </p>
 * <p>
 * The <code>F_ADDED_TO_CLASSPATH</code>, <code>F_REMOVED_FROM_CLASSPATH</code> and
 * <code>F_CLASSPATH_REORDER</code> flags are triggered by changes to a project's classpath. They do not mean that
 * the underlying resource was added, removed or changed. For example, if a project P already contains a folder src, then 
 * adding a classpath entry with the 'P/src' path to the project's classpath will result in an <code>IJavaElementDelta</code> 
 * with the <code>F_ADDED_TO_CLASSPATH</code> flag for the <code>IPackageFragmentRoot</code> P/src.
 * On the contrary, if a resource is physically added, removed or changed and this resource corresponds to a classpath
 * entry of the project, then an <code>IJavaElementDelta</code> with the <code>ADDED</code>, 
 * <code>REMOVED</code>, or <code>CHANGED</code> kind will be fired.
 * </p>
 * <p>
 * Note that when a source attachment path or a source attachment root path is changed, then the flags of the delta contain
 * both <code>F_SOURCEATTACHED</code> and <code>F_SOURCEDETTACHED</code>.
 * </p>
 * <p>
 * No assumptions should be made on whether the java element delta tree is rooted at the <code>IJavaModel</code>
 * level or not.
 * </p>
 * <p>
 * <code>IJavaElementDelta</code> object are not valid outside the dynamic scope
 * of the notification.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 */
public interface IJavaElementDelta {

	/**
	 * Status constant indicating that the element has been added.
	 * Note that an added java element delta has no children, as they are all implicitely added.
	 */
	public int ADDED = 1;

	/**
	 * Status constant indicating that the element has been removed.
	 * Note that a removed java element delta has no children, as they are all implicitely removed.
	 */
	public int REMOVED = 2;

	/**
	 * Status constant indicating that the element has been changed,
	 * as described by the change flags.
	 * 
	 * @see #getFlags()
	 */
	public int CHANGED = 4;

	/**
	 * Change flag indicating that the content of the element has changed.
	 * This flag is only valid for elements which correspond to files.
	 */
	public int F_CONTENT = 0x00001;

	/**
	 * Change flag indicating that the modifiers of the element have changed.
	 * This flag is only valid if the element is an <code>IMember</code>. 
	 */
	public int F_MODIFIERS = 0x00002;

	/**
	 * Change flag indicating that there are changes to the children of the element.
	 * This flag is only valid if the element is an <code>IParent</code>. 
	 */
	public int F_CHILDREN = 0x00008;

	/**
	 * Change flag indicating that the element was moved from another location.
	 * The location of the old element can be retrieved using <code>getMovedFromElement</code>.
	 */
	public int F_MOVED_FROM = 0x00010;

	/**
	 * Change flag indicating that the element was moved to another location.
	 * The location of the new element can be retrieved using <code>getMovedToElement</code>.
	 */
	public int F_MOVED_TO = 0x00020;

	/**
	 * Change flag indicating that a classpath entry corresponding to the element has been added to the project's classpath. 
	 * This flag is only valid if the element is an <code>IPackageFragmentRoot</code>.
	 */
	public int F_ADDED_TO_CLASSPATH = 0x00040;

	/**
	 * Change flag indicating that a classpath entry corresponding to the element has been removed from the project's 
	 * classpath. This flag is only valid if the element is an <code>IPackageFragmentRoot</code>.
	 */
	public int F_REMOVED_FROM_CLASSPATH = 0x00080;

	/**
	 * Change flag indicating that a classpath entry corresponding to the element has changed position in the project's 
	 * classpath. This flag is only valid if the element is an <code>IPackageFragmentRoot</code>.
	 * @deprecated Use F_REORDER instead.
	 */
	public int F_CLASSPATH_REORDER = 0x00100;
	/**
	 * Change flag indicating that the element has changed position relatively to its siblings. 
	 * If the element is an <code>IPackageFragmentRoot</code>,  a classpath entry corresponding 
	 * to the element has changed position in the project's classpath.
	 * 
	 * @since 2.1
	 */
	public int F_REORDER = 0x00100;

	/**
	 * Change flag indicating that the underlying <code>IProject</code> has been
	 * opened. This flag is only valid if the element is an <code>IJavaProject</code>. 
	 */
	public int F_OPENED = 0x00200;

	/**
	 * Change flag indicating that the underlying <code>IProject</code> has been
	 * closed. This flag is only valid if the element is an <code>IJavaProject</code>. 
	 */
	public int F_CLOSED = 0x00400;

	/**
	 * Change flag indicating that one of the supertypes of an <code>IType</code>
	 * has changed.
	 */
	public int F_SUPER_TYPES = 0x00800;

	/**
	 * Change flag indicating that the source attachment path or the source attachment root path of a classpath entry 
	 * corresponding to the element was added. This flag is only valid if the element is an 
	 * <code>IPackageFragmentRoot</code>.
	 */
	public int F_SOURCEATTACHED = 0x01000;	

	/**
	 * Change flag indicating that the source attachment path or the source attachment root path of a classpath entry 
	 * corresponding to the element was removed. This flag is only valid if the element is an 
	 * <code>IPackageFragmentRoot</code>.
	 */
	public int F_SOURCEDETACHED = 0x02000;	
	
	/**
	 * Change flag indicating that this is a fine-grained delta, that is, an analysis down
	 * to the members level was done to determine if there were structural changes to
	 * members.
	 * <p>
	 * Clients can use this flag to find out if a compilation unit 
     * that have a <code>F_CONTENT</code> change should assume that there are 
     * no finer grained changes (<code>F_FINE_GRAINED</code> is set) or if 
     * finer grained changes were not considered (<code>F_FINE_GRAINED</code> 
     * is not set). 
     * 
     * @since 2.0
	 */
	public int F_FINE_GRAINED = 0x04000;

	/**
	 * Change flag indicating that the element's archive content on the classpath has changed.
	 * This flag is only valid if the element is an <code>IPackageFragmentRoot</code>
	 * which is an archive.
	 * 
	 * @see IPackageFragmentRoot#isArchive()
	 * @since 2.0
	 */
	public int F_ARCHIVE_CONTENT_CHANGED = 0x08000;
	
	/**
	 * Change flag indicating that a compilation unit has become a primary working copy, or that a 
	 * primary working copy has reverted to a compilation unit.
	 * This flag is only valid if the element is an <code>ICompilationUnit</code>.
	 * 
	 * @since 3.0
	 */
	public int F_PRIMARY_WORKING_COPY = 0x10000;

	/**
	 * Change flag indicating that the raw classpath (or the output folder) of a project has changed. 
	 * This flag is only valid if the element is an <code>IJavaProject</code>.
	 *
	 * @since 3.0
	 */
	public int F_CLASSPATH_CHANGED = 0x20000;

	/**
	 * Change flag indicating that the resource of a primary compilation unit has changed.
	 * This flag is only valid if the element is a primary <code>ICompilationUnit</code>.
	 * 
	 * @since 3.0
	 */
	public int F_PRIMARY_RESOURCE = 0x40000;

	/**
	 * Returns deltas for the children that have been added.
	 * @return deltas for the children that have been added
	 */
	public IJavaElementDelta[] getAddedChildren();

	/**
	 * Returns deltas for the affected (added, removed, or changed) children.
	 * @return deltas for the affected (added, removed, or changed) children
	 */
	public IJavaElementDelta[] getAffectedChildren();

	/**
	 * Returns deltas for the children which have changed.
	 * @return deltas for the children which have changed
	 */
	public IJavaElementDelta[] getChangedChildren();

	/**
	 * Returns the element that this delta describes a change to.
	 * @return the element that this delta describes a change to
	 */
	public IAdaptable getElement();

	/**
	 * Returns flags that describe how an element has changed. 
	 * Such flags should be tested using the <code>&</code> operand. For example:
	 * <pre>
	 * if ((delta.getFlags() & IJavaElementDelta.F_CONTENT) != 0) {
	 * 	// the delta indicates a content change
	 * }
	 * </pre>
	 *
	 * @return flags that describe how an element has changed
	 */
	public int getFlags();

	/**
	 * Returns the kind of this delta - one of <code>ADDED</code>, <code>REMOVED</code>,
	 * or <code>CHANGED</code>.
	 * 
	 * @return the kind of this delta
	 */
	public int getKind();

	/**
	 * Returns an element describing this element before it was moved
	 * to its current location, or <code>null</code> if the
	 * <code>F_MOVED_FROM</code> change flag is not set. 
	 * 
	 * @return an element describing this element before it was moved
	 * to its current location, or <code>null</code> if the
	 * <code>F_MOVED_FROM</code> change flag is not set
	 */
	public IAdaptable getMovedFromElement();

	/**
	 * Returns an element describing this element in its new location,
	 * or <code>null</code> if the <code>F_MOVED_TO</code> change
	 * flag is not set.
	 * 
	 * @return an element describing this element in its new location,
	 * or <code>null</code> if the <code>F_MOVED_TO</code> change
	 * flag is not set
	 */
	public IAdaptable getMovedToElement();

	/**
	 * Returns deltas for the children which have been removed.
	 * 
	 * @return deltas for the children which have been removed
	 */
	public IJavaElementDelta[] getRemovedChildren();

	/**
	 * Returns the collection of resource deltas.
	 * <p>
	 * Note that resource deltas, like Java element deltas, are generally only valid
	 * for the dynamic scope of an event notification. Clients must not hang on to
	 * these objects.
	 * </p>
	 *
	 * @return the underlying resource deltas, or <code>null</code> if none
	 */
	public IResourceDelta[] getResourceDeltas();
}
