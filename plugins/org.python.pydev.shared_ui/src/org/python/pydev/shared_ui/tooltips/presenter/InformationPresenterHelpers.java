/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.tooltips.presenter;

import java.lang.ref.WeakReference;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension3;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;

public class InformationPresenterHelpers {

    public static class TooltipInformationControlCreator implements IInformationControlCreator {

        private IInformationPresenter presenter;

        private WeakReference<InformationPresenterControlManager> informationPresenterControlManager;

        public void setInformationPresenterControlManager(
                InformationPresenterControlManager informationPresenterControlManager) {
            this.informationPresenterControlManager = new WeakReference<InformationPresenterControlManager>(
                    informationPresenterControlManager);
        }

        public TooltipInformationControlCreator(IInformationPresenter presenter) {
            if (presenter == null) {
                presenter = new AbstractTooltipInformationPresenter() {

                    @Override
                    protected void onUpdatePresentation(String hoverInfo, TextPresentation presentation) {
                    }

                    @Override
                    protected void onHandleClick(Object data) {

                    }
                };
            }
            this.presenter = presenter;
        }

        @Override
        public IInformationControl createInformationControl(Shell parent) {

            //            try { -- this would show the 'F2' for focus, but we don't actually handle that, so, don't use it.
            //                tooltipAffordanceString = EditorsUI.getTooltipAffordanceString();
            //            } catch (Throwable e) {
            //                //Not available on Eclipse 3.2
            //            }

            //Note: don't use the parent because when it's closed we don't want the parent to have focus (we want the original
            //widget that had focus to regain the focus).
            //            if (parent == null) {
            //                parent = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            //            }

            String tooltipAffordanceString = null;
            if (this.informationPresenterControlManager != null) {
                InformationPresenterControlManager m = this.informationPresenterControlManager.get();
                if (m != null) {
                    tooltipAffordanceString = m.getTooltipAffordanceString();
                }
            }
            PyInformationControl tooltip = new PyInformationControl(null, tooltipAffordanceString, presenter);
            return tooltip;
        }

    }

    public final static class PyInformationControl extends DefaultInformationControl implements
            IInformationControlExtension3 {
        public PyInformationControl(Shell parent, String statusFieldText, IInformationPresenter presenter) {
            super(parent, statusFieldText, presenter);
        }

        public Rectangle getShellTooltipBounds() {
            return getShell().getBounds();
        }
    }

}
