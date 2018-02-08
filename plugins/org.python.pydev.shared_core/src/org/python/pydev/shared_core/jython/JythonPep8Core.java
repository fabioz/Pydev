package org.python.pydev.shared_core.jython;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.callbacks.ICallback;

public class JythonPep8Core {

    // Extension point.
    public static ICallback<Object, JythonPep8Core> analyzeCallback;

    public final String absolutePath;
    public final IDocument document;
    public final boolean useConsole;
    public final Object visitor;
    public final String[] pep8CommandLine;

    public JythonPep8Core(String absolutePath, IDocument document, boolean useConsole, Object visitor,
            String[] pep8CommandLine) {
        this.absolutePath = absolutePath;
        this.document = document;
        this.useConsole = useConsole;
        this.visitor = visitor;
        this.pep8CommandLine = pep8CommandLine;
    }

    public static boolean isAnalyzeCallbackSet() {
        return analyzeCallback != null;
    }

    public void analyze() {
        analyzeCallback.call(this);
    }
}