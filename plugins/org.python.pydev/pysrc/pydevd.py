#IMPORTANT: pydevd_constants must be the 1st thing defined because it'll keep a reference to the original sys._getframe
from __future__ import nested_scopes #Jython 2.1 support
from pydevd_constants import * #@UnusedWildImport
import pydev_imports
from pydevd_comm import  CMD_CHANGE_VARIABLE, \
                         CMD_EVALUATE_EXPRESSION, \
                         CMD_EVALUATE_CONSOLE_EXPRESSION, \
                         CMD_EXEC_EXPRESSION, \
                         CMD_GET_COMPLETIONS, \
                         CMD_GET_FRAME, \
                         CMD_SET_PY_EXCEPTION, \
                         CMD_GET_VARIABLE, \
                         CMD_LIST_THREADS, \
                         CMD_REMOVE_BREAK, \
                         CMD_RUN, \
                         CMD_SET_BREAK, \
                         CMD_SET_NEXT_STATEMENT, \
                         CMD_STEP_INTO, \
                         CMD_STEP_OVER, \
                         CMD_STEP_RETURN, \
                         CMD_THREAD_CREATE, \
                         CMD_THREAD_KILL, \
                         CMD_THREAD_RUN, \
                         CMD_THREAD_SUSPEND, \
                         CMD_RUN_TO_LINE, \
                         CMD_RELOAD_CODE, \
                         CMD_VERSION, \
                         CMD_GET_FILE_CONTENTS, \
                         CMD_SET_PROPERTY_TRACE, \
                         GetGlobalDebugger, \
                         InternalChangeVariable, \
                         InternalGetCompletions, \
                         InternalEvaluateExpression, \
                         InternalGetFrame, \
                         InternalGetVariable, \
                         InternalEvaluateConsoleExpression, \
                         InternalConsoleGetCompletions, \
                         InternalTerminateThread, \
                         InternalRunThread, \
                         InternalStepThread, \
                         NetCommand, \
                         NetCommandFactory, \
                         PyDBDaemonThread, \
                         PydevQueue, \
                         ReaderThread, \
                         SetGlobalDebugger, \
                         WriterThread, \
                         PydevdFindThreadById, \
                         PydevdLog, \
                         StartClient, \
                         StartServer, \
                         InternalSetNextStatementThread

from pydevd_file_utils import NormFileToServer, GetFilenameAndBase
import pydevd_import_class
import pydevd_vars
import traceback
import pydevd_vm_type
import pydevd_tracing
import pydevd_io
from pydevd_additional_thread_info import PyDBAdditionalThreadInfo
import pydevd_traceproperty
import time
threadingEnumerate = threading.enumerate
threadingCurrentThread = threading.currentThread


DONT_TRACE = {
              #commonly used things from the stdlib that we don't want to trace
              'threading.py':1,
              'Queue.py':1,
              'socket.py':1,

              #things from pydev that we don't want to trace
              'pydevd_additional_thread_info.py':1,
              'pydevd_comm.py':1,
              'pydevd_constants.py':1,
              'pydevd_file_utils.py':1,
              'pydevd_frame.py':1,
              'pydevd_io.py':1 ,
              'pydevd_resolver.py':1 ,
              'pydevd_tracing.py':1 ,
              'pydevd_vars.py':1,
              'pydevd_vm_type.py':1,
              'pydevd.py':1 ,
              'pydevd_psyco_stub.py':1,
              'pydevd_traceproperty.py':1
              }

if IS_PY3K:
    #if we try to trace io.py it seems it can get halted (see http://bugs.python.org/issue4716)
    DONT_TRACE['io.py'] = 1

    #Don't trace common encodings too
    DONT_TRACE['cp1252.py'] = 1
    DONT_TRACE['utf_8.py'] = 1


connected = False
bufferStdOutToServer = False
bufferStdErrToServer = False


#=======================================================================================================================
# PyDBCommandThread
#=======================================================================================================================
class PyDBCommandThread(PyDBDaemonThread):

    def __init__(self, pyDb):
        PyDBDaemonThread.__init__(self)
        self.pyDb = pyDb
        self.setName('pydevd.CommandThread')

    def OnRun(self):
        time.sleep(5) #this one will only start later on (because otherwise we may not have any non-daemon threads

        run_traced = True

        if pydevd_vm_type.GetVmType() == pydevd_vm_type.PydevdVmType.JYTHON and sys.hexversion <= 0x020201f0:
            #don't run untraced threads if we're in jython 2.2.1 or lower
            #jython bug: if we start a thread and another thread changes the tracing facility
            #it affects other threads (it's not set only for the thread but globally) 
            #Bug: http://sourceforge.net/tracker/index.php?func=detail&aid=1870039&group_id=12867&atid=112867
            run_traced = False

        if run_traced:
            pydevd_tracing.SetTrace(None) # no debugging on this thread

        try:
            while not self.killReceived:
                try:
                    self.pyDb.processInternalCommands()
                except:
                    PydevdLog(0, 'Finishing debug communication...(2)')
                time.sleep(0.5)
        except:
            pass
            #only got this error in interpreter shutdown
            #PydevdLog(0, 'Finishing debug communication...(3)')


_original_excepthook = None


#=======================================================================================================================
# excepthook
#=======================================================================================================================
def excepthook(exctype, value, tb):
    #Always call the original excepthook before going on to call the debugger post mortem to show it.
    _original_excepthook(exctype, value, tb)

    debugger = GetGlobalDebugger()
    if debugger is None or not debugger.break_on_uncaught:
        return

    if debugger.handle_exceptions is not None:
        if not issubclass(exctype, debugger.handle_exceptions):
            return

    frames = []

    while tb:
        frames.append(tb.tb_frame)
        tb = tb.tb_next

    thread = threadingCurrentThread()
    frames_byid = dict([(id(frame), frame) for frame in frames])
    frame = frames[-1]
    thread.additionalInfo.pydev_force_stop_at_exception = (frame, frames_byid)
    debugger = GetGlobalDebugger()
    debugger.force_post_mortem_stop += 1


#=======================================================================================================================
# set_pm_excepthook
#=======================================================================================================================
def set_pm_excepthook(handle_exceptions=None):
    '''
    This function is now deprecated (PyDev provides an UI to handle that now).
    '''

    raise DeprecationWarning(
'''This function is now controlled directly in the PyDev UI.
I.e.: Go to the debug perspective and choose the menu:  PyDev > Manage exception breakpoints and
check "Suspend on uncaught exceptions".
Programmatically, it was replaced by: GetGlobalDebugger().setExceptHook
''')


