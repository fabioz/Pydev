package org.python.pydev.refactoring.ui.model.generateproperties;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.refactoring.ast.adapters.INodeAdapter;
import org.python.pydev.refactoring.ast.adapters.PropertyTextAdapter;
import org.python.pydev.refactoring.ui.UITexts;
import org.python.pydev.refactoring.ui.model.tree.ITreeNode;
import org.python.pydev.refactoring.ui.model.tree.TreeNodeSimple;

public class TreeAttributeNode extends TreeNodeSimple<INodeAdapter> {

	public TreeAttributeNode(ITreeNode parent, INodeAdapter adapter) {
		super(parent, adapter);
	}

	@Override
	public Object[] getChildren() {
		List<ITreeNode> children = new ArrayList<ITreeNode>();
		children.add(new TreeNodeSimple<PropertyTextAdapter>(this,
				new PropertyTextAdapter(PropertyTextAdapter.GETTER,
						UITexts.generatePropertiesGetter)));
		children.add(new TreeNodeSimple<PropertyTextAdapter>(this,
				new PropertyTextAdapter(PropertyTextAdapter.SETTER,
						UITexts.generatePropertiesSetter)));
		children.add(new TreeNodeSimple<PropertyTextAdapter>(this,
				new PropertyTextAdapter(PropertyTextAdapter.DELETE,
						UITexts.generatePropertiesDelete)));
		children.add(new TreeNodeSimple<PropertyTextAdapter>(this,
				new PropertyTextAdapter(PropertyTextAdapter.DOCSTRING,
						UITexts.generatePropertiesDocString)));
		return children.toArray();
	}

	@Override
	public String getImageName() {
		return ITreeNode.NODE_ATTRIBUTE;
	}

}
