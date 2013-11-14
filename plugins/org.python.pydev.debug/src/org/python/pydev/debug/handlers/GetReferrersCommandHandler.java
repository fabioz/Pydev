package org.python.pydev.debug.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;
import org.python.pydev.debug.model.remote.RunCustomOperationCommand;
import org.python.pydev.debug.referrers.ReferrersView;
import org.python.pydev.shared_core.structure.Tuple;

public class GetReferrersCommandHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        Tuple<AbstractDebugTarget, String> context = RunCustomOperationCommand.extractContextFromSelection(selection);
        if (context != null) {
            RunCustomOperationCommand cmd = new RunCustomOperationCommand(context.o1, context.o2,
                    "from pydevd_referrers import print_referrers",
                    "print_referrers");
            cmd.setCompletionListener(new ICommandResponseListener() {

                @Override
                public void commandComplete(AbstractDebuggerCommand cmd) {
                    if (cmd instanceof RunCustomOperationCommand) {
                        RunCustomOperationCommand c = (RunCustomOperationCommand) cmd;
                        System.out.println("Received: " + c.getResponsePayload());
                    }
                }
            });
            context.o1.postCommand(cmd);

            ReferrersView view = ReferrersView.getView(true);

        }

        return null;
    }
}