try:
    import thread
except ImportError:
    import _thread as thread #Py3K changed it.
_original_start_new_thread = thread.start_new_thread

#=======================================================================================================================
# NewThreadStartup
#=======================================================================================================================
class NewThreadStartup:

    def __init__(self, original_func):
        self.original_func = original_func

    def __call__(self, *args, **kwargs):
        global_debugger = GetGlobalDebugger()
        if global_debugger is not None:
            pydevd_tracing.SetTrace(global_debugger.trace_dispatch)
        return self.original_func(*args, **kwargs)


#=======================================================================================================================
# ClassWithPydevStartNewThread
#=======================================================================================================================
class ClassWithPydevStartNewThread:

    def pydev_start_new_thread(self, function, args, kwargs={}):
        '''
        We need to replace the original thread.start_new_thread with this function so that threads started through
        it and not through the threading module are properly traced.
        '''
        return _original_start_new_thread(NewThreadStartup(function), args, kwargs)

#This is a hack for the situation where the thread.start_new_thread is declared inside a class, such as the one below
#class F(object):
#    start_new_thread = thread.start_new_thread
#    
#    def start_it(self):
#        self.start_new_thread(self.function, args, kwargs)
#So, if it's an already bound method, calling self.start_new_thread won't really receive a different 'self' -- it
#does work in the default case because in builtins self isn't passed either.
pydev_start_new_thread = ClassWithPydevStartNewThread().pydev_start_new_thread

