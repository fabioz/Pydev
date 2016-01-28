package org.python.pydev.editor.hover;

import java.io.IOException;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.docutils.PyStringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.PyInformationPresenter;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterPrefsV2;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterV2;
import org.python.pydev.parser.visitors.NodeUtils;

public abstract class AbstractPyEditorTextHover implements ITextHover, ITextHoverExtension {

    /**
     * The text selected
     */
    private ITextSelection textSelection;

    private final class PyInformationControl extends DefaultInformationControl
            implements IInformationControlExtension3 {
        private PyInformationControl(Shell parent, int textStyles, IInformationPresenter presenter,
                String statusFieldText) {
            super(parent, textStyles, presenter, statusFieldText);
        }

    }

    public abstract boolean isContentTypeSupported(String contentType);

    /*
     * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
     */
    public IInformationControlCreator getHoverControlCreator() {
        return new IInformationControlCreator() {
            public IInformationControl createInformationControl(Shell parent) {
                String tooltipAffordanceString = null;
                try {
                    tooltipAffordanceString = EditorsUI.getTooltipAffordanceString();
                } catch (Throwable e) {
                    //Not available on Eclipse 3.2
                }
                DefaultInformationControl ret = new PyInformationControl(parent, SWT.NONE,
                        new PyInformationPresenter(), tooltipAffordanceString);
                return ret;
            }
        };
    }

    /*
     * @see org.eclipse.jface.text.ITextHover#getHoverRegion(org.eclipse.jface.text.ITextViewer, int)
     */
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        //we have to set it here (otherwise we don't have thread access to the UI)
        this.textSelection = (ITextSelection) textViewer.getSelectionProvider().getSelection();
        return new Region(offset, 0);
    }

    public static String printAst(PyEdit edit, SimpleNode astToPrint) {
        String str = null;
        if (astToPrint != null) {
            IIndentPrefs indentPrefs;
            if (edit != null) {
                indentPrefs = edit.getIndentPrefs();
            } else {
                indentPrefs = DefaultIndentPrefs.get(null);
            }

            Str docStr = NodeUtils.getNodeDocStringNode(astToPrint);
            if (docStr != null) {
                docStr.s = PyStringUtils.fixWhitespaceColumnsToLeftFromDocstring(docStr.s,
                        indentPrefs.getIndentationString());
            }

            PrettyPrinterPrefsV2 prefsV2 = PrettyPrinterV2.createDefaultPrefs(edit, indentPrefs,
                    PyInformationPresenter.LINE_DELIM);

            PrettyPrinterV2 prettyPrinterV2 = new PrettyPrinterV2(prefsV2);
            try {

                str = prettyPrinterV2.print(astToPrint);
            } catch (IOException e) {
                Log.log(e);
            }
        }
        return str;
    }
}
