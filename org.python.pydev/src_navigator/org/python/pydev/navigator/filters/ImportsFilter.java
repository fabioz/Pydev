package org.python.pydev.navigator.filters;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.navigator.elements.PythonNode;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.NodeUtils;

public class ImportsFilter extends ViewerFilter{

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if(element instanceof PythonNode){
			PythonNode node = (PythonNode) element;
			SimpleNode n = node.entry.getAstThis().node;
			if(NodeUtils.isImport(n)){
				return false;
			}
		}
		return true;
	}

}
