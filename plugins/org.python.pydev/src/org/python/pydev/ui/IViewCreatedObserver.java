package org.python.pydev.ui;

public interface IViewCreatedObserver {

	/**
	 * Called for:
	 * - PydevPackageExplorer
	 * - PyOutlinePage  
	 */
	void notifyViewCreated(Object view);

}
