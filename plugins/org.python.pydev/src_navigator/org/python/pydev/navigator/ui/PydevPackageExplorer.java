/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 29, 2006
 * @author Fabio
 */
package org.python.pydev.navigator.ui;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.AggregateWorkingSet;
import org.eclipse.ui.internal.navigator.ContributorTrackingSet;
import org.eclipse.ui.internal.navigator.NavigatorContentService;
import org.eclipse.ui.internal.navigator.dnd.CommonDropAdapterDescriptor;
import org.eclipse.ui.internal.navigator.dnd.CommonDropDescriptorManager;
import org.eclipse.ui.internal.navigator.dnd.NavigatorDnDService;
import org.eclipse.ui.navigator.CommonDragAdapter;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.INavigatorContentService;
import org.eclipse.ui.navigator.INavigatorDnDService;
import org.eclipse.ui.navigator.INavigatorPipelineService;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.python.pydev.core.log.Log;
import org.python.pydev.navigator.LabelAndImage;
import org.python.pydev.navigator.actions.PythonLinkHelper;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.callbacks.ICallbackWithListeners;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_ui.editor_input.PydevZipFileEditorInput;
import org.python.pydev.shared_ui.editor_input.PydevZipFileStorage;
import org.python.pydev.shared_ui.utils.IViewWithControls;
import org.python.pydev.ui.NotifyViewCreated;

/**
 * This class is the package explorer for pydev. It uses the CNF (Common Navigator Framework) to show
 * the resources as python elements.
 */
@SuppressWarnings({ "restriction", "rawtypes", "unchecked" })
public class PydevPackageExplorer extends CommonNavigator implements IShowInTarget, IViewWithControls {

    public static class PydevNavigatorContentService extends NavigatorContentService {

        private INavigatorDnDService pyNavigatorDnDService;

        public PydevNavigatorContentService(String aViewerId, StructuredViewer aViewer) {
            super(aViewerId, aViewer);
        }

        @Override
        public INavigatorDnDService getDnDService() {
            if (pyNavigatorDnDService == null) {
                pyNavigatorDnDService = new PydevNavigatorDnDService(this);
            }
            return pyNavigatorDnDService;
        }

    }

    public static class PydevNavigatorDnDService extends NavigatorDnDService {

        private static final CommonDropAdapterAssistant[] NO_ASSISTANTS = new CommonDropAdapterAssistant[0];

        private INavigatorContentService pyContentService;

        private CommonDropAdapter pyDropAdapter;

        private final Map pyDropAssistants = new HashMap();

        public PydevNavigatorDnDService(INavigatorContentService aContentService) {
            super(aContentService);
            pyContentService = aContentService;
        }

        @Override
        public void setDropAdaptor(CommonDropAdapter da) {
            super.setDropAdaptor(da);
            pyDropAdapter = da;
        }

        // This method performs the ultimate goal of choosing PyDev drop assistants over others,
        // to ensure that PYTHONPATH updates happen.
        private CommonDropAdapterAssistant[] pySortAssistants(CommonDropAdapterAssistant[] array) {
            Arrays.sort(array, new Comparator() {
                public int compare(Object arg0, Object arg1) {
                    CommonDropAdapterAssistant a = (CommonDropAdapterAssistant) arg0;
                    CommonDropAdapterAssistant b = (CommonDropAdapterAssistant) arg1;
                    // This is to ensure that the PyDev drop assistant will always
                    // be chosen over non-PyDev ones, if a conflict ever occurs.
                    String id = "org.python.pydev.navigator.actions"; //$NON-NLS-1$
                    if (a.getClass().getName().startsWith(id)) {
                        return -1;
                    }
                    if (b.getClass().getName().startsWith(id)) {
                        return 1;
                    }
                    return a.getClass().getName().compareTo(b.getClass().getName());
                }
            });
            return array;
        }

        // These methods are here just so pySortAssistants can function properly. They are more
        // or less the same as the methods they override (or in the case of private methods, mimic).
        @Override
        public CommonDropAdapterAssistant[] findCommonDropAdapterAssistants(
                Object aDropTarget, TransferData aTransferType) {

            CommonDropAdapterDescriptor[] descriptors = CommonDropDescriptorManager
                    .getInstance().findCommonDropAdapterAssistants(aDropTarget,
                            pyContentService);

            if (descriptors.length == 0) {
                return NO_ASSISTANTS;
            }

            if (LocalSelectionTransfer.getTransfer().isSupportedType(aTransferType)
                    && LocalSelectionTransfer.getTransfer().getSelection() instanceof IStructuredSelection) {
                return pyGetAssistantsBySelection(descriptors, (IStructuredSelection) LocalSelectionTransfer
                        .getTransfer().getSelection());
            }
            return pyGetAssistantsByTransferData(descriptors, aTransferType);
        }

