package com.aptana.js.interactive_console.rhino;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.tools.shell.Global;
import org.python.pydev.core.log.Log;

/**
 * This is the basic implementation for the interpreter. Note that it's much more complicated than
 * it appears it should be because rhino commands must all be evaluated at the same thread, so,
 * it creates an internal thread for that purpose and uses a queue to synchronize things with it.
 * 
 * Note that this also means that the interpreter must be properly disposed.
 * 
 * @author Fabio Zadrozny
 */
public class RhinoInterpreter {

    private static final boolean DEBUG = false;

    /**
     * Command to be passed to the thread.
     */
    private static interface ICommand {
        void evaluate();
    }

    public static final Object NO_RESULT = new Object() {
        public String toString() {
            return "NO_RESULT";
        };
    };

    /**
     * Command to signal that the thread/interpreter should be disposed.
     */
    private class DisposeCommand implements ICommand {
        public void evaluate() {
            //Does nothing: just used to notify that the thread should be disposed.
        }

        @Override
        public String toString() {
            return "DisposeCommand";
        }
    }

    /**
     * Some command that evaluates in the thread and returns some result.
     */
    private abstract class AbstractResultCommand implements ICommand {

        private final Object lock = new Object();

        /**
         * The result of the evaluation.
         */
        private volatile Object result;
        /**
         * Some exception that happened during the evaluation.
         */
        private volatile Throwable exception;

        public AbstractResultCommand() {
            this.result = NO_RESULT;
        }

        public void evaluate() {
            try {
                this.result = onEvaluate();
            } catch (Throwable e) {
                this.exception = e;
            }

            synchronized (lock) {
                lock.notify();
            }
        }

        /**
         * Subclasses must override to do the actual evaluation.
         */
        protected abstract Object onEvaluate();

