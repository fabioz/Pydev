package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.ICodeCompletionASTManager.ImportInfo;

public abstract class AbstractPyCodeCompletion  implements IPyCodeCompletion  {

    /* (non-Javadoc)
     * @see org.python.pydev.editor.codecompletion.IPyCodeCompletion#getImportsTipperStr(org.python.pydev.editor.codecompletion.CompletionRequest)
     */
    public ImportInfo getImportsTipperStr(CompletionRequest request) {
        
        IDocument doc = request.doc;
        int documentOffset = request.documentOffset;
        
        return PyCodeCompletionUtils.getImportsTipperStr(doc, documentOffset);
    }

}
