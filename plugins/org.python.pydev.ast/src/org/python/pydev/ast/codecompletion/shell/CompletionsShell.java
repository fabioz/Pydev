package org.python.pydev.ast.codecompletion.shell;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledToken;
import org.python.pydev.core.IToken;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

public abstract class CompletionsShell extends AbstractShell {

    public CompletionsShell(File scriptWithinPySrc) throws IOException, CoreException {
        super(scriptWithinPySrc);
    }

    /**
     * @param pythonpath
     */
    private void internalChangePythonPath(List<String> pythonpath) throws Exception {
        if (finishedForGood) {
            throw new RuntimeException(
                    "Shells are already finished for good, so, it is an invalid state to try to change its dir.");
        }
        String pythonpathStr;

        synchronized (lockLastPythonPath) {
            pythonpathStr = StringUtils.join("|", pythonpath.toArray(new String[pythonpath.size()]));

            if (lastPythonPath != null && lastPythonPath.equals(pythonpathStr)) {
                return;
            }
            lastPythonPath = pythonpathStr;
        }
        try {
            writeAndGetResults("@@CHANGE_PYTHONPATH:", URLEncoder.encode(pythonpathStr, ENCODING_UTF_8), "\nEND@@");
        } catch (Exception e) {
            Log.log("Error changing the pythonpath to: " + StringUtils.join("\n", pythonpath), e);
            throw e;
        }
    }

    /**
     * @return list with tuples: new String[]{token, description}
     * @throws CoreException
     */
    public Tuple<String, List<String[]>> getImportCompletions(String str, List<String> pythonpath)
            throws Exception {
        FastStringBuffer read = null;

        str = URLEncoder.encode(str, ENCODING_UTF_8);

        try (AutoCloseable permit = acquire(StringUtils.join("", "getImportCompletions: ", str))) {
            internalChangePythonPath(pythonpath);
            read = this.writeAndGetResults("@@IMPORTS:", str, "\nEND@@");
        }
        return ShellConvert.convertStringToCompletions(read);
    }

    /**
     * @param moduleName the name of the module where the token is defined
     * @param token the token we are looking for
     * @return the file where the token was defined, its line and its column (or null if it was not found)
     * @throws Exception
     */
    public Tuple<String[], int[]> getLineCol(String moduleName, String token, List<String> pythonpath)
            throws Exception {
        FastStringBuffer read = null;

        String str = moduleName + "." + token;
        str = URLEncoder.encode(str, ENCODING_UTF_8);

        try (AutoCloseable permit = acquire("getLineCol")) {
            internalChangePythonPath(pythonpath);
            read = this.writeAndGetResults("@@SEARCH", str, "\nEND@@");
        }

        Tuple<String, List<String[]>> theCompletions = ShellConvert.convertStringToCompletions(read);

        List<String[]> def = theCompletions.o2;
        if (def.size() == 0) {
            return null;
        }

        String[] comps = def.get(0);
        if (comps.length == 0) {
            return null;
        }

        int line = Integer.parseInt(comps[0]);
        int col = Integer.parseInt(comps[1]);

        String foundAs = comps[2];
        return new Tuple<String[], int[]>(new String[] { theCompletions.o1, foundAs }, new int[] { line, col });
    }

    /**
     * Gets completions for jedi library (https://github.com/davidhalter/jedi)
     */
    public List<IToken> getJediCompletions(File editorFile, PySelection ps, String charset,
            List<String> pythonpath) throws Exception {

        FastStringBuffer read = null;
        String str = StringUtils.join(
                "|",
                new String[] { String.valueOf(ps.getCursorLine()), String.valueOf(ps.getCursorColumn()),
                        charset, FileUtils.getFileAbsolutePath(editorFile),
                        StringUtils.replaceNewLines(ps.getDoc().get(), "\n") });

        str = URLEncoder.encode(str, ENCODING_UTF_8);

        try (AutoCloseable permit = acquire("getJediCompletions")) {
            internalChangePythonPath(pythonpath);
            read = this.writeAndGetResults("@@MSG_JEDI:", str, "\nEND@@");
        }

        Tuple<String, List<String[]>> theCompletions = ShellConvert.convertStringToCompletions(read);
        ArrayList<IToken> lst = new ArrayList<>(theCompletions.o2.size());
        for (String[] s : theCompletions.o2) {
            //new CompiledToken(rep, doc, args, parentPackage, type);
            lst.add(new CompiledToken(s[0], s[1], "", "", Integer.parseInt(s[3]), null));
        }
        return lst;
    }

}
