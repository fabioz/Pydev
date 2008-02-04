package org.python.pydev.editor.hyperlink;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.visitors.PythonLanguageUtils;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Based on JavaElementHyperlinkDetector (which uses the hyperlink mechanism added at eclipse 3.3)
 *
 * @author Fabio
 */
public class PythonElementHyperlinkDetector extends AbstractHyperlinkDetector {

    /**
     * Will basically hyperlink any non keyword word (and let the PythonHyperlink work later on to open it if that's possible)
     */
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
        ITextEditor textEditor = (ITextEditor) getAdapter(ITextEditor.class);
        if (region == null || canShowMultipleHyperlinks || !(textEditor instanceof PyEdit)) {
            return null;
        }

        PyEdit editor = (PyEdit) textEditor;

        int offset = region.getOffset();

        try {
            IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
            
            //see if we can find a word there
            IRegion wordRegion = PythonWordFinder.findWord(document, offset);
            if (wordRegion == null) {
                return null;
            }

            //don't highlight keywords
            try {
                IDocument doc = editor.getDocument();
                String selectedWord = doc.get(wordRegion.getOffset(), wordRegion.getLength());
                if(PythonLanguageUtils.isKeyword(selectedWord)){
                    return null;
                }
            } catch (BadLocationException e) {
                PydevPlugin.log(e);
            }

            //return a hyperlink even without trying to find the definition (which may be costly)
            return new IHyperlink[] { new PythonHyperlink(wordRegion, editor) };
        } catch (Exception e) {
            PydevPlugin.log(e);
            return null;
        }

    }

}
