package org.python.pydev.debug.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.IVariableLocator;
import org.python.pydev.debug.model.remote.RunCustomOperationCommand;
import org.python.pydev.debug.referrers.ReferrersView;
import org.python.pydev.shared_core.structure.Tuple;

public class GetReferrersCommandHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        Tuple<AbstractDebugTarget, IVariableLocator> context = RunCustomOperationCommand
                .extractContextFromSelection(selection);
        if (context != null) {
            ReferrersView view = ReferrersView.getView(true);
            if (view != null) {
                view.showReferrersFor(context.o1, context.o2);
            } else {
                Log.log("Could not find ReferrersView.");
            }
        }

        return null;
    }
}