        /**
         * If the evaluation throws an exception, rethrows it.
         */
        public Object getResult() throws Exception {
            while (true) {
                if (DEBUG) {
                    System.out.println("Locking to get result for: " + this);
                }
                try {
                    synchronized (lock) {
                        if (DEBUG) {
                            System.out.println("Result for: " + this + ": " + result + " - Exception: "
                                    + this.exception);
                        }
                        if (this.exception != null) {
                            if (this.exception instanceof Exception) {
                                throw (Exception) this.exception;
                            } else {
                                throw new RuntimeException(this.exception);
                            }
                        }
                        if (this.result != NO_RESULT) {
                            return this.result;
                        }
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        }
    }

    /**
     * Command to evaluate something. Clients get locked in the getResult() until the result is
     * available.
     */
    private class EvalCommand extends AbstractResultCommand {

        private String source;
        private int line;

        public EvalCommand(String source, int line) {
            super();
            this.source = source;
            this.line = line;
        }

        /**
         * Returns undefined or a string-representation of the evaluation.
         */
        protected Object onEvaluate() {
            Object eval;
            if (DEBUG) {
                try {
                    eval = cx.evaluateString(global, source, "eval", line, null);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
            } else {
                eval = cx.evaluateString(global, source, "eval", line, null);
            }
            if (!(eval instanceof Undefined)) {
                return Context.toString(eval);
            }
            return eval; //return undefined
        }

        @Override
        public String toString() {
            return "EvalCommand: " + source;
        }
    }

    /**
     * Evaluation context
     */
    private Context cx;

    /**
     * Scope where the evaluation should happen
     */
    private Global global;

    /**
     * Queue to help in synchronizing commands.
     */
    private final BlockingQueue<ICommand> queue = new LinkedBlockingQueue<ICommand>();

    private class RhinoInterpreterThread extends Thread {

        /**
         * Whether the thread should keep running.
         */
        private volatile boolean finished = false;

        public RhinoInterpreterThread() {
            super();
            setName("RhinoInterpreterThread");
        }

        @Override
        public void run() {
            ContextFactory contextFactory = new ContextFactory();
            RhinoInterpreter.this.global = new Global();
            RhinoInterpreter.this.global.init(contextFactory);
            RhinoInterpreter.this.cx = contextFactory.enterContext();
            while (!finished) {
                try {
                    ICommand cmd = queue.take();
                    if (cmd instanceof DisposeCommand) {
                        finished = true;
                    }
                    if (DEBUG) {
                        System.out.println("About to evaluate: " + cmd);
                    }
                    try {
                        cmd.evaluate();
                    } catch (Throwable e) {
                        if (DEBUG) {
                            System.out.println("Evaluation finished with ERROR: " + cmd);
                        }
                        Log.log(e);
                    }
                    if (DEBUG) {
                        System.out.println("Finished evaluation: " + cmd);
                    }
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        }

        public void setErr(final OutputStream stream) {
            global.setErr(new PrintStream(stream));
        }

        public void setOut(final OutputStream stream) {
            global.setOut(new PrintStream(stream));
        }

        public PrintStream getOut() {
            return global.getOut();
        }

        public Object getErr() {
            return global.getErr();
        }

        /**
         * Array with tuples with name, doc, args, type
         */
        public List<Object[]> getCompletions(String text, String actTok) {
            ArrayList<Object[]> ret = new ArrayList<Object[]>();

            Scriptable obj;
            int index = actTok.lastIndexOf('.');
            if (index != -1) {
                String var = actTok.substring(0, index);
                actTok = actTok.substring(index + 1);
                try {
                    Object eval = cx.evaluateString(global, var, "<eval>", 0, null);
                    if (eval instanceof Scriptable) {
                        obj = (Scriptable) eval;
                    } else {
                        return ret; //not something we can complete on.
                    }
                } catch (Exception e) {
                    return ret; //unable to get variable.
                }

            } else {
                obj = global;
            }

            Object val = obj.get(actTok, global);
            if (val instanceof Scriptable) {
                obj = (Scriptable) val;
            }

            Object[] ids;
            if (obj instanceof ScriptableObject) {
                ids = ((ScriptableObject) obj).getAllIds();
            } else {
                ids = obj.getIds();
            }
            //types: 
            // function: 2
            // local: 9
            // see: IToken.TYPE_
            String lastPart = actTok.toLowerCase();
            for (int i = 0; i < ids.length; i++) {
                if (!(ids[i] instanceof String)) {
                    continue;
                }
                String id = (String) ids[i];
                if (id.toLowerCase().startsWith(lastPart)) {
                    if (obj.get(id, obj) instanceof Function) {
                        ret.add(new Object[] { id, "", "()", 2 });
                    } else {
                        ret.add(new Object[] { id, "", "", 9 });
                    }
                }
            }
            return ret;
        }
    }

    private RhinoInterpreterThread rhinoThread;

    public RhinoInterpreter() {
        rhinoThread = new RhinoInterpreterThread();
        rhinoThread.start();
    }

    /**
     * Helper to add some command to the queue.
     * @param command
     * @return 
     */
    protected ICommand addCommand(ICommand command) {
        if (DEBUG) {
            System.out.println("Adding command: " + command);
        }
        boolean added = false;
        while (!added) {
            try {
                queue.put(command);
                added = true;
            } catch (InterruptedException e) {
            }
        }
        return command;
    }

    /**
     * Throws an exception if command was not properly evaluated.
     */
    public Object eval(String source) throws Exception {
        return eval(source, 0);
    }

    /**
     * Throws an exception if command was not properly evaluated.
     */
    public Object eval(String source, int line) throws Exception {
        EvalCommand command = new EvalCommand(source, line);
        addCommand(command);
        return command.getResult();
    }

    public void setErr(final OutputStream stream) {
        addCommand(new ICommand() {

            public void evaluate() {
                rhinoThread.setErr(new PrintStream(stream));
            }
        });
    }

    public void setOut(final OutputStream stream) {
        addCommand(new ICommand() {

            public void evaluate() {
                rhinoThread.setOut(new PrintStream(stream));
            }
        });
    }

    public PrintStream getOut() {
        AbstractResultCommand cmd = new AbstractResultCommand() {

            @Override
            protected Object onEvaluate() {
                return rhinoThread.getOut();
            }

            @Override
            public String toString() {
                return "AbstractResultCommand:getOut()";
            }
        };
        addCommand(cmd);
        try {
            return (PrintStream) cmd.getResult();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void dispose() {
        addCommand(new DisposeCommand());
    }

    public PrintStream getErr() {
        AbstractResultCommand cmd = new AbstractResultCommand() {

            @Override
            protected Object onEvaluate() {
                return rhinoThread.getErr();
            }

            @Override
            public String toString() {
                return "AbstractResultCommand:getErr()";
            }
        };
        addCommand(cmd);
        try {
            return (PrintStream) cmd.getResult();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object getDescription(String evalStr) throws Exception {
        AbstractResultCommand cmd = new EvalCommand(evalStr, 0) {

            @Override
            protected Object onEvaluate() {
                Object o = super.onEvaluate();
                return Context.toString(o);
            }

            @Override
            public String toString() {
                return "EvalCommand:getDescription()";
            }
        };
        addCommand(cmd);
        return cmd.getResult();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getCompletions(final String text, final String actTok) {
        AbstractResultCommand cmd = new AbstractResultCommand() {

            @Override
            protected Object onEvaluate() {
                return rhinoThread.getCompletions(text, actTok);
            }

            @Override
            public String toString() {
                return "AbstractResultCommand:getCompletions()";
            }
        };
        addCommand(cmd);
        try {
            return (List<Object[]>) cmd.getResult();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
