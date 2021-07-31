package org.python.pydev.ast.codecompletion.shell;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.ast.runners.SimplePythonRunner;
import org.python.pydev.ast.runners.SimpleRunner;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.log.Log;
import org.python.pydev.json.eclipsesource.JsonObject;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class CythonShell extends AbstractShell {

    /**
     * Initialize with the default python server file.
     *
     * @throws IOException
     * @throws CoreException
     */
    public CythonShell() throws IOException, CoreException {
        super(CorePlugin.getScriptWithinPySrc(new Path("third_party").append("cython_json.py").toString()));
    }

    @Override
    protected synchronized ProcessCreationInfo createServerProcess(IInterpreterInfo interpreter, int port)
            throws IOException {
        File file = new File(interpreter.getExecutableOrJar());
        if (file.isDirectory()) {
            throw new RuntimeException("The interpreter location found is a directory. " + interpreter);
        }

        String[] parameters = SimplePythonRunner.preparePythonCallParameters(interpreter.getExecutableOrJar(),
                FileUtils.getFileAbsolutePath(serverFile), new String[] { "" + port });

        IInterpreterManager manager = InterpreterManagersAPI.getPythonInterpreterManager();

        String[] envp = null;
        try {
            envp = SimpleRunner.getEnvironment(null, interpreter, manager);
        } catch (CoreException e) {
            Log.log(e);
        }

        File workingDir = serverFile.getParentFile();

        return new ProcessCreationInfo(parameters, envp, workingDir, SimpleRunner.createProcess(parameters, envp,
                workingDir));
    }

    public String convertToJsonAst(String string) throws CoreException {
        JsonObject object = new JsonObject();
        object.add("command", "cython_to_json_ast");
        object.add("contents", string);
        FastStringBuffer writeAndGetResults = writeAndGetResults(true, object.toString());
        if (writeAndGetResults == null) {
            throw new RuntimeException("Error. Unable to get cython ast as json.");
        }
        return writeAndGetResults.toString();
    }
}
