package org.python.pydev.shared_ui.field_editors;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.dialogs.ProjectSelectionDialog;

public class ProjectDirectoryFieldEditorCustom extends DirectoryFieldEditorCustom {

    public ProjectDirectoryFieldEditorCustom(String name, String labelText, Composite parent) {
        super(name, labelText, parent);
    }

    private Composite newParent;
    private Button projectButton;

    @Override
    protected Button getChangeControl(Composite p) {
        if (newParent == null) {
            newParent = new Composite(p, SWT.None);
            newParent.setLayout(new GridLayout(2, true));
            if (projectButton == null) {
                projectButton = new Button(newParent, SWT.PUSH);
                projectButton
                        .setImage(ImageCache.asImage(SharedCorePlugin.getImageCache().get(UIConstants.PROJECT_ICON)));
                projectButton.addSelectionListener(widgetSelectedAdapter(evt -> {
                    ProjectSelectionDialog dialog = new ProjectSelectionDialog(EditorUtils.getShell(), null);
                    dialog.setMessage("Choose project.\n");
                    if (dialog.open() == Window.OK) {
                        Object[] result = dialog.getResult();
                        if (result != null && result.length == 1) {
                            setStringValue(((IProject) result[0]).getLocation().toOSString());
                        }
                    }
                }));
                projectButton.addDisposeListener(event -> projectButton = null);
            }
        }
        return super.getChangeControl(newParent);
    }

}
