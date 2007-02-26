package org.python.pydev.refactoring.ui.model.tree;

import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.ui.UITexts;

public interface ITreeNode {

	public static String NODE_CLASS = UITexts.imgClass;

	public static String NODE_METHOD = UITexts.imgMethod;

	public static String NODE_ATTRIBUTE = UITexts.imgAttribute;

	public String getImageName();

	public ITreeNode getParent();

	public String getLabel();

	public boolean hasChildren();

	public Object[] getChildren();

	public INodeAdapter getAdapter();
}