#=======================================================================================================================
# PyDB
#=======================================================================================================================
class PyDB:
    """ Main debugging class
    Lots of stuff going on here:

    PyDB starts two threads on startup that connect to remote debugger (RDB)
    The threads continuously read & write commands to RDB.
    PyDB communicates with these threads through command queues.
       Every RDB command is processed by calling processNetCommand.
       Every PyDB net command is sent to the net by posting NetCommand to WriterThread queue

       Some commands need to be executed on the right thread (suspend/resume & friends)
       These are placed on the internal command queue.
    """


    def __init__(self):
        SetGlobalDebugger(self)
        pydevd_tracing.ReplaceSysSetTraceFunc()
        self.reader = None
        self.writer = None
        self.quitting = None
        self.cmdFactory = NetCommandFactory()
        self._cmd_queue = {}     # the hash of Queues. Key is thread id, value is thread
        self.breakpoints = {}
        self.readyToRun = False
        self._main_lock = threading.Lock()
        self._lock_running_thread_ids = threading.Lock()
        self._finishDebuggingSession = False
        self.force_post_mortem_stop = 0
        self.break_on_uncaught = False
        self.break_on_caught = False
        self.handle_exceptions = None

        # By default user can step into properties getter/setter/deleter methods
        self.disable_property_trace = False
        self.disable_property_getter_trace = False
        self.disable_property_setter_trace = False
        self.disable_property_deleter_trace = False

        #this is a dict of thread ids pointing to thread ids. Whenever a command is passed to the java end that
        #acknowledges that a thread was created, the thread id should be passed here -- and if at some time we do not
        #find that thread alive anymore, we must remove it from this list and make the java side know that the thread
        #was killed.
        self._running_thread_ids = {}


    def FinishDebuggingSession(self):
        self._finishDebuggingSession = True


    def initializeNetwork(self, sock):
        try:
            sock.settimeout(None) # infinite, no timeouts from now on - jython does not have it
        except:
            pass
        self.writer = WriterThread(sock)
        self.reader = ReaderThread(sock)
        self.writer.start()
        self.reader.start()

        time.sleep(0.1) # give threads time to start        

    def connect(self, host, port):
        if host:
            s = StartClient(host, port)
        else:
            s = StartServer(port)

        self.initializeNetwork(s)


    def getInternalQueue(self, thread_id):
        """ returns internal command queue for a given thread.
        if new queue is created, notify the RDB about it """
        try:
            return self._cmd_queue[thread_id]
        except KeyError:
            return self._cmd_queue.setdefault(thread_id, PydevQueue.Queue()) #@UndefinedVariable


    def postInternalCommand(self, int_cmd, thread_id):
        """ if thread_id is *, post to all """
        if thread_id == "*":
            for k in self._cmd_queue.keys():
                self._cmd_queue[k].put(int_cmd)

        else:
            queue = self.getInternalQueue(thread_id)
            queue.put(int_cmd)

    def checkOutput(self, out, outCtx):
        '''Checks the output to see if we have to send some buffered output to the debug server

        @param out: sys.stdout or sys.stderr
        @param outCtx: the context indicating: 1=stdout and 2=stderr (to know the colors to write it)
        '''

        try:
            v = out.getvalue()
            if v:
                self.cmdFactory.makeIoMessage(v, outCtx, self)
        except:
            traceback.print_exc()


    def processInternalCommands(self):
        '''This function processes internal commands
        '''
        curr_thread_id = GetThreadId(threadingCurrentThread())
        program_threads_alive = {}
        all_threads = threadingEnumerate()
        program_threads_dead = []


        self._main_lock.acquire()
        try:
            if bufferStdOutToServer:
                self.checkOutput(sys.stdoutBuf, 1) #@UndefinedVariable

            if bufferStdErrToServer:
                self.checkOutput(sys.stderrBuf, 2) #@UndefinedVariable

            self._lock_running_thread_ids.acquire()
            try:
                for t in all_threads:
                    thread_id = GetThreadId(t)

                    if not isinstance(t, PyDBDaemonThread) and t.isAlive():
                        program_threads_alive[thread_id] = t

                        if not DictContains(self._running_thread_ids, thread_id):
                            if not hasattr(t, 'additionalInfo'):
                                #see http://sourceforge.net/tracker/index.php?func=detail&aid=1955428&group_id=85796&atid=577329
                                #Let's create the additional info right away!
                                t.additionalInfo = PyDBAdditionalThreadInfo()
                            self._running_thread_ids[thread_id] = t
                            self.writer.addCommand(self.cmdFactory.makeThreadCreatedMessage(t))


                        queue = self.getInternalQueue(thread_id)
                        cmdsToReadd = []    #some commands must be processed by the thread itself... if that's the case,
                                            #we will re-add the commands to the queue after executing.
                        try:
                            while True:
                                int_cmd = queue.get(False)
                                if int_cmd.canBeExecutedBy(curr_thread_id):
                                    PydevdLog(2, "processing internal command ", str(int_cmd))
                                    int_cmd.doIt(self)
                                else:
                                    PydevdLog(2, "NOT processing internal command ", str(int_cmd))
                                    cmdsToReadd.append(int_cmd)

                        except PydevQueue.Empty: #@UndefinedVariable
                            for int_cmd in cmdsToReadd:
                                queue.put(int_cmd)
                            # this is how we exit


                thread_ids = list(self._running_thread_ids.keys())
                for tId in thread_ids:
                    if not DictContains(program_threads_alive, tId):
                        program_threads_dead.append(tId)
            finally:
                self._lock_running_thread_ids.release()

            for tId in program_threads_dead:
                try:
                    self.processThreadNotAlive(tId)
                except:
                    sys.stderr.write('Error iterating through %s (%s) - %s\n' % (
                        program_threads_alive, program_threads_alive.__class__, dir(program_threads_alive)))
                    raise


            if len(program_threads_alive) == 0:
                self.FinishDebuggingSession()
                for t in all_threads:
                    if hasattr(t, 'doKillPydevThread'):
                        t.doKillPydevThread()

        finally:
            self._main_lock.release()


    def setTracingForUntracedContexts(self):
        #Enable the tracing for existing threads (because there may be frames being executed that
        #are currently untraced).
        threads = threadingEnumerate()
        for t in threads:
            if not t.getName().startswith('pydevd.'):
                #TODO: optimize so that we only actually add that tracing if it's in
                #the new breakpoint context.
                additionalInfo = None
                try:
                    additionalInfo = t.additionalInfo
                except AttributeError:
                    pass #that's ok, no info currently set

                if additionalInfo is not None:
                    for frame in additionalInfo.IterFrames():
                        self.SetTraceForFrameAndParents(frame)
                        del frame


    def processNetCommand(self, cmd_id, seq, text):
        '''Processes a command received from the Java side

        @param cmd_id: the id of the command
        @param seq: the sequence of the command
        @param text: the text received in the command

        @note: this method is run as a big switch... after doing some tests, it's not clear whether changing it for
        a dict id --> function call will have better performance result. A simple test with xrange(10000000) showed
        that the gains from having a fast access to what should be executed are lost because of the function call in
        a way that if we had 10 elements in the switch the if..elif are better -- but growing the number of choices
        makes the solution with the dispatch look better -- so, if this gets more than 20-25 choices at some time,
        it may be worth refactoring it (actually, reordering the ifs so that the ones used mostly come before
        probably will give better performance).
        '''

        self._main_lock.acquire()
        try:
            try:
                cmd = None
                if cmd_id == CMD_RUN:
                    self.readyToRun = True

                elif cmd_id == CMD_VERSION:
                    # response is version number
                    cmd = self.cmdFactory.makeVersionMessage(seq)

                elif cmd_id == CMD_LIST_THREADS:
                    # response is a list of threads
                    cmd = self.cmdFactory.makeListThreadsMessage(seq)

                elif cmd_id == CMD_THREAD_KILL:
                    int_cmd = InternalTerminateThread(text)
                    self.postInternalCommand(int_cmd, text)

                elif cmd_id == CMD_THREAD_SUSPEND:
                    #Yes, thread suspend is still done at this point, not through an internal command!
                    t = PydevdFindThreadById(text)
                    if t:
                        additionalInfo = None
                        try:
                            additionalInfo = t.additionalInfo
                        except AttributeError:
                            pass #that's ok, no info currently set

                        if additionalInfo is not None:
                            for frame in additionalInfo.IterFrames():
                                self.SetTraceForFrameAndParents(frame)
                                del frame

                        self.setSuspend(t, CMD_THREAD_SUSPEND)

                elif cmd_id == CMD_THREAD_RUN:
                    t = PydevdFindThreadById(text)
                    if t:
                        thread_id = GetThreadId(t)
                        int_cmd = InternalRunThread(thread_id)
                        self.postInternalCommand(int_cmd, thread_id)

                elif cmd_id == CMD_STEP_INTO or cmd_id == CMD_STEP_OVER or cmd_id == CMD_STEP_RETURN:
                    #we received some command to make a single step
                    t = PydevdFindThreadById(text)
                    if t:
                        thread_id = GetThreadId(t)
                        int_cmd = InternalStepThread(thread_id, cmd_id)
                        self.postInternalCommand(int_cmd, thread_id)

                elif cmd_id == CMD_RUN_TO_LINE or cmd_id == CMD_SET_NEXT_STATEMENT:
                    #we received some command to make a single step
                    thread_id, line, func_name = text.split('\t', 2)
                    t = PydevdFindThreadById(thread_id)
                    if t:
                        int_cmd = InternalSetNextStatementThread(thread_id, cmd_id, line, func_name)
                        self.postInternalCommand(int_cmd, thread_id)


                elif cmd_id == CMD_RELOAD_CODE:
                    #we received some command to make a reload of a module
                    module_name = text.strip()
                    from pydevd_reload import xreload
                    if not DictContains(sys.modules, module_name):
                        if '.' in module_name:
                            new_module_name = module_name.split('.')[-1]
                            if DictContains(sys.modules, new_module_name):
                                module_name = new_module_name

                    if not DictContains(sys.modules, module_name):
                        sys.stderr.write('pydev debugger: Unable to find module to reload: "' + module_name + '".\n')
                        sys.stderr.write('pydev debugger: This usually means you are trying to reload the __main__ module (which cannot be reloaded).\n')

                    else:
                        sys.stderr.write('pydev debugger: Reloading: ' + module_name + '\n')
                        xreload(sys.modules[module_name])


                elif cmd_id == CMD_CHANGE_VARIABLE:
                    #the text is: thread\tstackframe\tFRAME|GLOBAL\tattribute_to_change\tvalue_to_change
                    try:
                        thread_id, frame_id, scope, attr_and_value = text.split('\t', 3)

                        tab_index = attr_and_value.rindex('\t')
                        attr = attr_and_value[0:tab_index].replace('\t', '.')
                        value = attr_and_value[tab_index + 1:]
                        int_cmd = InternalChangeVariable(seq, thread_id, frame_id, scope, attr, value)
                        self.postInternalCommand(int_cmd, thread_id)

                    except:
                        traceback.print_exc()

                elif cmd_id == CMD_GET_VARIABLE:
                    #we received some command to get a variable
                    #the text is: thread_id\tframe_id\tFRAME|GLOBAL\tattributes*
                    try:
                        thread_id, frame_id, scopeattrs = text.split('\t', 2)

                        if scopeattrs.find('\t') != -1: # there are attributes beyond scope
                            scope, attrs = scopeattrs.split('\t', 1)
                        else:
                            scope, attrs = (scopeattrs, None)

                        int_cmd = InternalGetVariable(seq, thread_id, frame_id, scope, attrs)
                        self.postInternalCommand(int_cmd, thread_id)

                    except:
                        traceback.print_exc()

                elif cmd_id == CMD_GET_COMPLETIONS:
                    #we received some command to get a variable
                    #the text is: thread_id\tframe_id\tactivation token
                    try:
                        thread_id, frame_id, scope, act_tok = text.split('\t', 3)

                        int_cmd = InternalGetCompletions(seq, thread_id, frame_id, act_tok)
                        self.postInternalCommand(int_cmd, thread_id)

                    except:
                        traceback.print_exc()

                elif cmd_id == CMD_GET_FRAME:
                    thread_id, frame_id, scope = text.split('\t', 2)

                    int_cmd = InternalGetFrame(seq, thread_id, frame_id)
                    self.postInternalCommand(int_cmd, thread_id)

                elif cmd_id == CMD_SET_BREAK:
                    #func name: 'None': match anything. Empty: match global, specified: only method context.

                    #command to add some breakpoint.
                    # text is file\tline. Add to breakpoints dictionary
                    file, line, condition = text.split('\t', 2)
                    if condition.startswith('**FUNC**'):
                        func_name, condition = condition.split('\t', 1)

                        #We must restore new lines and tabs as done in 
                        #AbstractDebugTarget.breakpointAdded
                        condition = condition.replace("@_@NEW_LINE_CHAR@_@", '\n').\
                            replace("@_@TAB_CHAR@_@", '\t').strip()

                        func_name = func_name[8:]
                    else:
                        func_name = 'None' #Match anything if not specified.


                    file = NormFileToServer(file)

                    if not os.path.exists(file):
                        sys.stderr.write('pydev debugger: warning: trying to add breakpoint'\
                            ' to file that does not exist: %s (will have no effect)\n' % (file,))

                    line = int(line)

                    if DEBUG_TRACE_BREAKPOINTS > 0:
                        sys.stderr.write('Added breakpoint:%s - line:%s - func_name:%s\n' % (file, line, func_name))

                    if DictContains(self.breakpoints, file):
                        breakDict = self.breakpoints[file]
                    else:
                        breakDict = {}

                    if len(condition) <= 0 or condition == None or condition == "None":
                        breakDict[line] = (True, None, func_name)
                    else:
                        breakDict[line] = (True, condition, func_name)


                    self.breakpoints[file] = breakDict
                    self.setTracingForUntracedContexts()

                elif cmd_id == CMD_REMOVE_BREAK:
                    #command to remove some breakpoint
                    #text is file\tline. Remove from breakpoints dictionary
                    file, line = text.split('\t', 1)
                    file = NormFileToServer(file)
                    try:
                        line = int(line)
                    except ValueError:
                        pass

                    else:
                        try:
                            del self.breakpoints[file][line] #remove the breakpoint in that line
                            if DEBUG_TRACE_BREAKPOINTS > 0:
                                sys.stderr.write('Removed breakpoint:%s\n' % (file,))
                        except KeyError:
                            #ok, it's not there...
                            if DEBUG_TRACE_BREAKPOINTS > 0:
                                #Sometimes, when adding a breakpoint, it adds a remove command before (don't really know why)
                                sys.stderr.write("breakpoint not found: %s - %s\n" % (file, line))

                elif cmd_id == CMD_EVALUATE_EXPRESSION or cmd_id == CMD_EXEC_EXPRESSION:
                    #command to evaluate the given expression
                    #text is: thread\tstackframe\tLOCAL\texpression
                    thread_id, frame_id, scope, expression = text.split('\t', 3)
                    int_cmd = InternalEvaluateExpression(seq, thread_id, frame_id, expression,
                        cmd_id == CMD_EXEC_EXPRESSION)
                    self.postInternalCommand(int_cmd, thread_id)

                elif cmd_id == CMD_SET_PY_EXCEPTION:
                    # Command which receives set of exceptions on which user wants to break the debugger
                    # text is: break_on_uncaught;break_on_caught;TypeError;ImportError;zipimport.ZipImportError;
                    splitted = text.split(';')
                    if len(splitted) >= 2:


                        if splitted[0] == 'true':
                            break_on_uncaught = True
                        else:
                            break_on_uncaught = False


                        if splitted[1] == 'true':
                            break_on_caught = True
                        else:
                            break_on_caught = False

                        handle_exceptions = []
                        for exception_type in splitted[2:]:
                            exception_type = exception_type.strip()
                            if not exception_type:
                                continue

                            try:
                                handle_exceptions.append(eval(exception_type))
                            except:
                                try:
                                    handle_exceptions.append(pydevd_import_class.ImportName(exception_type))
                                except:
                                    sys.stderr.write("Unable to Import: %s when determining exceptions to break.\n" % (exception_type,))

                        if DEBUG_TRACE_BREAKPOINTS > 0:
                            sys.stderr.write("Exceptions to hook : %s\n" % (handle_exceptions,))

                        self.setExceptHook(tuple(handle_exceptions), break_on_uncaught, break_on_caught)
                        self.setTracingForUntracedContexts()

                    else:
                        sys.stderr.write("Error when setting exception list. Received: %s\n" % (text,))

                elif cmd_id == CMD_GET_FILE_CONTENTS:
                    if os.path.exists(text):
                        f = open(text, 'r')
                        try:
                            source = f.read()
                        finally:
                            f.close()
                        cmd = self.cmdFactory.makeGetFileContents(seq, source)

                elif cmd_id == CMD_SET_PROPERTY_TRACE:
                    # Command which receives whether to trace property getter/setter/deleter
                    # text is feature_state(true/false);disable_getter/disable_setter/disable_deleter
                    if text != "":
                        splitted = text.split(';')
                        if len(splitted) >= 3:
                            if self.disable_property_trace is False and splitted[0] == 'true':
                                # Replacing property by custom property only when the debugger starts
                                pydevd_traceproperty.replace_builtin_property()
                                self.disable_property_trace = True
                            # Enable/Disable tracing of the property getter
                            if splitted[1] == 'true':
                                self.disable_property_getter_trace = True
                            else:
                                self.disable_property_getter_trace = False
                            # Enable/Disable tracing of the property setter
                            if splitted[2] == 'true':
                                self.disable_property_setter_trace = True
                            else:
                                self.disable_property_setter_trace = False
                            # Enable/Disable tracing of the property deleter
                            if splitted[3] == 'true':
                                self.disable_property_deleter_trace = True
                            else:
                                self.disable_property_deleter_trace = False
                    else:
                        # User hasn't configured any settings for property tracing
                        pass

                elif cmd_id == CMD_EVALUATE_CONSOLE_EXPRESSION:
                    # Command which takes care for the debug console communication
                    if text != "":
                        thread_id, frame_id, console_command = text.split('\t', 2)
                        console_command, line = console_command.split('\t')
                        if console_command == 'EVALUATE':
                            int_cmd = InternalEvaluateConsoleExpression(seq, thread_id, frame_id, line)
                        elif console_command == 'GET_COMPLETIONS':
                            int_cmd = InternalConsoleGetCompletions(seq, thread_id, frame_id, line)
                        self.postInternalCommand(int_cmd, thread_id)

                else:
                    #I have no idea what this is all about
                    cmd = self.cmdFactory.makeErrorMessage(seq, "unexpected command " + str(cmd_id))

                if cmd is not None:
                    self.writer.addCommand(cmd)
                    del cmd

            except Exception:
                traceback.print_exc()
                cmd = self.cmdFactory.makeErrorMessage(seq,
                    "Unexpected exception in processNetCommand.\nInitial params: %s" % ((cmd_id, seq, text),))

                self.writer.addCommand(cmd)
        finally:
            self._main_lock.release()


    def setExceptHook(self, handle_exceptions, break_on_uncaught, break_on_caught):
        '''
        Should be called to set the exceptions to be handled and whether it should break on uncaught and
        caught exceptions.

        Can receive a parameter to stop only on some exceptions.

        E.g.:
            set_pm_excepthook((IndexError, ValueError), True, True)

            or

            set_pm_excepthook(IndexError, True, False)

            if passed without a parameter, will break on any exception

        @param handle_exceptions: exception or tuple(exceptions)
            The exceptions that should be handled.

        @param break_on_uncaught bool
            Whether it should break on uncaught exceptions.

        @param break_on_caught: bool
            Whether it should break on caught exceptions.
        '''
        global _original_excepthook
        if sys.excepthook != excepthook:
            #Only keep the original if it's not our own excepthook (if called many times).
            _original_excepthook = sys.excepthook

        self.handle_exceptions = handle_exceptions

        #Note that we won't set to break if we don't have any exception to break on
        self.break_on_uncaught = handle_exceptions and break_on_uncaught
        self.break_on_caught = handle_exceptions and break_on_caught
        sys.excepthook = excepthook



    def processThreadNotAlive(self, threadId):
        """ if thread is not alive, cancel trace_dispatch processing """
        self._lock_running_thread_ids.acquire()
        try:
            thread = DictPop(self._running_thread_ids, threadId)
            if thread is None:
                return

            wasNotified = thread.additionalInfo.pydev_notify_kill
            if not wasNotified:
                thread.additionalInfo.pydev_notify_kill = True

        finally:
            self._lock_running_thread_ids.release()

        cmd = self.cmdFactory.makeThreadKilledMessage(threadId)
        self.writer.addCommand(cmd)


    def setSuspend(self, thread, stop_reason):
        thread.additionalInfo.pydev_state = STATE_SUSPEND
        thread.stop_reason = stop_reason


    def doWaitSuspend(self, thread, frame, event, arg): #@UnusedVariable
        """ busy waits until the thread state changes to RUN
        it expects thread's state as attributes of the thread.
        Upon running, processes any outstanding Stepping commands.
        """
        self.processInternalCommands()
        cmd = self.cmdFactory.makeThreadSuspendMessage(GetThreadId(thread), frame, thread.stop_reason)
        self.writer.addCommand(cmd)

        info = thread.additionalInfo
        while info.pydev_state == STATE_SUSPEND and not self._finishDebuggingSession:
            self.processInternalCommands()
            time.sleep(0.01)

        #process any stepping instructions 
        if info.pydev_step_cmd == CMD_STEP_INTO:
            info.pydev_step_stop = None

        elif info.pydev_step_cmd == CMD_STEP_OVER:
            info.pydev_step_stop = frame
            self.SetTraceForFrameAndParents(frame)

        elif info.pydev_step_cmd == CMD_RUN_TO_LINE or info.pydev_step_cmd == CMD_SET_NEXT_STATEMENT :
            self.SetTraceForFrameAndParents(frame)

            if event == 'line' or event == 'exception':
                #If we're already in the correct context, we have to stop it now, because we can act only on
                #line events -- if a return was the next statement it wouldn't work (so, we have this code
                #repeated at pydevd_frame). 
                stop = False
                curr_func_name = frame.f_code.co_name

                #global context is set with an empty name
                if curr_func_name in ('?', '<module>'):
                    curr_func_name = ''

                if curr_func_name == info.pydev_func_name:
                    line = info.pydev_next_line
                    if frame.f_lineno == line:
                        stop = True
                    else:
                        if frame.f_trace is None:
                            frame.f_trace = self.trace_dispatch
                        frame.f_lineno = line
                        frame.f_trace = None
                        stop = True
                if stop:
                    info.pydev_state = STATE_SUSPEND
                    self.doWaitSuspend(thread, frame, event, arg)
                    return


        elif info.pydev_step_cmd == CMD_STEP_RETURN:
            back_frame = frame.f_back
            if back_frame is not None:
                #steps back to the same frame (in a return call it will stop in the 'back frame' for the user)
                info.pydev_step_stop = frame
                self.SetTraceForFrameAndParents(frame)
            else:
                #No back frame?!? -- this happens in jython when we have some frame created from an awt event
                #(the previous frame would be the awt event, but this doesn't make part of 'jython', only 'java')
                #so, if we're doing a step return in this situation, it's the same as just making it run
                info.pydev_step_stop = None
                info.pydev_step_cmd = None
                info.pydev_state = STATE_RUN

        del frame
        cmd = self.cmdFactory.makeThreadRunMessage(GetThreadId(thread), info.pydev_step_cmd)
        self.writer.addCommand(cmd)



    def trace_dispatch(self, frame, event, arg):
        ''' This is the callback used when we enter some context in the debugger.

        We also decorate the thread we are in with info about the debugging.
        The attributes added are:
            pydev_state
            pydev_step_stop
            pydev_step_cmd
            pydev_notify_kill
        '''
        try:
            if self._finishDebuggingSession:
                #that was not working very well because jython gave some socket errors
                threads = threadingEnumerate()
                for t in threads:
                    if hasattr(t, 'doKillPydevThread'):
                        t.doKillPydevThread()
                return None

            filename, base = GetFilenameAndBase(frame)

            is_file_to_ignore = DictContains(DONT_TRACE, base) #we don't want to debug threading or anything related to pydevd

            if not self.force_post_mortem_stop: #If we're in post mortem mode, we might not have another chance to show that info!
                if is_file_to_ignore:
                    return None

            #print('trace_dispatch', base, frame.f_lineno, event, frame.f_code.co_name)
            try:
                #this shouldn't give an exception, but it could happen... (python bug)
                #see http://mail.python.org/pipermail/python-bugs-list/2007-June/038796.html
                #and related bug: http://bugs.python.org/issue1733757
                t = threadingCurrentThread()
            except:
                frame.f_trace = self.trace_dispatch
                return self.trace_dispatch

            try:
                additionalInfo = t.additionalInfo
            except:
                additionalInfo = t.additionalInfo = PyDBAdditionalThreadInfo()

            if self.force_post_mortem_stop: #If we're in post mortem mode, we might not have another chance to show that info!
                if additionalInfo.pydev_force_stop_at_exception:
                    self.force_post_mortem_stop -= 1
                    frame, frames_byid = additionalInfo.pydev_force_stop_at_exception
                    thread_id = GetThreadId(t)
                    used_id = pydevd_vars.addAdditionalFrameById(thread_id, frames_byid)
                    try:
                        self.setSuspend(t, CMD_STEP_INTO)
                        self.doWaitSuspend(t, frame, 'exception', None)
                    finally:
                        additionalInfo.pydev_force_stop_at_exception = None
                        pydevd_vars.removeAdditionalFrameById(thread_id)

            # if thread is not alive, cancel trace_dispatch processing
            if not t.isAlive():
                self.processThreadNotAlive(GetThreadId(t))
                return None # suspend tracing

            if is_file_to_ignore:
                return None

            #each new frame...
            return additionalInfo.CreateDbFrame((self, filename, additionalInfo, t, frame)).trace_dispatch(frame, event, arg)

        except SystemExit:
            return None

        except Exception:
            #Log it
            if traceback is not None:
                #This can actually happen during the interpreter shutdown in Python 2.7
                traceback.print_exc()
            return None

    if USE_PSYCO_OPTIMIZATION:
        try:
            import psyco
            trace_dispatch = psyco.proxy(trace_dispatch)
            processNetCommand = psyco.proxy(processNetCommand)
            processInternalCommands = psyco.proxy(processInternalCommands)
            doWaitSuspend = psyco.proxy(doWaitSuspend)
            getInternalQueue = psyco.proxy(getInternalQueue)
        except ImportError:
            if hasattr(sys, 'exc_clear'): #jython does not have it
                sys.exc_clear() #don't keep the traceback (let's keep it clear for when we go to the point of executing client code)

            if not IS_PY3K and not IS_PY27 and not IS_64_BITS and not sys.platform.startswith("java") and not sys.platform.startswith("cli"):
                sys.stderr.write("pydev debugger: warning: psyco not available for speedups (the debugger will still work correctly, but a bit slower)\n")



    def SetTraceForFrameAndParents(self, frame, also_add_to_passed_frame=True):
        dispatch_func = self.trace_dispatch

        if also_add_to_passed_frame:
            if frame.f_trace is None:
                frame.f_trace = dispatch_func
            else:
                try:
                    #If it's the trace_exception, go back to the frame trace dispatch!
                    if frame.f_trace.im_func.__name__ == 'trace_exception':
                        frame.f_trace = frame.f_trace.im_self.trace_dispatch
                except AttributeError:
                    pass

        frame = frame.f_back
        while frame:
            if frame.f_trace is None:
                frame.f_trace = dispatch_func
            else:
                try:
                    #If it's the trace_exception, go back to the frame trace dispatch!
                    if frame.f_trace.im_func.__name__ == 'trace_exception':
                        frame.f_trace = frame.f_trace.im_self.trace_dispatch
                except AttributeError:
                    pass
            frame = frame.f_back
        del frame

    def prepareToRun(self):
        ''' Shared code to prepare debugging by installing traces and registering threads '''

        # for completeness, we'll register the pydevd.reader & pydevd.writer threads
        net = NetCommand(str(CMD_THREAD_CREATE), 0, '<xml><thread name="pydevd.reader" id="-1"/></xml>')
        self.writer.addCommand(net)
        net = NetCommand(str(CMD_THREAD_CREATE), 0, '<xml><thread name="pydevd.writer" id="-1"/></xml>')
        self.writer.addCommand(net)

        pydevd_tracing.SetTrace(self.trace_dispatch)
        try:
            #not available in jython!
            threading.settrace(self.trace_dispatch) # for all future threads
        except:
            pass

        try:
            thread.start_new_thread = pydev_start_new_thread
            thread.start_new = pydev_start_new_thread
        except:
            pass

        PyDBCommandThread(self).start()

    def run(self, file, globals=None, locals=None, set_trace=True):

        if globals is None:
            #patch provided by: Scott Schlesier - when script is run, it does not 
            #use globals from pydevd:
            #This will prevent the pydevd script from contaminating the namespace for the script to be debugged

            #pretend pydevd is not the main module, and
            #convince the file to be debugged that it was loaded as main
            sys.modules['pydevd'] = sys.modules['__main__']
            sys.modules['pydevd'].__name__ = 'pydevd'

            from imp import new_module
            m = new_module('__main__')
            sys.modules['__main__'] = m
            m.__file__ = file
            globals = m.__dict__
            try:
                globals['__builtins__'] = __builtins__
            except NameError:
                pass #Not there on Jython...

        if locals is None:
            locals = globals

        if set_trace:
            #Predefined (writable) attributes: __name__ is the module's name; 
            #__doc__ is the module's documentation string, or None if unavailable; 
            #__file__ is the pathname of the file from which the module was loaded, 
            #if it was loaded from a file. The __file__ attribute is not present for 
            #C modules that are statically linked into the interpreter; for extension modules 
            #loaded dynamically from a shared library, it is the pathname of the shared library file. 
    
    
            #I think this is an ugly hack, bug it works (seems to) for the bug that says that sys.path should be the same in
            #debug and run.
            if m.__file__.startswith(sys.path[0]):
                #print >> sys.stderr, 'Deleting: ', sys.path[0]
                del sys.path[0]
    
            #now, the local directory has to be added to the pythonpath
            #sys.path.insert(0, os.getcwd())
            #Changed: it's not the local directory, but the directory of the file launched
            #The file being run ust be in the pythonpath (even if it was not before)
            sys.path.insert(0, os.path.split(file)[0])
    
            self.prepareToRun()

            while not self.readyToRun:
                time.sleep(0.1) # busy wait until we receive run command
    

        pydev_imports.execfile(file, globals, locals) #execute the script


