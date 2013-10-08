/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.tooltips.presenter;

import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public interface IInformationPresenterControlManager {

    void hideInformationControl(boolean activateEditor, boolean restoreFocus);

    void install(Control control);

    void setInformationProvider(ITooltipInformationProvider provider);

    void showInformation();

    void setActivateEditorBinding(KeySequence activateEditorBinding);

    void setInitiallyActiveShell(Shell activeShell);
}
