package org.python.pydev.refactoring.codegenerator.generateproperties;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.refactoring.ast.adapters.PropertyTextAdapter;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;
import org.python.pydev.refactoring.ast.visitors.NodeHelper;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.GeneratePropertiesRequest;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.ui.model.generateproperties.TreeAttributeNode;
import org.python.pydev.refactoring.ui.model.generateproperties.TreeClassNode;
import org.python.pydev.refactoring.ui.model.tree.TreeNodeSimple;

public class GeneratePropertiesRequestProcessor implements IRequestProcessor<GeneratePropertiesRequest> {

	private Object[] checked;

	private int offsetMethodStrategy;

	private int offsetPropertyStrategy;

	private int accessModifier;

    private String endLineDelim;

	public GeneratePropertiesRequestProcessor(String endLineDelim) {
        this.endLineDelim = endLineDelim;
		checked = new Object[0];
		offsetMethodStrategy = IOffsetStrategy.AFTERINIT;
		offsetPropertyStrategy = IOffsetStrategy.END;
		accessModifier = NodeHelper.ACCESS_PUBLIC;
	}

	public void setCheckedElements(Object[] checked) {
		this.checked = checked;
	}

	private List<TreeAttributeNode> getAttributes() {
		List<TreeAttributeNode> attrs = new ArrayList<TreeAttributeNode>();

		for (Object elem : checked) {
			if (elem instanceof TreeAttributeNode) {
				attrs.add((TreeAttributeNode) elem);
			}
		}

		return attrs;
	}

	private List<PropertyTextAdapter> getProperties(TreeAttributeNode attr) {
		List<PropertyTextAdapter> props = new ArrayList<PropertyTextAdapter>();

		for (Object elem : checked) {
			if (elem instanceof TreeNodeSimple) {
				TreeNodeSimple propertyNode = (TreeNodeSimple) elem;
				if (propertyNode.getParent() == attr) {
					props.add((PropertyTextAdapter) propertyNode.getAdapter());
				}
			}
		}

		return props;
	}

	public List<GeneratePropertiesRequest> getRefactoringRequests() {
		List<GeneratePropertiesRequest> requests = generateRequests();

		return requests;
	}

	private List<GeneratePropertiesRequest> generateRequests() {
		List<GeneratePropertiesRequest> requests = new ArrayList<GeneratePropertiesRequest>();

		for (TreeAttributeNode elem : getAttributes()) {
			GeneratePropertiesRequest request = extractRequest(elem);
			if (request != null)
				requests.add(request);
		}

		return requests;
	}

	private GeneratePropertiesRequest extractRequest(TreeAttributeNode attr) {
		if (attr.getParent() != null && attr.getParent() instanceof TreeClassNode) {
			TreeClassNode classNode = (TreeClassNode) attr.getParent();

			return new GeneratePropertiesRequest(classNode.getAdapter(), attr.getAdapter(), getProperties(attr), offsetMethodStrategy,
					offsetPropertyStrategy, accessModifier, endLineDelim);
		}
		return null;
	}

	public void setMethodDestination(int strat) {
		this.offsetMethodStrategy = strat;
	}

	public void setPropertyDestination(int strat) {
		this.offsetPropertyStrategy = strat;
	}

	public void setAccessModifier(int accessModifier) {
		this.accessModifier = accessModifier;
	}
}