def processCommandLine(argv):
    """ parses the arguments.
        removes our arguments from the command line """
    retVal = {}
    retVal['client'] = ''
    retVal['server'] = False
    retVal['port'] = 0
    retVal['file'] = ''
    i = 0
    del argv[0]
    while (i < len(argv)):
        if (argv[i] == '--port'):
            del argv[i]
            retVal['port'] = int(argv[i])
            del argv[i]
        elif (argv[i] == '--vm_type'):
            del argv[i]
            retVal['vm_type'] = argv[i]
            del argv[i]
        elif (argv[i] == '--client'):
            del argv[i]
            retVal['client'] = argv[i]
            del argv[i]
        elif (argv[i] == '--server'):
            del argv[i]
            retVal['server'] = True
        elif (argv[i] == '--file'):
            del argv[i]
            retVal['file'] = argv[i];
            i = len(argv) # pop out, file is our last argument
        elif (argv[i] == '--DEBUG_RECORD_SOCKET_READS'):
            del argv[i]
            retVal['DEBUG_RECORD_SOCKET_READS'] = True
        else:
            raise ValueError("unexpected option " + argv[i])
    return retVal

def usage(doExit=0):
    sys.stdout.write('Usage:\n')
    sys.stdout.write('pydevd.py --port=N [(--client hostname) | --server] --file executable [file_options]\n')
    if doExit:
        sys.exit(0)



