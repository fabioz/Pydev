package org.python.pydev.ast.codecompletion.revisited;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.autoedit.DefaultIndentPrefs;
import org.python.pydev.core.parser.IPyParser;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.PyParser.ParserInfo;
import org.python.pydev.parser.PythonNatureStub;
import org.python.pydev.shared_core.editor.IBaseEditor;
import org.python.pydev.shared_core.model.IModelListener;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;
import org.python.pydev.shared_core.string.ICoreTextSelection;

public class PyEditStub implements IPyEdit {
    public IDocument doc;
    public int parserChanged;
    private Map<String, Object> cache = new HashMap<String, Object>();
    private IEditorInput pydevFileEditorInputStub;
    private IPythonNature nature;
    public IFile iFile;
    public File file;

    public PyEditStub(IDocument doc, IEditorInput pydevFileEditorInputStub) {
        this(doc, pydevFileEditorInputStub, new PythonNatureStub(), null);
    }

    public PyEditStub(IDocument doc, IEditorInput pydevFileEditorInputStub, IPythonNature nature, File file) {
        this.doc = doc;
        this.pydevFileEditorInputStub = pydevFileEditorInputStub;
        this.nature = nature;
        this.file = file;
    }

    @Override
    public IEditorInput getEditorInput() {
        return pydevFileEditorInputStub;
    }

    @Override
    public IPythonNature getPythonNature() {
        return nature;
    }

    public void setParser(IPyParser parser) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object getParser() {
        return null;
    }

    @Override
    public void parserChanged(ISimpleNode root, IAdaptable file, IDocument doc, long docModificationStamp) {
        this.parserChanged += 1;
    }

    @Override
    public void parserError(Throwable error, IAdaptable file, IDocument doc) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Map<String, Object> getCache() {
        return this.cache;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }

    @Override
    public boolean hasSameInput(IBaseEditor edit) {
        if (this == edit) {
            throw new RuntimeException(
                    "Cannot compare when it's the same... it should change the document in this case");
        }
        if (edit.getEditorInput() == getEditorInput()) {
            return true;
        }
        return false;
    }

    @Override
    public IDocument getDocument() {
        return doc;
    }

    public void setDocument(IDocument doc) {
        this.doc = doc;
    }

    public void setInput(PydevFileEditorInputStub input) {
        this.pydevFileEditorInputStub = input;
    }

    @Override
    public void setStatusLineErrorMessage(String msg) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IGrammarVersionProvider getGrammarVersionProvider() {
        return this.getPythonNature();
    }

    @Override
    public IIndentPrefs getIndentPrefs() {
        return DefaultIndentPrefs.get(null);
    }

    @Override
    public Object getFormatStd() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void addModelListener(IModelListener modelListener) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void removeModelListener(IModelListener modelListener) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int getGrammarVersion() throws MisconfigurationException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions() throws MisconfigurationException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IProject getProject() {
        return this.nature.getProject();
    }

    @Override
    public Object getAST() {
        try {
            ParseOutput output = PyParser.parseFull(new ParserInfo(doc, nature));
            return output.ast; // It's ok if we have errors, return what we have.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public File getEditorFile() {
        return file;
    }

    @Override
    public long getAstModificationTimeStamp() {
        return 0;
    }

    @Override
    public IFile getIFile() {
        return iFile;
    }

    @Override
    public void addOfflineActionListener(String key, Object action, String description, boolean needsEnter) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isCythonFile() {
        return false;
    }

    @Override
    public ICoreTextSelection getTextSelection() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int getPrintMarginColums() {
        throw new RuntimeException("Not implemented");
    }
}