        @Override
        public CommonDropAdapterAssistant[] findCommonDropAdapterAssistants(
                Object aDropTarget, IStructuredSelection theDragSelection) {

            CommonDropAdapterDescriptor[] descriptors = CommonDropDescriptorManager
                    .getInstance().findCommonDropAdapterAssistants(aDropTarget,
                            pyContentService);

            if (descriptors.length == 0) {
                return NO_ASSISTANTS;
            }

            return pyGetAssistantsBySelection(descriptors, theDragSelection);
        }

        private CommonDropAdapterAssistant[] pyGetAssistantsByTransferData(
                CommonDropAdapterDescriptor[] descriptors,
                TransferData aTransferType) {

            Set assistants = new LinkedHashSet();
            for (int i = 0; i < descriptors.length; i++) {
                CommonDropAdapterAssistant asst = pyGetAssistant(descriptors[i]);
                if (asst.isSupportedType(aTransferType)) {
                    assistants.add(asst);
                }
            }
            return pySortAssistants((CommonDropAdapterAssistant[]) assistants
                    .toArray(new CommonDropAdapterAssistant[assistants.size()]));

        }

        private CommonDropAdapterAssistant[] pyGetAssistantsBySelection(
                CommonDropAdapterDescriptor[] descriptors, IStructuredSelection aSelection) {

            Set assistants = new LinkedHashSet();

            for (int i = 0; i < descriptors.length; i++) {
                if (descriptors[i].areDragElementsSupported(aSelection)) {
                    assistants.add(pyGetAssistant(descriptors[i]));
                }
            }

            return pySortAssistants((CommonDropAdapterAssistant[]) assistants
                    .toArray(new CommonDropAdapterAssistant[assistants.size()]));
        }

        private CommonDropAdapterAssistant pyGetAssistant(
                CommonDropAdapterDescriptor descriptor) {
            CommonDropAdapterAssistant asst = (CommonDropAdapterAssistant) pyDropAssistants
                    .get(descriptor);
            if (asst != null) {
                return asst;
            }
            synchronized (pyDropAssistants) {
                asst = (CommonDropAdapterAssistant) pyDropAssistants.get(descriptor);
                if (asst == null) {
                    pyDropAssistants.put(descriptor, (asst = descriptor
                            .createDropAssistant()));
                    asst.init(pyContentService);
                    asst.setCommonDropAdapter(pyDropAdapter);
                }
            }
            return asst;
        }

    }

    /**
     * This viewer is the one used instead of the common viewer -- should only be used to fix failures in the base class.
     */
    public static class PydevCommonViewer extends CommonViewer {

        /**
         * This is used so that we only restore the memento in the 'right' place
         */
        public boolean availableToRestoreMemento = false;

        /**
         * This is the pydev package explorer
         */
        private PydevPackageExplorer pydevPackageExplorer;

        public PydevPackageExplorer getPydevPackageExplorer() {
            return pydevPackageExplorer;
        }

        private PydevNavigatorContentService pyContentService;