#=======================================================================================================================
# patch_django_autoreload
#=======================================================================================================================
def patch_django_autoreload(patch_remote_debugger=True, patch_show_console=True):
    '''
    Patch Django to work with remote debugger without adding an explicit
    pydevd.settrace to set a breakpoint (i.e.: it'll setup the remote debugger machinery
    and don't suspend now -- this will load the breakpoints and will listen to
    changes in them so that we do stop on the breakpoints set in the editor).

    Checked with with Django 1.2.5.
    Checked with with Django 1.3.
    Checked with with Django 1.4.

    @param patch_remote_debugger: if True, the debug tracing mechanism will be put into place.

    @param patch_show_console: if True, each new process created in Django will allocate a new console
                               outside of Eclipse (so, it can be killed with a Ctrl+C in that console).
                               Note: when on Linux, even Ctrl+C will do a reload, so, the parent process
                               (inside Eclipse) must be killed before issuing the Ctrl+C (see TODO in code).
    '''
    if 'runserver' in sys.argv or 'testserver' in sys.argv:

        from django.utils import autoreload

        if patch_remote_debugger:
            original_main = autoreload.main

            def main(main_func, args=None, kwargs=None):

                if os.environ.get("RUN_MAIN") == "true":
                    original_main_func = main_func

                    def pydev_debugger_main_func(*args, **kwargs):
                        settrace(
                            suspend=False, #Don't suspend now (but put the debugger structure in place).
                            trace_only_current_thread=False, #Trace any created thread.
                        )
                        return original_main_func(*args, **kwargs)

                    main_func = pydev_debugger_main_func

                return original_main(main_func, args, kwargs)

            autoreload.main = main


        if patch_show_console:
            def restart_with_reloader():
                import subprocess
                create_new_console_supported = hasattr(subprocess, 'CREATE_NEW_CONSOLE')
                if not create_new_console_supported:
                    sys.stderr.write('Warning: to actually kill the created console, the parent process (in Eclipse console) must be killed first.\n')

                while True:
                    args = [sys.executable] + ['-W%s' % o for o in sys.warnoptions] + sys.argv
                    sys.stdout.write('Executing process on new console: %s\n' % (' '.join(args),))

                    #Commented out: not needed with Popen (in fact, it fails if that's done).
                    #if sys.platform == "win32":
                    #    args = ['"%s"' % arg for arg in args]

                    new_environ = os.environ.copy()
                    new_environ["RUN_MAIN"] = 'true'

                    #Changed to Popen variant so that the creation flag can be passed.
                    #exit_code = os.spawnve(os.P_WAIT, sys.executable, args, new_environ)
                    if create_new_console_supported:
                        popen = subprocess.Popen(args, env=new_environ, creationflags=subprocess.CREATE_NEW_CONSOLE)
                        exit_code = popen.wait()
                    else:
                        #On Linux, CREATE_NEW_CONSOLE is not available, thus, we use xterm itself. There is a problem
                        #here: xterm does not return the return code of the executable, so, we keep things running all
                        #the time, even when Ctrl+c is issued (which means that the user must first stop the parent
                        #process and only after that do a Ctrl+C in the terminal).
                        #
                        #TODO: It should be possible to create a 'wrapper' program to store this value and then read it
                        #to know if Ctrl+C was indeed used or a reload took place, but this is kept for the future :)
                        args = ['xterm', '-e'] + args
                        popen = subprocess.Popen(args, env=new_environ)
                        popen.wait() #This exit code will always be 0 when xterm is executed.
                        exit_code = 3

                    #Kept the same
                    if exit_code != 3:
                        return exit_code

            autoreload.restart_with_reloader = restart_with_reloader


