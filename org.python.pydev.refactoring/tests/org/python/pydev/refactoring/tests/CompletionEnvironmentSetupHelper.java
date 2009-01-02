package org.python.pydev.refactoring.tests;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.editor.codecompletion.shell.PythonShell;
import org.python.pydev.editor.codecompletion.shell.PythonShellTest;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.refactoring.ast.PythonModuleManager;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;

public class CompletionEnvironmentSetupHelper {


    /**
     * Static because we only want to initialize it once
     */
    public static PythonShell shell;
    public CodeCompletionTestsBase completionTestsBase;

    /**
     * Make things available for code-completion.
     */
    public void setupEnv() throws Exception {
        //use the completion test base to have setup the env to have tokens on builtins
        completionTestsBase = new CodeCompletionTestsBase();
        completionTestsBase.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        completionTestsBase.restorePythonPath(TestDependent.GetCompletePythonLib(true), false);
        completionTestsBase.codeCompletion = new PyCodeCompletion();

        //we don't want to start it more than once
        if(shell == null){
            shell = PythonShellTest.startShell();
        }
        AbstractShell.putServerShell(CodeCompletionTestsBase.nature, AbstractShell.COMPLETION_SHELL, shell);
    }
    
    /**
     * Remove the shell from the code-completion!
     * @throws Exception 
     */
    public void tearDownEnv() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        completionTestsBase.tearDown();
        AbstractShell.putServerShell(CodeCompletionTestsBase.nature, AbstractShell.COMPLETION_SHELL, null);
    }

    /**
     * @return the configured nature (provided that setupEnv and tearDownEnv were properly called).
     */
    public IPythonNature getNature() {
        return CodeCompletionTestsBase.nature;
    }

    /**
     * @param ioTestCase the pre-configured test case (with file and source)
     * @return a module adapter for the file set in the io test case.
     * @throws ParseException
     */
    public ModuleAdapter createModuleAdapter(AbstractIOTestCase ioTestCase) throws ParseException {
      return VisitorFactory.createModuleAdapter(new PythonModuleManager(getNature()), ioTestCase.getFile(), 
              new Document(ioTestCase.getSource()), getNature());
    }
}
