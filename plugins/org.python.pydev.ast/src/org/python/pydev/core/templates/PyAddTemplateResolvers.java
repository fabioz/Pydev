package org.python.pydev.core.templates;

import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.shared_core.callbacks.ICallback;

public class PyAddTemplateResolvers {

    public static void addDefaultResolvers(TemplateContextType ctx) {
        ctx.addResolver(new GlobalTemplateVariables.Cursor());
        ctx.addResolver(new GlobalTemplateVariables.WordSelection());
        ctx.addResolver(new GlobalTemplateVariables.LineSelection());
        ctx.addResolver(new GlobalTemplateVariables.Dollar());
        ctx.addResolver(new GlobalTemplateVariables.Date());
        ctx.addResolver(new GlobalTemplateVariables.Year());
        ctx.addResolver(new GlobalTemplateVariables.Time());
        ctx.addResolver(new GlobalTemplateVariables.User());
        ctx.addResolver(PyTemplatesDefault.IsoDate());
        ctx.addResolver(PyTemplatesDefault.IsoDate1());
        ctx.addResolver(PyTemplatesDefault.IsoDate2());
        ctx.addResolver(PyTemplatesDefault.ModuleName());
        ctx.addResolver(PyTemplatesDefault.QualifiedNameScope());
        ctx.addResolver(PyTemplatesDefault.CurrentClass());
        ctx.addResolver(PyTemplatesDefault.SelfOrCls());
        ctx.addResolver(PyTemplatesDefault.PydevdFileLocation());
        ctx.addResolver(PyTemplatesDefault.PydevdDirLocation());
        ctx.addResolver(PyTemplatesDefault.CurrentMethod());
        ctx.addResolver(PyTemplatesDefault.PreviousClassOrMethod());
        ctx.addResolver(PyTemplatesDefault.NextClassOrMethod());
        ctx.addResolver(PyTemplatesDefault.Superclass());

        PyContextTypeVariables.addResolvers(ctx);
    }
}

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

class PyContextTypeVariables {

    private static boolean isGrammar3(PyDocumentTemplateContext context) {
        if (context == null) {
            return false;
        }
        return context.getGrammarVersion() >= IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_5;
    }

    public static void addResolvers(TemplateContextType pyContextType) {

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