#=======================================================================================================================
# settrace
#=======================================================================================================================
def settrace(host=None, stdoutToServer=False, stderrToServer=False, port=5678, suspend=True, trace_only_current_thread=True):
    '''Sets the tracing function with the pydev debug function and initializes needed facilities.

    @param host: the user may specify another host, if the debug server is not in the same machine (default is the local host)
    @param stdoutToServer: when this is true, the stdout is passed to the debug server
    @param stderrToServer: when this is true, the stderr is passed to the debug server
        so that they are printed in its console and not in this process console.
    @param port: specifies which port to use for communicating with the server (note that the server must be started
        in the same port). @note: currently it's hard-coded at 5678 in the client
    @param suspend: whether a breakpoint should be emulated as soon as this function is called.
    @param trace_only_current_thread: determines if only the current thread will be traced or all future threads will also have the tracing enabled.
    '''
    _set_trace_lock.acquire()
    try:
        _locked_settrace(host, stdoutToServer, stderrToServer, port, suspend, trace_only_current_thread)
    finally:
        _set_trace_lock.release()



_set_trace_lock = threading.Lock()

def _locked_settrace(host, stdoutToServer, stderrToServer, port, suspend, trace_only_current_thread):
    if host is None:
        import pydev_localhost
        host = pydev_localhost.get_localhost()

    global connected
    global bufferStdOutToServer
    global bufferStdErrToServer

    if not connected :
        connected = True
        bufferStdOutToServer = stdoutToServer
        bufferStdErrToServer = stderrToServer

        pydevd_vm_type.SetupType()

        debugger = PyDB()
        debugger.connect(host, port)

        net = NetCommand(str(CMD_THREAD_CREATE), 0, '<xml><thread name="pydevd.reader" id="-1"/></xml>')
        debugger.writer.addCommand(net)
        net = NetCommand(str(CMD_THREAD_CREATE), 0, '<xml><thread name="pydevd.writer" id="-1"/></xml>')
        debugger.writer.addCommand(net)

        if bufferStdOutToServer:
            sys.stdoutBuf = pydevd_io.IOBuf()
            sys.stdout = pydevd_io.IORedirector(sys.stdout, sys.stdoutBuf) #@UndefinedVariable

        if bufferStdErrToServer:
            sys.stderrBuf = pydevd_io.IOBuf()
            sys.stderr = pydevd_io.IORedirector(sys.stderr, sys.stderrBuf) #@UndefinedVariable

        debugger.SetTraceForFrameAndParents(GetFrame(), False)

        t = threadingCurrentThread()
        try:
            additionalInfo = t.additionalInfo
        except AttributeError:
            additionalInfo = PyDBAdditionalThreadInfo()
            t.additionalInfo = additionalInfo

        while not debugger.readyToRun:
            time.sleep(0.1) # busy wait until we receive run command

        if suspend:
            debugger.setSuspend(t, CMD_SET_BREAK)

        #note that we do that through pydevd_tracing.SetTrace so that the tracing
        #is not warned to the user!
        pydevd_tracing.SetTrace(debugger.trace_dispatch)

        if not trace_only_current_thread:
            #Trace future threads?
            try:
                #not available in jython!  
                threading.settrace(debugger.trace_dispatch) # for all future threads
            except:
                pass

            try:
                thread.start_new_thread = pydev_start_new_thread
                thread.start_new = pydev_start_new_thread
            except:
                pass

        PyDBCommandThread(debugger).start()

    else:
        #ok, we're already in debug mode, with all set, so, let's just set the break
        debugger = GetGlobalDebugger()

        debugger.SetTraceForFrameAndParents(GetFrame(), False)

        t = threadingCurrentThread()
        try:
            additionalInfo = t.additionalInfo
        except AttributeError:
            additionalInfo = PyDBAdditionalThreadInfo()
            t.additionalInfo = additionalInfo

        pydevd_tracing.SetTrace(debugger.trace_dispatch)

        if not trace_only_current_thread:
            #Trace future threads?
            try:
                #not available in jython!  
                threading.settrace(debugger.trace_dispatch) # for all future threads
            except:
                pass

            try:
                thread.start_new_thread = pydev_start_new_thread
                thread.start_new = pydev_start_new_thread
            except:
                pass

        if suspend:
            debugger.setSuspend(t, CMD_SET_BREAK)


