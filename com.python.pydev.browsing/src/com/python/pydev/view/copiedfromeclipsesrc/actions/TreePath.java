/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.python.pydev.view.copiedfromeclipsesrc.actions;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.IElementComparer;
import com.python.pydev.view.copiedfromeclipsesrc.actions.DefaultElementComparer;

/**
 * A tree path denotes a model element in a tree viewer. Tree path
 * objects do have value semantics.
 * 
 * @since 3.1
 */
public final class TreePath {
	private Object[] fSegments;
	private int fHash;
	
	public TreePath(Object[] segments) {
		Assert.isNotNull(segments);
		for (int i= 0; i < segments.length; i++) {
			Assert.isNotNull(segments[i]);
		}
		fSegments= segments;
	}
	
	public Object getSegment(int index) {
		return fSegments[index];
	}
	
	public int getSegmentCount() {
		return fSegments.length;
	}
	
	public Object getFirstSegment() {
		if (fSegments.length == 0)
			return null;
		return fSegments[0];
	}
	
	public Object getLastSegment() {
		if (fSegments.length == 0)
			return null;
		return fSegments[fSegments.length - 1];
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof TreePath))
			return false;
		TreePath otherPath= (TreePath)other;
		if (fSegments.length != otherPath.fSegments.length)
			return false;
		for (int i= 0; i < fSegments.length; i++) {
			if (!fSegments[i].equals(otherPath.fSegments[i]))
				return false;
		}
		return true;
	}
	
	public int hashCode() {
		if (fHash != 0)
			return fHash;
		for (int i= 0; i < fSegments.length; i++) {
			fHash= fHash + fSegments[i].hashCode();
		}
		return fHash;
	}
	
	public boolean equals(TreePath otherPath, IElementComparer comparer) {
		if (comparer == null)
			comparer= DefaultElementComparer.INSTANCE;
		if (otherPath == null)
			return false;
		if (fSegments.length != otherPath.fSegments.length)
			return false;
		for (int i= 0; i < fSegments.length; i++) {
			if (!comparer.equals(fSegments[i], otherPath.fSegments[i]))
				return false;
		}
		return true;
	}
}
