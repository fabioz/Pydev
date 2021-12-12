/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 27, 2006
 */
package org.python.pydev.editor.actions;

import java.util.ResourceBundle;

import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.ResourceAction;
import org.python.pydev.editor.PyEdit;

/**
 * Reference: IncrementalFindAction extends ResourceAction implements IUpdate
 *
 * @author Fabio
 */
public class OfflineAction extends ResourceAction implements IUpdate {

    private OfflineActionTarget fTarget;
    private PyEdit edit;

    public OfflineAction(ResourceBundle bundle, String prefix, PyEdit edit) {
        super(bundle, prefix);
        this.edit = edit;
        update();
    }

    /*
     * @see IAction#run()
     */
    @Override
    public void run() {
        if (fTarget == null) {
            return;
        }

        fTarget.beginSession();
    }

    /*
     * @see IUpdate#update()
     */
    @Override
    public void update() {
        fTarget = (OfflineActionTarget) edit.getAdapter(OfflineActionTarget.class);
        setEnabled(fTarget != null);
    }

}