#=======================================================================================================================
# main
#=======================================================================================================================
if __name__ == '__main__':
    sys.stderr.write("pydev debugger: starting\n")
    # parse the command line. --file is our last argument that is required
    try:
        setup = processCommandLine(sys.argv)
    except ValueError:
        traceback.print_exc()
        usage(1)
        
        
    f = setup['file']
    fix_app_engine_debug = False
    if f.find('dev_appserver.py') != -1:
        if os.path.basename(f).startswith('dev_appserver.py'):
            appserver_dir = os.path.dirname(f)
            version_file = os.path.join(appserver_dir, 'VERSION')
            if os.path.exists(version_file):
                try:
                    stream = open(version_file, 'r')
                    try:
                        for line in stream.read().splitlines():
                            line = line.strip()
                            if line.startswith('release:'):
                                line = line[8:].strip()
                                version = line.replace('"', '')
                                version = version.split('.')
                                if int(version[0]) > 1:
                                    fix_app_engine_debug = True
                                    
                                elif int(version[0]) == 1:
                                    if int(version[1]) >= 7:
                                        # Only fix from 1.7 onwards
                                        fix_app_engine_debug = True
                                break
                    finally:
                        stream.close()
                except:
                    traceback.print_exc()
            
    if fix_app_engine_debug:
        sys.stderr.write("pydev debugger: google app engine integration enabled\n")
        curr_dir = os.path.dirname(__file__)
        app_engine_startup_file = os.path.join(curr_dir, 'pydev_app_engine_debug_startup.py')
        
        sys.argv.insert(1, '--python_startup_script='+app_engine_startup_file)
        import json
        setup['pydevd'] = __file__
        sys.argv.insert(2, '--python_startup_args=%s' % json.dumps(setup),)
        sys.argv.insert(3, '--automatic_restart=no')
        sys.argv.insert(4, '--max_module_instances=1')
        
        debugger = PyDB()
        #Run the dev_appserver
        debugger.run(setup['file'], None, None, set_trace=False)
        
    else:
        #as to get here all our imports are already resolved, the psyco module can be
        #changed and we'll still get the speedups in the debugger, as those functions 
        #are already compiled at this time.
        try:
            import psyco
        except ImportError:
            if hasattr(sys, 'exc_clear'): #jython does not have it
                sys.exc_clear() #don't keep the traceback -- clients don't want to see it
            pass #that's ok, no need to mock psyco if it's not available anyways
        else:
            #if it's available, let's change it for a stub (pydev already made use of it)
            import pydevd_psyco_stub
            sys.modules['psyco'] = pydevd_psyco_stub
    
    
        PydevdLog(2, "Executing file ", setup['file'])
        PydevdLog(2, "arguments:", str(sys.argv))
    
        pydevd_vm_type.SetupType(setup.get('vm_type', None))
    
        DebugInfoHolder.DEBUG_RECORD_SOCKET_READS = setup.get('DEBUG_RECORD_SOCKET_READS', False)
    
        debugger = PyDB()
        try:
            debugger.connect(setup['client'], setup['port'])
        except:
            sys.stderr.write("Could not connect to %s: %s\n" % (setup['client'], setup['port']))
            traceback.print_exc()
            sys.exit(1)
    
        connected = True #Mark that we're connected when started from inside eclipse.
    
        debugger.run(setup['file'], None, None)
    
