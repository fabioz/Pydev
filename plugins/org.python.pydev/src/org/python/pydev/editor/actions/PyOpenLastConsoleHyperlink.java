package org.python.pydev.editor.actions;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.internal.console.ConsoleHyperlinkPosition;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsole;

public class PyOpenLastConsoleHyperlink implements IEditorActionDelegate {

    @Override
    public void run(IAction action) {
        for (IConsoleView c : ScriptConsole.iterConsoles()) {
            IConsole console = c.getConsole();
            if (console instanceof IOConsole) {
                IOConsole ioConsole = (IOConsole) console;
                processIOConsole(ioConsole);
                break;
            }
        }
    }

    @SuppressWarnings("restriction")
    private void processIOConsole(IOConsole ioConsole) {
        IDocument document = ioConsole.getDocument();
        try {
            Position[] positions = document.getPositions(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY);
            Arrays.sort(positions, new Comparator<Position>() {

                @Override
                public int compare(Position o1, Position o2) {
                    return Integer.compare(o1.getOffset(), o2.getOffset());
                }
            });
            if (positions.length > 0) {
                Position p = positions[positions.length - 1];
                if (p instanceof ConsoleHyperlinkPosition) {
                    ConsoleHyperlinkPosition consoleHyperlinkPosition = (ConsoleHyperlinkPosition) p;
                    IHyperlink hyperLink = consoleHyperlinkPosition.getHyperLink();
                    hyperLink.linkActivated();
                }
            }
        } catch (BadPositionCategoryException e) {
            Log.log(e);
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {

    }

    @Override
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {

    }

}
