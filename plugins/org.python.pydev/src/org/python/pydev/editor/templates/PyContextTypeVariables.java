package org.python.pydev.editor.templates;

import org.eclipse.jface.text.templates.TemplateContext;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.editor.codecompletion.templates.PyDocumentTemplateContext;
import org.python.pydev.shared_core.callbacks.ICallback;

class CallableTemplateVariableResolver extends PyTemplateVariableResolver {

    private ICallback<String, PyDocumentTemplateContext> callable;

    protected CallableTemplateVariableResolver(String type, String description,
            ICallback<String, PyDocumentTemplateContext> callable) {
        super(type, description);
        this.callable = callable;
    }

    @Override
    protected String[] resolveAll(TemplateContext context) {
        String ret = this.callable.call((PyDocumentTemplateContext) context);
        if (ret == null) {
            ret = "";
        }
        return new String[] { ret };
    }
};

public class PyContextTypeVariables {

    private static boolean isGrammar3(PyDocumentTemplateContext context) {
        if (context == null) {
            return false;
        }
        return context.getGrammarVersion() >= IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_5;
    }

    public static void addResolvers(PyContextType pyContextType) {

        pyContextType.addResolver(new CallableTemplateVariableResolver("file", "Full path for file", (context) -> {
            return context.getEditorFile().toString().replace("\\", "/");
        }));

        pyContextType.addResolver(
                new CallableTemplateVariableResolver("space_if_py2", "Adds a space if python 2", (context) -> {
                    if (PyContextTypeVariables.isGrammar3(context)) {
                        return "";
                    }
                    return " ";
                }));

        pyContextType.addResolver(
                new CallableTemplateVariableResolver("rparen_if_py3", "Adds a ) if python 3", (context) -> {
                    if (PyContextTypeVariables.isGrammar3(context)) {
                        return ")";
                    }
                    return "";
                }));

        pyContextType.addResolver(
                new CallableTemplateVariableResolver("lparen_if_py3", "Adds a ( if python 3", (context) -> {
                    if (PyContextTypeVariables.isGrammar3(context)) {
                        return "(";
                    }
                    return "";
                }));

    }

}
