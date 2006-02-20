package com.python.pydev.view.filters;

import org.eclipse.jface.viewers.ViewerFilter;

public abstract class AbstractViewerFilter extends ViewerFilter {
	protected String name;
	protected String id;// = "com.python.pydev.browsing.view.PydevPackageExplorer";
	protected String viewerId = "Package Explorer View";
	protected String description;
	protected boolean initiallyEnabled;

	public String getViewerId() {
		return viewerId;
	}

	public void setViewerId(String viewerId) {
		this.viewerId = viewerId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isInitiallyEnabled() {
		return initiallyEnabled;
	}

	public void setInitiallyEnabled(boolean initiallyEnabled) {
		this.initiallyEnabled = initiallyEnabled;
	}
	
}
