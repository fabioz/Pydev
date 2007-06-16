from pydevd_constants import * #@UnusedWildImport

from pydevd_comm import  CMD_CHANGE_VARIABLE,\
                         CMD_EVALUATE_EXPRESSION,\
                         CMD_EXEC_EXPRESSION,\
                         CMD_GET_FRAME,\
                         CMD_GET_VARIABLE,\
                         CMD_LIST_THREADS,\
                         CMD_REMOVE_BREAK,\
                         CMD_RUN,\
                         CMD_SET_BREAK,\
                         CMD_STEP_INTO,\
                         CMD_STEP_OVER,\
                         CMD_STEP_RETURN,\
                         CMD_THREAD_CREATE,\
                         CMD_THREAD_KILL,\
                         CMD_THREAD_RUN,\
                         CMD_THREAD_SUSPEND,\
                         CMD_VERSION,\
                         DebugInfoHolder,\
                         GetGlobalDebugger,\
                         InternalChangeVariable,\
                         InternalEvaluateExpression,\
                         InternalGetFrame,\
                         InternalGetVariable,\
                         InternalTerminateThread,\
                         NetCommand,\
                         NetCommandFactory,\
                         PyDBDaemonThread,\
                         PydevQueue,\
                         ReaderThread,\
                         SetGlobalDebugger,\
                         WriterThread,\
                         pydevd_findThreadById,\
                         pydevd_log,\
                         pydevd_trace_breakpoints,\
                         startClient,\
                         startServer

import os
import pydevd_file_utils
import sys
import threading 
import traceback 
import pydevd_vm_type 
import pydevd_tracing 
import pydevd_io
import pydevd_additional_thread_info
import time

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
              }

connected = False
bufferStdOutToServer = False
bufferStdErrToServer = False

