/*
 * Created on Oct 29, 2006
 * @author Fabio
 */
package org.python.pydev.navigator.ui;

import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.internal.navigator.ContributorTrackingSet;
import org.eclipse.ui.internal.navigator.NavigatorContentService;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.INavigatorPipelineService;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;

public class PydevPackageExplorer extends CommonNavigator implements IShowInTarget {
	/**
	 * Returns the element contained in the EditorInput
	 */
	Object getElementOfInput(IEditorInput input) {
		if (input instanceof IFileEditorInput) {
			return ((IFileEditorInput) input).getFile();
		}
		return null;
	}

	/**
	 * Implements the 'show in...' action
	 */
	public boolean show(ShowInContext context) {
		Object elementOfInput = null;
		ISelection selection = context.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = ((IStructuredSelection) selection);
			if (structuredSelection.size() == 1) {
				elementOfInput = structuredSelection.getFirstElement();
			}
		}

		Object input = context.getInput();
		if (input instanceof IEditorInput) {
			elementOfInput = getElementOfInput((IEditorInput) context.getInput());
		}

		return elementOfInput != null && tryToReveal(elementOfInput);
	}
	
	public boolean tryToReveal(Object element) {
		INavigatorPipelineService pipelineService = this.getNavigatorContentService().getPipelineService();
		if(element instanceof IAdaptable){
			IAdaptable adaptable = (IAdaptable) element;
			IFile file = (IFile) adaptable.getAdapter(IFile.class);
			if(file != null){
				HashSet<Object> files = new ContributorTrackingSet((NavigatorContentService) this.getNavigatorContentService());
				files.add(file);
				pipelineService.interceptAdd(new PipelinedShapeModification(file.getParent(), files));
				if(files.size() > 0){
					element = files.iterator().next();
				}
			}
		}
		
		if (revealAndVerify(element)) {
			return true;
		}

		if (element != null) {
			if (revealAndVerify(element)) {
				return true;
			}

			if (element instanceof IAdaptable) {
				IAdaptable adaptable = (IAdaptable) element;
				IResource resource = (IResource) adaptable.getAdapter(IResource.class);
				if (resource != null) {
					if (revealAndVerify(resource)){
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean revealAndVerify(Object element) {
		if (element == null){
			return false;
		}
		
		selectReveal(new StructuredSelection(element));
		return !getSite().getSelectionProvider().getSelection().isEmpty();
	}

}