        public PydevCommonViewer(String id, Composite parent, int style, PydevPackageExplorer pydevPackageExplorer) {
            super(id, parent, style);
            this.pydevPackageExplorer = pydevPackageExplorer;

            //We need to be able to compare actual resources and IWrappedResources
            //as if they were the same thing.
            setComparer(new IElementComparer() {

                public int hashCode(Object element) {
                    if (element instanceof IWrappedResource) {
                        IWrappedResource wrappedResource = (IWrappedResource) element;
                        return wrappedResource.getActualObject().hashCode();
                    }
                    return element.hashCode();
                }

                public boolean equals(Object a, Object b) {
                    if (a instanceof IWrappedResource) {
                        IWrappedResource wrappedResource = (IWrappedResource) a;
                        a = wrappedResource.getActualObject();
                    }
                    if (b instanceof IWrappedResource) {
                        IWrappedResource wrappedResource = (IWrappedResource) b;
                        b = wrappedResource.getActualObject();
                    }
                    if (a == null) {
                        if (b == null) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                    if (b == null) {
                        return false;
                    }

                    return a.equals(b);
                }
            });
        }

        @Override
        protected void init() {
            pyContentService = new PydevNavigatorContentService("org.python.pydev.navigator.view", this);
            super.init();
        }

        @Override
        protected void initDragAndDrop() {
            int operations = DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK;

            CommonDragAdapter dragAdapter = createDragAdapter();
            addDragSupport(operations, dragAdapter.getSupportedDragTransfers(),
                    dragAdapter);

            CommonDropAdapter dropAdapter = createDropAdapter();
            addDropSupport(operations, dropAdapter.getSupportedDropTransfers(),
                    dropAdapter);

            // Set the drop adaptor of the PyDev content service instead of the standard one,
            // which shouldn't be used for drop policies.
            NavigatorDnDService dnd = (NavigatorDnDService) pyContentService.getDnDService();
            dnd.setDropAdaptor(dropAdapter);
        }

        // The only thing that needs the new content service is the drop adapter, because it
        // sets the DnDService.
        @Override
        protected CommonDropAdapter createDropAdapter() {
            return new CommonDropAdapter(pyContentService, this);
        }

        @Override
        public void dispose() {
            if (pyContentService != null) {
                pyContentService.dispose();
            }
            super.dispose();
        }

        /**
         * Returns the tree path for the given item.
         * 
         * It's overridden because when using mylyn, the paths may be expanded but not shown, so segment is null
         * -- that's why we return null if a given segment is null (instead of the assert that it contains in the superclass) 
         * @since 3.2
         */
        @Override
        protected TreePath getTreePathFromItem(Item item) {
            LinkedList<Object> segments = new LinkedListWarningOnSlowOperations<Object>();
            while (item != null) {
                Object segment = item.getData();
                if (segment == null) {
                    return null;
                }
                segments.addFirst(segment);
                item = getParentItem(item);
            }
            return new TreePath(segments.toArray());
        }

        public IWorkingSet[] getSelectedWorkingSets() {
            Object input = getInput();
            if (input instanceof AggregateWorkingSet) {
                return ((AggregateWorkingSet) input).getComponents();
            }
            return null;
        }
    }

    /**
     * This is the memento to be used.
     */
    private IMemento memento;
    public final ICallbackWithListeners onControlCreated = new CallbackWithListeners();
    public final ICallbackWithListeners onControlDisposed = new CallbackWithListeners();
    private PydevCommonViewer viewer;
    private final PythonLinkHelper pythonLinkHelper = new PythonLinkHelper();

    public PydevPackageExplorer() {
        super();
        NotifyViewCreated.notifyViewCreated(this);
    }

    /**
     * Overridden to keep the memento to be used later (it's private in the superclass).
     */
    @Override
    public void init(IViewSite aSite, IMemento aMemento) throws PartInitException {
        super.init(aSite, aMemento);
        memento = aMemento;
    }

    /**
     * Overridden to create our viewer and not the superclass CommonViewer.
     * 
     * (Unfortunately, the superclass does a little more than creating it, so, we have to do those operations here 
     * too -- that's why we have to keep the memento object in the init method).
     */
    @Override
    protected CommonViewer createCommonViewer(Composite aParent) {
        //super.createCommonViewer(aParent); -- don't even call the super class
        CommonViewer aViewer = new PydevCommonViewer(getViewSite().getId(), aParent, SWT.MULTI | SWT.H_SCROLL
                | SWT.V_SCROLL, this);
        initListeners(aViewer);

        //commented: we do that only after the part is completely created (because otherwise the state is reverted later)
        //aViewer.getNavigatorContentService().restoreState(memento);

        return aViewer;
    }

    /**
     * Overridden because if the state is not restored as the last thing, it is reverted back to the previous state.
     */
    @Override
    public void createPartControl(Composite aParent) {
        super.createPartControl(aParent);
        PydevCommonViewer viewer = (PydevCommonViewer) getCommonViewer();
        this.viewer = viewer;
        onControlCreated.call(viewer);

        viewer.availableToRestoreMemento = true;
        for (int i = 0; i < 3; i++) {
            try {
                //I don't know why the 1st time we restore it it doesn't work... so, we have to do it twice 
                //(and the other 1 is because we may have an exception in the 1st step).
                viewer.getNavigatorContentService().restoreState(memento);
            } catch (Exception e1) {
                if (i > 1) {
                    Log.log("Unable to restore the state of the Pydev Package Explorer.", e1);
                }
            }
        }
    }

    @Override
    public void dispose() {
        if (viewer != null) {
            onControlDisposed.call(viewer);
            viewer = null;
        }
        super.dispose();
    };

