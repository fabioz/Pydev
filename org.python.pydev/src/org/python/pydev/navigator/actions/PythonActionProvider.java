/*
 * Created on Oct 9, 2006
 * @author Fabio
 */
package org.python.pydev.navigator.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.DeleteResourceAction;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

public class PythonActionProvider extends CommonActionProvider{
    
    private OpenPythonNodeAction openAction;
    //private DeleteResourceAction deleteResourceAction;

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        ICommonViewerSite viewSite = aSite.getViewSite();
        if(viewSite instanceof ICommonViewerWorkbenchSite){
            ICommonViewerWorkbenchSite site = (ICommonViewerWorkbenchSite) viewSite;
            openAction = new OpenPythonNodeAction(site.getPage(), site.getSelectionProvider());
            //deleteResourceAction = new DeleteResourceAction(site.getShell());
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
        //if(deleteResourceAction.isEnabled()){
        //    actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteResourceAction);
        //}
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
     */
    public void fillContextMenu(IMenuManager menu) {
        if(openAction.isEnabled()){
            menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openAction);        
        }
//        if(deleteResourceAction.isEnabled()){
//            menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, deleteResourceAction);        
//        }
    }

}
