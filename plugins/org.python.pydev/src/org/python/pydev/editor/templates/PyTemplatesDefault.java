package org.python.pydev.editor.templates;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.text.templates.TemplateContext;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.codecompletion.templates.PyDocumentTemplateContext;
import org.python.pydev.parser.fastparser.FastParser;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class PyTemplatesDefault {

    public static class CallableTemplateVariableResolver extends PyTemplateVariableResolver {

        private ICallback<String, PyDocumentTemplateContext> callable;

        public CallableTemplateVariableResolver(String type, String description,
                ICallback<String, PyDocumentTemplateContext> callable) {
            super(type, description);
            this.callable = callable;
        }

        @Override
        public String[] resolveAll(TemplateContext context) {
            String ret = this.callable.call((PyDocumentTemplateContext) context);
            if (ret == null) {
                return new String[0];
            }
            return new String[] { ret };
        }
    }

    public static CallableTemplateVariableResolver IsoDate() {
        return new PyTemplatesDefault.CallableTemplateVariableResolver("isodate", "ISO-8601 Ymd date", (context) -> {
            // in Python: time.strftime("%Y-%m-%d")
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            return formatter.format(date);
        });
    }

    public static CallableTemplateVariableResolver IsoDate1() {
        return new PyTemplatesDefault.CallableTemplateVariableResolver("isodatestr", "ISO-8601 Ymd HM date",
                (context) -> {
                    // in Python: time.strftime("%Y-%m-%d %H:%M")
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    Date date = new Date();
                    return formatter.format(date);
                });
    }

    public static CallableTemplateVariableResolver IsoDate2() {
        return new PyTemplatesDefault.CallableTemplateVariableResolver("isodatestr2", "ISO-8601 Ymd HM date",
                (context) -> {
                    // in Python: time.strftime("%Y-%m-%d %H:%M:%S")
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = new Date();
                    return formatter.format(date);
                });
    }

    public static CallableTemplateVariableResolver ModuleName() {
        return new PyTemplatesDefault.CallableTemplateVariableResolver("module", "Current module",
                (context) -> {
                    return context.getModuleName();
                });
    }

    public static List<stmtType> getCurrentAstPath(PyDocumentTemplateContext context) {
        return getCurrentAstPath(context, false);
    }

    public static List<stmtType> getCurrentAstPath(PyDocumentTemplateContext context, boolean reverse) {
        PySelection selection = context.createPySelection();
        List<stmtType> ret = FastParser.parseToKnowGloballyAccessiblePath(context.getDocument(),
                selection.getStartLineIndex());
        if (reverse) {
            Collections.reverse(ret);
        }
        return ret;
    }

    public static CallableTemplateVariableResolver QualifiedNameScope() {
        return new PyTemplatesDefault.CallableTemplateVariableResolver("current_qualified_scope",
                "Current qualified scope",
                (context) -> {
                    List<stmtType> stmts = getCurrentAstPath(context);
                    FastStringBuffer buf = new FastStringBuffer();
                    for (stmtType stmt : stmts) {
                        if (!buf.isEmpty()) {
                            buf.append('.');
                        }
                        buf.append(NodeUtils.getRepresentationString(stmt));
                    }
                    return "";
                });
    }

}
