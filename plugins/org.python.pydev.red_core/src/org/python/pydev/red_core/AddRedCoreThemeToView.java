/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.red_core;

import org.eclipse.swt.custom.StyleRange;
import org.python.pydev.core.callbacks.ICallbackListener;
import org.python.pydev.debug.codecoverage.CoverageCache;
import org.python.pydev.debug.codecoverage.PyCodeCoverageView;
import org.python.pydev.ui.IViewCreatedObserver;
import org.python.pydev.ui.IViewWithControls;

public class AddRedCoreThemeToView implements IViewCreatedObserver {

    private static boolean registeredForStyleOnCoverage = false;

    @SuppressWarnings("unchecked")
    public void notifyViewCreated(IViewWithControls view) {
        if (!AddRedCoreThemeAvailable.isRedCoreAvailableForTheming()) {
            return;
        }
        AddRedCoreThemeToViewCallbacks onViewCreatedListener = new AddRedCoreThemeToViewCallbacks();
        view.getOnControlCreated().registerListener(onViewCreatedListener.onControlCreated);
        view.getOnControlDisposed().registerListener(onViewCreatedListener.onControlDisposed);

        if (view instanceof PyCodeCoverageView) {
            if (!registeredForStyleOnCoverage) {
                //Only register once as it's static.
                registeredForStyleOnCoverage = true;
                final AddRedCorePreferences preferences = new AddRedCorePreferences();
                CoverageCache.onStyleCreated.registerListener(new ICallbackListener<StyleRange>() {

                    public Object call(StyleRange obj) {
                        obj.foreground = preferences.getHyperlinkTextAttribute().getForeground();
                        return null;
                    }
                });
            }
        }
    }
}
