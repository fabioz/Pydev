package org.python.pydev.editor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.python.pydev.ast.codecompletion.PyCodeCompletionPreferences;
import org.python.pydev.core.IPySyntaxHighlightingAndCodeCompletionEditor;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.PythonPartitions;
import org.python.pydev.editor.codecompletion.PyContentAssistant;
import org.python.pydev.editor.codecompletion.PythonCompletionProcessor;
import org.python.pydev.editor.codecompletion.PythonStringCompletionProcessor;
import org.python.pydev.editor.codecompletion.SimpleAssistProcessor;

public class SetupContentAssist {

    public static IContentAssistant configContentAssistant(IPySyntaxHighlightingAndCodeCompletionEditor edit,
            PyContentAssistant pyContentAssistant) {
        // next create a content assistant processor to populate the completions window
        IContentAssistProcessor processor = new SimpleAssistProcessor(edit, new PythonCompletionProcessor(edit,
                pyContentAssistant), pyContentAssistant);

        PythonStringCompletionProcessor stringProcessor = new PythonStringCompletionProcessor(edit, pyContentAssistant);

        // No code completion in comments and strings
        for (String s : PythonPartitions.STRING_PROCESSOR_PARTITIONS) {
            pyContentAssistant.setContentAssistProcessor(stringProcessor, s);
        }

        pyContentAssistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
        pyContentAssistant.enableAutoActivation(true); //always true, but the chars depend on whether it is activated or not in the preferences

        //note: delay and auto activate are set on PyContentAssistant constructor.

        pyContentAssistant.setDocumentPartitioning(IPythonPartitions.PYTHON_PARTITION_TYPE);
        pyContentAssistant.setAutoActivationDelay(PyCodeCompletionPreferences.getAutocompleteDelay());

        return pyContentAssistant;
    }
}
