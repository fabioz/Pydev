package org.python.pydev.core.tooltips.presenter;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension3;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class InformationPresenterHelpers {

    public static class TooltipInformationControlCreator implements IInformationControlCreator {

        private IInformationPresenter presenter;

        public TooltipInformationControlCreator(IInformationPresenter presenter) {
            if(presenter == null){
                presenter = new AbstractTooltipInformationPresenter(){

                    protected void onUpdatePresentation(String hoverInfo, TextPresentation presentation) {
                    }

                    protected void onHandleClick(Object data) {
                        
                    }
                };
            }
            this.presenter = presenter;
        }
        
        public IInformationControl createInformationControl(Shell parent) {
            String tooltipAffordanceString = null;
//            try { -- this would show the 'F2' for focus, but we don't actually handle that, so, don't use it.
//                tooltipAffordanceString = EditorsUI.getTooltipAffordanceString();
//            } catch (Throwable e) {
//                //Not available on Eclipse 3.2
//            }

            if (parent == null) {
                parent = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            }

            PyInformationControl tooltip = new PyInformationControl(parent, tooltipAffordanceString, presenter);
            return tooltip;
        }
    }

    public final static class PyInformationControl extends DefaultInformationControl implements IInformationControlExtension3 {
        public PyInformationControl(Shell parent, String statusFieldText, IInformationPresenter presenter) {
            super(parent, statusFieldText, presenter);
        }
    }

}
