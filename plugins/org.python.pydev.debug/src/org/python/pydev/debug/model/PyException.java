package org.python.pydev.debug.model;

import java.util.ArrayList;
import java.util.List;

public class PyException {

	PyException parent;
	String name;
	int selected;
	List<PyException> subClassList = new ArrayList<PyException>();

	public PyException getParent() {
		return parent;
	}

	public void setParent(PyException parent) {
		this.parent = parent;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSelected() {
		return selected;
	}

	public void setSelected(int selected) {
		this.selected = selected;
	}

	public List<PyException> getSubClassList() {
		return subClassList;
	}

	public void setSubClassList(List<PyException> subClassList) {
		this.subClassList = subClassList;
	}

	public String toString() {
		if (this.parent != null) {
			return "Class Name: " + this.name + "; Parent: " + this.parent.name;
		} else {
			return "Class Name: " + this.name + "; Parent: null";
		}
	}

}