PyDBUseLocks = True

     
#=======================================================================================================================
# PyDBCommandThread
#=======================================================================================================================
class PyDBCommandThread(PyDBDaemonThread):
    
    def __init__(self, pyDb):
        PyDBDaemonThread.__init__(self)
        self.pyDb = pyDb
        self.setName('pydevd.CommandThread')

    def run(self):
        time.sleep(5) #this one will only start later on (because otherwise we may not have any non-daemon threads
        pydevd_tracing.SetTrace(None) # no debugging on this thread
        try:
            while not self.killReceived:
                try:
                    self.pyDb.processInternalCommands()
                except:
                    pydevd_log(0, 'Finishing debug communication...(2)')
                time.sleep(0.5)
        except:
            pass
            #only got this error in interpreter shutdown
            #pydevd_log(0, 'Finishing debug communication...(3)')
            



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
    
    RUNNING_THREAD_IDS = {} #this is a dict of thread ids pointing to thread ids. Whenever a command
                            #is passed to the java end that acknowledges that a thread was created,
                            #the thread id should be passed here -- and if at some time we do not find
                            #that thread alive anymore, we must remove it from this list and make
                            #the java side know that the thread was killed.

    def __init__(self):
        SetGlobalDebugger(self)
        pydevd_tracing.ReplaceSysSetTraceFunc()
        self.reader = None
        self.writer = None
        self.quitting = None
        self.cmdFactory = NetCommandFactory() 
        self.cmdQueue = {}     # the hash of Queues. Key is thread id, value is thread
        self.breakpoints = {}
        self.readyToRun = False
        self.lock = threading.RLock()
        self.finishDebuggingSession = False
        
    def acquire(self):
        if PyDBUseLocks:
            self.lock.acquire()
        return True
    
    def release(self):
        if PyDBUseLocks:
            self.lock.release()
        return True
        
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
            s = startClient(host, port)
        else:
            s = startServer(port)
            
        self.initializeNetwork(s)
        

    def getInternalQueue(self, thread_id):
        """ returns intenal command queue for a given thread.
        if new queue is created, notify the RDB about it """
        thread_id = int(thread_id)
        try:
            return self.cmdQueue[thread_id]
        except KeyError:
            self.cmdQueue[thread_id] = PydevQueue.Queue()
            all_threads = threading.enumerate()
            cmd = None
            for t in all_threads:
                if id(t) == thread_id:
                    self.RUNNING_THREAD_IDS[thread_id] = t
                    cmd = self.cmdFactory.makeThreadCreatedMessage(t)
            if cmd:
                pydevd_log(2, "found a new thread " + str(thread_id))
                self.writer.addCommand(cmd)
            else:
                pydevd_log(0, "could not find thread by id to register")
                
        return self.cmdQueue[thread_id]
        
    def postInternalCommand(self, int_cmd, thread_id):
        """ if thread_id is *, post to all """
        if thread_id == "*":
            for k in self.cmdQueue.keys(): 
                self.cmdQueue[k].put(int_cmd)
                
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
        
        self.acquire()
        try:
            if bufferStdOutToServer:
                self.checkOutput(sys.stdoutBuf,1) #@UndefinedVariable
                    
            if bufferStdErrToServer:
                self.checkOutput(sys.stderrBuf,2) #@UndefinedVariable

            currThreadId = id(threading.currentThread())
            threads = threading.enumerate()
            foundNonPyDBDaemonThread = False
            foundThreads = {}
            
            for t in threads:
                tId = id(t)
                if t.isAlive():
                    foundThreads[tId] = tId
                    
                if not isinstance(t, PyDBDaemonThread):
                    foundNonPyDBDaemonThread = True
                    queue = self.getInternalQueue(id(t))
                    cmdsToReadd = [] #some commands must be processed by the thread itself... if that's the case,
                                     #we will re-add the commands to the queue after executing.
                    try:
                        while True:
                            int_cmd = queue.get(False)
                            if int_cmd.canBeExecutedBy(currThreadId):
                                pydevd_log(2, "processing internal command " + str(int_cmd))
                                int_cmd.doIt(self)
                            else:
                                pydevd_log(2, "NOT processign internal command " + str(int_cmd))
                                cmdsToReadd.append(int_cmd)
                                
                    except PydevQueue.Empty:
                        for int_cmd in cmdsToReadd:
                            queue.put(int_cmd)
                        # this is how we exit

            if not foundNonPyDBDaemonThread:
                self.finishDebuggingSession = True
                        
            for tId in self.RUNNING_THREAD_IDS.keys():
                try:
                    if tId not in foundThreads.keys():
                        self.processThreadNotAlive(tId)
                except:
                    print 'Error iterating through %s (%s) - %s' % (foundThreads, foundThreads.__class__, dir(foundThreads))
                    raise
                    
        finally:
            self.release()
      
    def processNetCommand(self, cmd_id, seq, text):
        '''Processes a command received from the Java side
        
        @param cmd_id: the id of the command
        @param seq: the sequence of the command
        @param text: the text received in the command
        '''

        self.acquire()
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
                    t = pydevd_findThreadById(text)
                    if t: 
                        additionalInfo = None
                        try:
                            additionalInfo = t.additionalInfo
                        except AttributeError:
                            pass #that's ok, no info currently set
                            
                        if additionalInfo is not None:
                            for frame in additionalInfo.IterFrames():
                                frame.f_trace = self.trace_dispatch
                                SetTraceForParents(frame, self.trace_dispatch)
                                del frame
                            
                        self.setSuspend(t, CMD_THREAD_SUSPEND)
    
                elif cmd_id  == CMD_THREAD_RUN:
                    t = pydevd_findThreadById(text)
                    if t: 
                        t.additionalInfo.pydev_step_cmd = None
                        t.additionalInfo.pydev_step_stop = None
                        t.additionalInfo.pydev_state = STATE_RUN
                        
                elif cmd_id == CMD_STEP_INTO or cmd_id == CMD_STEP_OVER or cmd_id == CMD_STEP_RETURN:
                    #we received some command to make a single step
                    t = pydevd_findThreadById(text)
                    if t:
                        t.additionalInfo.pydev_step_cmd = cmd_id
                        t.additionalInfo.pydev_state = STATE_RUN
                        
                        
                elif cmd_id == CMD_CHANGE_VARIABLE:
                    #the text is: thread\tstackframe\tFRAME|GLOBAL\tattribute_to_change\tvalue_to_change
                    try:
                        thread_id, frame_id, scope, attr_and_value = text.split('\t', 3)
                        thread_id = long(thread_id)
                        
                        tab_index = attr_and_value.rindex('\t')
                        attr = attr_and_value[0:tab_index].replace('\t', '.')
                        value = attr_and_value[tab_index+1:]
                        int_cmd = InternalChangeVariable(seq, thread_id, frame_id, scope, attr, value)
                        self.postInternalCommand(int_cmd, thread_id)
                            
                    except:
                        traceback.print_exc()
                    
                elif cmd_id == CMD_GET_VARIABLE:
                    #we received some command to get a variable
                    #the text is: thread\tstackframe\tFRAME|GLOBAL\tattributes*
                    try:
                        thread_id, frame_id, scopeattrs = text.split('\t', 2)
                        thread_id = long(thread_id)
                        
                        if scopeattrs.find('\t') != -1: # there are attibutes beyond scope
                            scope, attrs = scopeattrs.split('\t', 1)
                        else:
                            scope, attrs = (scopeattrs, None)
                        
                        int_cmd = InternalGetVariable(seq, thread_id, frame_id, scope, attrs)
                        self.postInternalCommand(int_cmd, thread_id)
                            
                    except:
                        traceback.print_exc()
                        
                elif cmd_id == CMD_GET_FRAME:
                    thread_id, frame_id, scope = text.split('\t', 2)
                    thread_id = long(thread_id)
                    
                    int_cmd = InternalGetFrame(seq, thread_id, frame_id)
                    self.postInternalCommand(int_cmd, thread_id)
                        
                elif cmd_id == CMD_SET_BREAK:
                    #command to add some breakpoint.
                    # text is file\tline. Add to breakpoints dictionary
                    file, line, condition = text.split( '\t', 2 )
                    if condition.startswith('**FUNC**'):
                        func_name, condition = condition.split('\t', 1)
                        func_name = func_name[8:]
                    else:
                        func_name = ''
                    
                    file = pydevd_file_utils.NormFile( file )
                    
                    line = int( line )
                    
                    if pydevd_trace_breakpoints > 0:
                        print 'Added breakpoint:%s - line:%s - func_name:%s' % (file, line, func_name)
                        
                    if self.breakpoints.has_key( file ):
                        breakDict = self.breakpoints[file]
                    else:
                        breakDict = {}
    
                    if len(condition) <= 0 or condition == None or condition == "None":
                        breakDict[line] = (True, None, func_name)
                    else:
                        breakDict[line] = (True, condition, func_name)
                    
                        
                    self.breakpoints[file] = breakDict
                    
                    #and enable the tracing for existing threads (because there may be frames being executed that
                    #are currently untraced).
                    threads = threading.enumerate()
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
                                    frame.f_trace = self.trace_dispatch
                                    SetTraceForParents(frame, self.trace_dispatch)
                                    del frame
                    
                elif cmd_id == CMD_REMOVE_BREAK:
                    #command to remove some breakpoint
                    #text is file\tline. Remove from breakpoints dictionary
                    file, line = text.split('\t', 1)
                    file = pydevd_file_utils.NormFile(file)
                    line = int(line)
                    
                    if self.breakpoints.has_key(file):
                        if pydevd_trace_breakpoints > 0:
                            print 'Removed breakpoint:%s' % (file)
                            
                        if self.breakpoints[file].has_key(line):
                            del self.breakpoints[file][line]
                            keys = self.breakpoints[file].keys()
                            if len(keys) is 0:
                                del self.breakpoints[file]
                        else:
                            pass
                            #Sometimes, when adding a breakpoint, it adds a remove command before (don't really know why)
                            #print >> sys.stderr, "breakpoint not found", file, str(line)
                            
                elif cmd_id == CMD_EVALUATE_EXPRESSION or cmd_id == CMD_EXEC_EXPRESSION:
                    #command to evaluate the given expression
                    #text is: thread\tstackframe\tLOCAL\texpression
                    thread_id, frame_id, scope, expression = text.split('\t', 3)
                    thread_id = long(thread_id)
                    int_cmd = InternalEvaluateExpression(seq, thread_id, frame_id, expression, cmd_id == CMD_EXEC_EXPRESSION)
                    self.postInternalCommand(int_cmd, thread_id)
                        
                        
                else:
                    #I have no idea what this is all about
                    cmd = self.cmdFactory.makeErrorMessage(seq, "unexpected command " + str(cmd_id))
                    
                if cmd is not None: 
                    self.writer.addCommand(cmd)
                    del cmd
                    
            except Exception, e:
                traceback.print_exc(e)
                traceback.print_exc()
                cmd = self.cmdFactory.makeErrorMessage(seq, "Unexpected exception in processNetCommand: %s\nInitial params: %s" % (str(e), (cmd_id, seq, text)))
                self.writer.addCommand(cmd)
        finally:
            self.release()

    def processThreadNotAlive(self, threadId):
        """ if thread is not alive, cancel trace_dispatch processing """
        thread = self.RUNNING_THREAD_IDS.get(threadId,None)
        if thread is None:
            return
        
        del self.RUNNING_THREAD_IDS[threadId]
        wasNotified = thread.additionalInfo.pydev_notify_kill
            
        if not wasNotified:
            cmd = self.cmdFactory.makeThreadKilledMessage(threadId)
            self.writer.addCommand(cmd)
            thread.additionalInfo.pydev_notify_kill = True

    def setSuspend(self, thread, stop_reason):
        thread.additionalInfo.pydev_state = STATE_SUSPEND
        thread.stop_reason = stop_reason

    def doWaitSuspend(self, thread, frame, event, arg): #@UnusedVariable
        """ busy waits until the thread state changes to RUN 
        it expects thread's state as attributes of the thread.
        Upon running, processes any outstanding Stepping commands.
        """
        self.processInternalCommands()
        cmd = self.cmdFactory.makeThreadSuspendMessage(id(thread), frame, thread.stop_reason)
        self.writer.addCommand(cmd)
        
        info = thread.additionalInfo
        while info.pydev_state == STATE_SUSPEND and not self.finishDebuggingSession:            
            self.processInternalCommands()
            time.sleep(0.2)
            
        #process any stepping instructions 
        if info.pydev_step_cmd == CMD_STEP_INTO:
            info.pydev_step_stop = None
            
        elif info.pydev_step_cmd == CMD_STEP_OVER:
            if event == 'return': # if we are returning from the function, stop in parent
                frame.f_back.f_trace = GetGlobalDebugger().trace_dispatch
                info.pydev_step_stop = frame.f_back
            else:
                info.pydev_step_stop = frame
                
        elif info.pydev_step_cmd == CMD_STEP_RETURN:
            frame.f_back.f_trace = GetGlobalDebugger().trace_dispatch
            info.pydev_step_stop = frame.f_back
 
        del frame
        cmd = self.cmdFactory.makeThreadRunMessage(id(thread), info.pydev_step_cmd)
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
            if self.finishDebuggingSession:
                #that was not working very well because jython gave some socket errors
                threads = threading.enumerate()
                for t in threads: 
                    if hasattr(t, 'doKill'):
                        t.doKill()
                time.sleep(0.2) #give them some time to release their locks...
                try:
                    import java.lang.System #@UnresolvedImport
                    java.lang.System.exit(0)
                except:
                    sys.exit(0)
    
            filename, base = pydevd_file_utils.GetFilenameAndBase(frame)
            #print 'trace_dispatch', base, frame.f_lineno, event, frame.f_code.co_name
    
            if DONT_TRACE.has_key(base): #we don't want to debug threading or anything related to pydevd
                return None
    
            try:
                #this shouldn't give an exception, but it could happen... (python bug
                #see http://sourceforge.net/tracker/index.php?func=detail&aid=1733757&group_id=5470&atid=105470 for details)
                t = threading.currentThread()
            except:
                frame.f_trace = self.trace_dispatch
                return self.trace_dispatch
            
            # if thread is not alive, cancel trace_dispatch processing
            if not t.isAlive():
                self.processThreadNotAlive(id(t))
                return None # suspend tracing
            
            try:
                additionalInfo = t.additionalInfo
            except AttributeError:
                additionalInfo = pydevd_additional_thread_info.PyDBAdditionalThreadInfo()
                t.additionalInfo = additionalInfo
            
            #always keep a reference to the topmost frame so that we're able to start tracing it (if it was untraced)
            #that's needed when a breakpoint is added in a current frame for a currently untraced context.
            
            #each new frame...
            dbFrame = additionalInfo.CreateDbFrame(self, filename, base, additionalInfo, t, frame)
            return dbFrame.trace_dispatch(frame, event, arg)
        except:
            traceback.print_exc()
            return None

    def run(self, file, globals=None, locals=None):

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

        if locals is None: 
            locals = globals        
            
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
        sys.path.insert(0, os.getcwd())
        # for completness, we'll register the pydevd.reader & pydevd.writer threads
        net = NetCommand(str(CMD_THREAD_CREATE), 0, '<xml><thread name="pydevd.reader" id="-1"/></xml>')
        self.writer.addCommand(net)
        net = NetCommand(str(CMD_THREAD_CREATE), 0, '<xml><thread name="pydevd.writer" id="-1"/></xml>')
        self.writer.addCommand(net)

        pydevd_tracing.SetTrace(self.trace_dispatch) 
        try:                     
            threading.settrace(self.trace_dispatch) # for all future threads           
        except:
            pass

        while not self.readyToRun: 
            time.sleep(0.1) # busy wait until we receive run command
            
        PyDBCommandThread(debugger).start()

        execfile(file, globals, locals) #execute the script