    /**
     * Returns the element contained in the EditorInput
     */
    Object getElementOfInput(IEditorInput input) {
        if (input instanceof IFileEditorInput) {
            return ((IFileEditorInput) input).getFile();
        }
        if (input instanceof IURIEditorInput) {
            IURIEditorInput iuriEditorInput = (IURIEditorInput) input;
            URI uri = iuriEditorInput.getURI();
            return new File(uri);

        }
        if (input instanceof PydevZipFileEditorInput) {
            PydevZipFileEditorInput pydevZipFileEditorInput = (PydevZipFileEditorInput) input;
            try {
                IStorage storage = pydevZipFileEditorInput.getStorage();
                if (storage instanceof PydevZipFileStorage) {
                    PydevZipFileStorage pydevZipFileStorage = (PydevZipFileStorage) storage;
                    return pydevZipFileStorage;
                }
            } catch (CoreException e) {
                Log.log(e);
            }

        }
        return null;
    }

    /**
     * Implements the 'show in...' action
     */
    @Override
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

    /**
     * This is the method that actually tries to reveal some item in the tree.
     * 
     * It will go through the pipeline to see if the actual object to reveal has been replaced in the replace pipeline.
     */
    public boolean tryToReveal(Object element) {
        element = getPythonModelElement(element);

        if (element instanceof PydevZipFileStorage) {
            pythonLinkHelper.setCommonViewer(this.getCommonViewer());
            PydevZipFileStorage pydevZipFileStorage = (PydevZipFileStorage) element;

            IStructuredSelection externalFileSelectionInTree = pythonLinkHelper
                    .findExternalFileSelection(pydevZipFileStorage.zipFile);
            if (externalFileSelectionInTree != null && !externalFileSelectionInTree.isEmpty()) {
                Object firstElement = externalFileSelectionInTree.getFirstElement();
                if (firstElement instanceof TreeNode) {
                    TreeNode treeNode = (TreeNode) firstElement;
                    //Ok, got to the zip file, let's try to find the path below it...
                    String zipPath = pydevZipFileStorage.zipPath;
                    List<String> split = StringUtils.split(zipPath, '/');
                    for (String string : split) {
                        List<TreeNode> children = treeNode.getChildren();
                        for (TreeNode<LabelAndImage> child : children) {
                            if (string.equals(child.getData().label)) {
                                treeNode = child;
                                break; //Goes on to the next substring...
                            }
                        }
                    }

                    if (revealAndVerify(new StructuredSelection(treeNode))) {
                        return true;
                    }
                } else {
                    Log.log("Expected a TreeNode. Found: " + firstElement);
                    //Just go on to show the zip, not the internal contents...
                    if (revealAndVerify(externalFileSelectionInTree)) {
                        return true;
                    }
                }
            }

        } else if (element instanceof File) {
            pythonLinkHelper.setCommonViewer(this.getCommonViewer());

            IStructuredSelection externalFileSelectionInTree = pythonLinkHelper
                    .findExternalFileSelection((File) element);
            if (externalFileSelectionInTree != null && !externalFileSelectionInTree.isEmpty()) {
                if (revealAndVerify(externalFileSelectionInTree)) {
                    return true;
                }
            }
        }

        //null is checked in the revealAndVerify function
        if (revealAndVerify(element)) {
            return true;
        }

        //if it is a wrapped resource that we couldn't show, try to reveal as a resource...
        if (element instanceof IAdaptable && !(element instanceof IResource)) {
            IAdaptable adaptable = (IAdaptable) element;
            IResource resource = adaptable.getAdapter(IResource.class);
            if (resource != null) {
                if (revealAndVerify(resource)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param element the element that should be gotten as an element from the pydev model
     * @return a pydev element or the same element passed as a parameter.
     */
    private Object getPythonModelElement(Object element) {
        if (element instanceof IWrappedResource) {
            return element;
        }
        INavigatorPipelineService pipelineService = this.getNavigatorContentService().getPipelineService();
        if (element instanceof IAdaptable) {
            IAdaptable adaptable = (IAdaptable) element;
            IFile file = adaptable.getAdapter(IFile.class);
            if (file != null) {
                HashSet<Object> files = new ContributorTrackingSet(
                        (NavigatorContentService) this.getNavigatorContentService());
                files.add(file);
                pipelineService.interceptAdd(new PipelinedShapeModification(file.getParent(), files));
                if (files.size() > 0) {
                    element = files.iterator().next();
                }
            }
        }
        return element;
    }

    /**
     * Tries to reveal some selection
     * @return if it revealed the selection correctly (and false otherwise)
     */
    private boolean revealAndVerify(Object element) {
        if (element == null) {
            return false;
        }
        if (element instanceof ISelection) {
            selectReveal((ISelection) element);

        } else {
            selectReveal(new StructuredSelection(element));
        }
        return !getSite().getSelectionProvider().getSelection().isEmpty();
    }

    public ICallbackWithListeners getOnControlCreated() {
        return onControlCreated;
    }

    public ICallbackWithListeners getOnControlDisposed() {
        return onControlDisposed;
    }

}
