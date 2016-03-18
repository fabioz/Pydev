/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.callbacks.ICallbackWithListeners;

public abstract class ViewPartWithOrientation extends ViewPart implements IPropertyChangeListener {

    protected Composite fParent;
    protected int fCurrentOrientation;
    private int orientationPreference;

    protected ViewPartWithOrientation() {
        String orientationPreferencesKey = getOrientationPreferencesKey();
        if (!SharedCorePlugin.inTestMode()) {
            IPreferenceStore preferenceStore = PydevPrefs.getPreferenceStore();
            orientationPreference = preferenceStore.getInt(orientationPreferencesKey);
            PydevPrefs.getPreferenceStore().addPropertyChangeListener(this);
        }
    }

    /*default*/static final int PREFERENCES_VIEW_ORIENTATION_AUTOMATIC = 0;
    /*default*/static final int PREFERENCES_VIEW_ORIENTATION_HORIZONTAL = 1;
    /*default*/static final int PREFERENCES_VIEW_ORIENTATION_VERTICAL = 2;

    //Subclasses will be passed these constants!
    public static final int VIEW_ORIENTATION_HORIZONTAL = 1;
    public static final int VIEW_ORIENTATION_VERTICAL = 2;

    @SuppressWarnings("rawtypes")
    public final ICallbackWithListeners onControlCreated = new CallbackWithListeners();

    @SuppressWarnings("rawtypes")
    public final ICallbackWithListeners onControlDisposed = new CallbackWithListeners();

    @Override
    public void createPartControl(Composite parent) {
        fParent = parent;
        addResizeListener(parent);
    }

    public abstract String getOrientationPreferencesKey();

    /*default*/int getOrientationPreferenceValue() {
        return orientationPreference;
    }

    protected void addOrientationPreferences(IMenuManager menuManager) {
        menuManager.add(new SetOrientationAction(this));
    }

    protected void updateOrientation() {
        switch (orientationPreference) {
            case PREFERENCES_VIEW_ORIENTATION_HORIZONTAL:
                setNewOrientation(VIEW_ORIENTATION_HORIZONTAL);
                break;

            case PREFERENCES_VIEW_ORIENTATION_VERTICAL:
                setNewOrientation(VIEW_ORIENTATION_VERTICAL);
                break;

            default:
                //automatic
                Point size = fParent.getSize();
                if (size.x != 0 && size.y != 0) {
                    if (size.x > size.y) {
                        setNewOrientation(VIEW_ORIENTATION_HORIZONTAL);
                    } else {
                        setNewOrientation(VIEW_ORIENTATION_VERTICAL);
                    }
                }
        }
    }

    private void addResizeListener(Composite parent) {
        parent.addControlListener(new ControlListener() {
            @Override
            public void controlMoved(ControlEvent e) {
            }

            @Override
            public void controlResized(ControlEvent e) {
                updateOrientation();
            }
        });
    }

    protected abstract void setNewOrientation(int orientation);

    @Override
    public void dispose() {
        PydevPrefs.getPreferenceStore().removePropertyChangeListener(this);
        super.dispose();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getProperty().equals(this.getOrientationPreferencesKey())) {
            orientationPreference = (Integer) event.getNewValue();
            updateOrientation();
        }
    }
}
