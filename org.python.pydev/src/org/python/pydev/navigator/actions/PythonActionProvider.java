/*
 * Created on Oct 9, 2006
 * @author Fabio
 */
package org.python.pydev.navigator.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

public class PythonActionProvider extends CommonActionProvider{
    
    private OpenPythonNodeAction openAction;
    private PyOpenPythonFileAction openResourceAction;
    private PyDeleteResourceAction deleteResourceAction;
    private PyRenameResourceAction renameResourceAction;
    private PyCopyResourceAction copyResourceAction;
    private Clipboard clipboard;
    private PyPasteAction pasteAction;
    private PyMoveResourceAction moveResourceAction;

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        ICommonViewerSite viewSite = aSite.getViewSite();
        if(viewSite instanceof ICommonViewerWorkbenchSite){
            ICommonViewerWorkbenchSite site = (ICommonViewerWorkbenchSite) viewSite;
            Shell shell = site.getShell();
            
            ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
            clipboard = new Clipboard(shell.getDisplay());
            openAction = new OpenPythonNodeAction(site.getPage(), site.getSelectionProvider());
            openResourceAction = new PyOpenPythonFileAction(site.getPage(), site.getSelectionProvider());
            
            deleteResourceAction = new PyDeleteResourceAction(shell, site.getSelectionProvider());
            renameResourceAction = new PyRenameResourceAction(shell, site.getSelectionProvider());
            copyResourceAction = new PyCopyResourceAction(shell, site.getSelectionProvider(), clipboard);
            pasteAction = new PyPasteAction(shell, site.getSelectionProvider(), clipboard);
            moveResourceAction = new PyMoveResourceAction(shell, site.getSelectionProvider());
            
            copyResourceAction.setDisabledImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
            copyResourceAction.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
            
            pasteAction.setDisabledImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED));
            pasteAction.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
            
            deleteResourceAction.setDisabledImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE_DISABLED));
            deleteResourceAction.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));

        }
    }
    
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.actions.ActionGroup#fillActionBars(org.eclipse.ui.IActionBars)
     */
    public void fillActionBars(IActionBars actionBars) { 
        /* Set up the property open action when enabled. */
        if(openAction.isEnabled()){
            actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, openAction);
        }
        if(openResourceAction.isEnabled()){
            actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, openResourceAction);
        }
        if(copyResourceAction.isEnabled()){
            actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyResourceAction);
        }
        if(pasteAction.isEnabled()){
            actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), pasteAction);
        }
        if(deleteResourceAction.isEnabled()){
            actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteResourceAction);
        }
        if(moveResourceAction.isEnabled()){
            actionBars.setGlobalActionHandler(ActionFactory.MOVE.getId(), moveResourceAction);
        }
        if(renameResourceAction.isEnabled()){
            actionBars.setGlobalActionHandler(ActionFactory.RENAME.getId(), renameResourceAction);
        }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
     */
    public void fillContextMenu(IMenuManager menu) {
        if(openAction.isEnabled()){
            menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openAction);        
        }
//        if(openResourceAction.isEnabled()){
//            menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openResourceAction);        
//        }
        if(copyResourceAction.isEnabled()){
            menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, copyResourceAction);        
        }
        if(pasteAction.isEnabled()){
            menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, pasteAction);        
        }
        if(deleteResourceAction.isEnabled()){
            menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, deleteResourceAction);        
        }
        if(moveResourceAction.isEnabled()){
            menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, moveResourceAction);        
        }
        if(renameResourceAction.isEnabled()){
            menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, renameResourceAction);        
        }
    }

}