def processCommandLine(argv):
    """ parses the arguments.
        removes our arguments from the command line """
    retVal = {}
    retVal['client'] = ''
    retVal['server'] = False
    retVal['port'] = 0
    retVal['file'] = ''
    i=0
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
        elif (argv[i] == '--RECORD_SOCKET_READS'):
            del argv[i]            
            retVal['RECORD_SOCKET_READS'] = True
        else:
            raise ValueError, "unexpected option " + argv[i]
    return retVal

def usage(doExit=0):
    print 'Usage:'
    print 'pydevd.py --port=N [(--client hostname) | --server] --file executable [file_options]'
    if doExit:
        sys.exit(0)



def SetTraceForParents(frame, dispatch_func):
    frame = frame.f_back
    while frame:
        frame.f_trace = dispatch_func
        frame = frame.f_back
    del frame

def settrace(host='localhost', stdoutToServer = False, stderrToServer = False, port=5678, suspend=True):
    '''
    @param host: the user may specify another host, if the debug server is not in the same machine
    @param stdoutToServer: when this is true, the stdout is passed to the debug server
    @param stderrToServer: when this is true, the stderr is passed to the debug server
        so that they are printed in its console and not in this process console.
    @param port: specifies which port to use for communicating with the server (note that the server must be started 
        in the same port)
    @param suspend: whether a breakpoint should be emulated as soon as this function is called. 
    '''
    
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
            
        SetTraceForParents(sys._getframe(), debugger.trace_dispatch)
        
        t = threading.currentThread()      
        try:
            additionalInfo = t.additionalInfo
        except AttributeError:
            additionalInfo = pydevd_additional_thread_info.PyDBAdditionalThreadInfo()
            t.additionalInfo = additionalInfo
  
        if suspend:
            debugger.setSuspend(t, CMD_SET_BREAK)
        
        #that's right, debug only threads that pass through this function
        #(so, we just call sys.settrace and not threading.settrace)
        sys.settrace(debugger.trace_dispatch)
        PyDBCommandThread(debugger).start()
        
    else:
        #ok, we're already in debug mode, with all set, so, let's just set the break
        debugger = GetGlobalDebugger()
        
        SetTraceForParents(sys._getframe(), debugger.trace_dispatch)
        
        t = threading.currentThread()      
        try:
            additionalInfo = t.additionalInfo
        except AttributeError:
            additionalInfo = pydevd_additional_thread_info.PyDBAdditionalThreadInfo()
            t.additionalInfo = additionalInfo
            
        sys.settrace(debugger.trace_dispatch)
        if suspend:
            debugger.setSuspend(t, CMD_SET_BREAK)
    
if __name__ == '__main__':
    print >>sys.stderr, "pydev debugger"
    # parse the command line. --file is our last argument that is required
    try:
        setup = processCommandLine(sys.argv)
    except ValueError, e:
        print e
        usage(1)
        
    pydevd_log(2, "Executing file " + setup['file'])
    pydevd_log(2, "arguments:" + str(sys.argv))
 
    pydevd_vm_type.SetupType(setup.get('vm_type', None))
    
    DebugInfoHolder.RECORD_SOCKET_READS = setup.get('RECORD_SOCKET_READS', False)

    debugger = PyDB()
    debugger.connect(setup['client'], setup['port'])
    debugger.run(setup['file'], None, None)
    