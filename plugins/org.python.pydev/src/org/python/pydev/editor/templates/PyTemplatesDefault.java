package org.python.pydev.editor.templates;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.templates.PyDocumentTemplateContext;
import org.python.pydev.parser.fastparser.FastParser;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class PyTemplatesDefault {

    public static class CallableTemplateVariableResolver extends PyTemplateVariableResolver {

        private ICallback<Object /*String or List<String>*/, PyDocumentTemplateContext> callable;

        public CallableTemplateVariableResolver(String type, String description,
                ICallback<Object /*String or List<String>*/, PyDocumentTemplateContext> callable) {
            super(type, description);
            this.callable = callable;
        }

        @Override
        public String[] resolveAll(TemplateContext context) {
            Object obj = this.callable.call((PyDocumentTemplateContext) context);
            if (obj == null) {
                return new String[0];
            }
            if (obj instanceof String) {
                String ret = (String) obj;
                return new String[] { ret };
            }
            if (obj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) obj;
                return list.toArray(new String[0]);
            }
            throw new RuntimeException("Error. Expected String or List<String>. Found: " + obj);
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

    public static ClassDef getCurrentClassStmt(PyDocumentTemplateContext context) {
        for (stmtType stmt : getCurrentAstPath(context, true)) {
            if (stmt instanceof ClassDef) {
                return (ClassDef) stmt;
            }
        }
        return null;
    }

    public static CallableTemplateVariableResolver CurrentClass() {
        return new PyTemplatesDefault.CallableTemplateVariableResolver("current_class",
                "Current class",
                (context) -> {
                    stmtType stmt = getCurrentClassStmt(context);
                    if (stmt != null) {
                        return NodeUtils.getRepresentationString(stmt);
                    }

                    return "";
                });
    }

    public static CallableTemplateVariableResolver SelfOrCls() {
        return new PyTemplatesDefault.CallableTemplateVariableResolver("self_or_cls",
                "Get `self` or `cls`",
                (context) -> {
                    PySelection selection = context.createPySelection();
                    stmtType node = FastParser.firstClassOrFunction(context.getDocument(),
                            selection.getStartLineIndex(), false, false);
                    if (node instanceof FunctionDef) {
                        String firstToken = selection.getFirstInsideParentesisTok(node.beginLine - 1);
                        if ("cls".equals(firstToken)) {
                            return "cls";
                        }
                    }

                    return "self";
                });
    }

    public static TemplateVariableResolver PydevdFileLocation() {
        return new PyTemplatesDefault.CallableTemplateVariableResolver("pydevd_file_location",
                "pydevd.py File Location",
                (context) -> {

                    try {
                        return FileUtils.getFileAbsolutePath(CorePlugin.getScriptWithinPySrc("pydevd.py"));
                    } catch (CoreException e) {
                        Log.log(e);
                        return "<unable to get pydevd.py location>";
                    }
                });
    }

    public static TemplateVariableResolver PydevdDirLocation() {
        return new PyTemplatesDefault.CallableTemplateVariableResolver("pydevd_dir_location",
                "pydevd.py Directory Location",
                (context) -> {

                    try {
                        return FileUtils
                                .getFileAbsolutePath(CorePlugin.getScriptWithinPySrc("pydevd.py").getParentFile());
                    } catch (CoreException e) {
                        Log.log(e);
                        return "<unable to get pydevd.py directory location>";
                    }
                });
    }

    public static TemplateVariableResolver CurrentMethod() {
        return new PyTemplatesDefault.CallableTemplateVariableResolver("current_method",
                "Current method",
                (context) -> {
                    for (stmtType stmt : getCurrentAstPath(context, true)) {
                        if (stmt instanceof FunctionDef) {
                            return NodeUtils.getRepresentationString(stmt);
                        }
                    }
                    return "";
                });
    }

    private static String getPreviousOrNextClassOrMethod(PyDocumentTemplateContext context, boolean searchForward) {
        IDocument doc = context.getDocument();
        PySelection selection = context.createPySelection();
        int startLine = selection.getStartLineIndex();
        stmtType found = FastParser.firstClassOrFunction(doc, startLine, searchForward, context.isCythonFile());
        if (found != null) {
            return NodeUtils.getRepresentationString(found);
        }
        return "";
    }

    public static TemplateVariableResolver PreviousClassOrMethod() {
        return new PyTemplatesDefault.CallableTemplateVariableResolver("prev_class_or_method",
                "Previous class or method",
                (context) -> {
                    return getPreviousOrNextClassOrMethod(context, false);
                });
    }

    public static TemplateVariableResolver NextClassOrMethod() {
        return new PyTemplatesDefault.CallableTemplateVariableResolver("next_class_or_method",
                "Next class or method",
                (context) -> {
                    return getPreviousOrNextClassOrMethod(context, true);
                });
    }

    public static TemplateVariableResolver Superclass() {
        return new PyTemplatesDefault.CallableTemplateVariableResolver("superclass",
                "Superclass of the current class",
                (context) -> {
                    PySelection selection = context.createPySelection();
                    ClassDef stmt = getCurrentClassStmt(context);
                    if (stmt == null) {
                        return "";
                    }

                    IDocument doc = context.getDocument();
                    NameTok name = (NameTok) stmt.name;
                    int nameStartOffset = selection.getAbsoluteCursorOffset(name.beginLine - 1, name.beginColumn - 1);
                    nameStartOffset += name.id.length();

                    boolean foundStart = false;
                    int i = 0;
                    FastStringBuffer contents = new FastStringBuffer();
                    while (true) {
                        try {
                            char c = doc.get(nameStartOffset + i, 1).charAt(0);
                            i++;

                            if (c == '(') {
                                foundStart = true;
                            } else if (c == ')' || c == ':') {
                                break;
                            } else if (c == '\r' || c == '\n' || c == ' ' || c == '\t') {
                                // pass
                            } else if (c == '#') { // Skip comments
                                while (c != '\r' && c != '\n') {
                                    c = doc.get(nameStartOffset + i, 1).charAt(0);
                                    i++;
                                }
                            } else {
                                if (foundStart) {
                                    contents.append(c);
                                }
                            }
                        } catch (BadLocationException e) {
                            return ""; // Seems the class declaration is not properly finished as we're now out of bounds in the doc.
                        }
                    }

                    if (contents.indexOf(',') != -1) {
                        List<String> ret = new ArrayList<>();
                        for (String param : contents.toString().split(",")) {
                            ret.add(param.trim());
                        }
                        return ret;
                    }

                    return contents.toString().trim();
                });
    }

}
