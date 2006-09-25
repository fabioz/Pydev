import traceback
import weakref
from pydevd_comm import * #@UnusedWildImport
import pydevd_io

DONT_TRACE = ('pydevd.py' , 'threading.py', 'pydevd_vars.py', 'pydevd_comm.py')

connected = False
bufferStdOutToServer = False
bufferStdErrToServer = False

#this is because jython does not have staticmethods
PyDBCtx_threadToCtx = {}
PyDBCtx_Lock = threading.RLock()
PyDBCtx_Threads = {}#weakref.WeakValueDictionary()

PyDBCtx_UseLocks = False #I don't know why jython halts when using this synchronization...
PyDBCtxObjUseLocks = True
PyDBUseLocks = True

def PyDBCtx_LockAcquire():
    if PyDBCtx_UseLocks:
        PyDBCtx_Lock.acquire()
    return True
    
def PyDBCtx_LockRelease():
    if PyDBCtx_UseLocks:
        PyDBCtx_Lock.release()
    return True
    
def PyDBCtx_GetCtxs():
    PyDBCtx_LockAcquire()
    try:
        ret = []
        for v in PyDBCtx_threadToCtx.values():
            ret += v.values()
    finally:
        PyDBCtx_LockRelease()
    return ret

def PyDBCtx_GetCtx(frame, currThread):
    PyDBCtx_LockAcquire()
    try:
        #keep the last frame for each thread (previously we were keeping all the frames, in the context,
        #but that was a bad thing, as things in the frames did not die).
        threadId = id(currThread)
        PyDBCtx_Threads[threadId] = weakref.ref(currThread)
        currThread._lastFrame = frame
        
        #we create a context for each thread
        ctxs = PyDBCtx_threadToCtx.get(threadId)
        if ctxs is None:
            ctxs = {}
            PyDBCtx_threadToCtx[threadId] = ctxs
            
        #and for each thread, the code for the frame
        key = frame.f_code
        ctx = ctxs.get(key)
        if ctx is None:
            ctx = PyDBCtx(frame, threadId)
            ctxs[key] = ctx
    finally:
        PyDBCtx_LockRelease()

    return ctx

def PyDBCtx_SetTraceForAllFileCtxs(f):
    PyDBCtx_LockAcquire()
    try:
        g = GetGlobalDebugger()
        if g:
            for ctx in PyDBCtx_GetCtxs():
                if ctx.filename == f:
                    thread = PyDBCtx_Threads.get(ctx.thread_id)()#it is a weakref
                    if thread is not None:
                        frame = thread._lastFrame
                        while frame:
                            currFile = NormFile(frame.f_code.co_filename)
                            if f == currFile:
                                frame.f_trace = g.trace_dispatch
                            frame = frame.f_back
    finally:
        PyDBCtx_LockRelease()


class PyDBCtx:
    '''This class is used to keep track of the contexts we pass through (acting as a cache for them).
    '''
            
    def __init__(self, frame, threadId):
        self.filename = NormFile(frame.f_code.co_filename)
        self.base = os.path.basename( self.filename )
        self.thread_id = threadId
        self.lock = threading.RLock()
        
    def __str__(self):
        return 'PyDBCtx [%s %s %s]' % (self.base, self.thread_id)
    
    def acquire(self):
        if PyDBCtxObjUseLocks:
            self.lock.acquire()
        return True
        
    def release(self):
        if PyDBCtxObjUseLocks:
            self.lock.release()
        return True
        
class PyDBCommandThread(PyDBDaemonThread):
    
    def __init__(self, pyDb):
        PyDBDaemonThread.__init__(self)
        self.pyDb = pyDb

    def run(self):
        time.sleep(5) #this one will only start later on (because otherwise we may not have any non-daemon threads
        sys.settrace(None) # no debugging on this thread
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
            

class PyDBAdditionalThreadInfo:
    def __init__(self):
        self.pydev_state = PyDB.STATE_RUN 
        self.pydev_step_stop = None
        self.pydev_step_cmd = None
        self.pydev_notify_kill = False
        self.pydev_stop_on_return_count_1 = False
        self.pydev_return_call_count = 0

#---------------------------------------------------------------------------------------- THIS IS THE DEBUGGER

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
    
    STATE_RUN = 1
    STATE_SUSPEND = 2 # thread states
    RUNNING_THREAD_IDS = {} #this is a dict of thread ids pointing to thread ids. Whenever a command
                            #is passed to the java end that acknowledges that a thread was created,
                            #the thread id should be passed here -- and if at some time we do not find
                            #that thread alive anymore, we must remove it from this list and make
                            #the java side know that the thread was killed.

    def __init__(self):
        SetGlobalDebugger(self)
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

    def checkOutput(self, out, ctx):
        '''Checks the output to see if we have to send some buffered output to the debug server
        
        @param out: sys.stdout or sys.stderr
        @param ctx: the context indicating: 1=stdout and 2=stderr (to know the colors to write it)
        '''
        
        try:
            v = out.getvalue()
            if v:
                self.cmdFactory.makeIoMessage(v, ctx, self)
        except:
            traceback.print_exc()
        

    def processInternalCommands(self):
        '''This function processes internal commands
        '''
        
        self.acquire()
        try:
            if bufferStdOutToServer:
                self.checkOutput(sys.stdoutBuf,1)
                    
            if bufferStdErrToServer:
                self.checkOutput(sys.stderrBuf,2)

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
      
    def processNetCommand(self, id, seq, text):
        '''Processes a command received from the Java side
        
        @param id: the id of the command
        @param seq: the sequence of the command
        @param text: the text received in the command
        '''
        
        self.acquire()
        try:
            try:
                cmd = None
                if id == CMD_RUN:
                    self.readyToRun = True
                    
                elif id == CMD_VERSION: 
                    # response is version number
                    cmd = self.cmdFactory.makeVersionMessage(seq)
                    
                elif id == CMD_LIST_THREADS: 
                    # response is a list of threads
                    cmd = self.cmdFactory.makeListThreadsMessage(seq)
                    
                elif id == CMD_THREAD_KILL:
                    int_cmd = InternalTerminateThread(text)
                    self.postInternalCommand(int_cmd, text)
                    
                elif id == CMD_THREAD_SUSPEND:
                    t = pydevd_findThreadById(text)
                    if t: 
                        self.setSuspend(t, CMD_THREAD_SUSPEND)
    
                elif id  == CMD_THREAD_RUN:
                    t = pydevd_findThreadById(text)
                    if t: 
                        t.additionalInfo.pydev_step_cmd = None
                        t.additionalInfo.pydev_state = PyDB.STATE_RUN
                        
                elif id == CMD_STEP_INTO or id == CMD_STEP_OVER or id == CMD_STEP_RETURN:
                    #we received some command to make a single step
                    t = pydevd_findThreadById(text)
                    if t:
                        t.additionalInfo.pydev_step_cmd = id
                        t.additionalInfo.pydev_state = PyDB.STATE_RUN
                        
                        
                elif id == CMD_GET_VARIABLE:
                    #we received some command to get a variable
                    #the text is: thread\tstackframe\tLOCAL|GLOBAL\tattributes*
                    
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
                        
                elif id == CMD_GET_FRAME:
                    thread_id, frame_id, scope = text.split('\t', 2)
                    thread_id = long(thread_id)
                    
                    int_cmd = InternalGetFrame(seq, thread_id, frame_id)
                    self.postInternalCommand(int_cmd, thread_id)
                        
                elif id == CMD_SET_BREAK:
                    #command to add some breakpoint.
                    # text is file\tline. Add to breakpoints dictionary
                    file, line, condition = text.split( '\t', 2 )
                    file = NormFile( file )
                    
                    line = int( line )
                    
                    if pydevd_trace_breakpoints > 0:
                        print 'Added breakpoint:%s - line:%s' % (file, line)
                        
                    if self.breakpoints.has_key( file ):
                        breakDict = self.breakpoints[file]
                    else:
                        breakDict = {}
    
                    if len(condition) <= 0 or condition == None or "None" == condition:
                        breakDict[line] = (True, None)
                    else:
                        breakDict[line] = (True, condition)
                    
                        
                    self.breakpoints[file] = breakDict
                    PyDBCtx_SetTraceForAllFileCtxs(file)
                    
                elif id == CMD_REMOVE_BREAK:
                    #command to remove some breakpoint
                    #text is file\tline. Remove from breakpoints dictionary
                    file, line = text.split('\t', 1)
                    file = NormFile(file)
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
                            
                elif id == CMD_EVALUATE_EXPRESSION or id == CMD_EXEC_EXPRESSION:
                    #command to evaluate the given expression
                    #text is: thread\tstackframe\tLOCAL\texpression
                    thread_id, frame_id, scope, expression = text.split('\t', 3)
                    thread_id = long(thread_id)
                    int_cmd = InternalEvaluateExpression(seq, thread_id, frame_id, expression, id == CMD_EXEC_EXPRESSION)
                    self.postInternalCommand(int_cmd, thread_id)
                        
                        
                else:
                    #I have no idea what this is all about
                    cmd = self.cmdFactory.makeErrorMessage(seq, "unexpected command " + str(id))
                    
                if cmd: 
                    self.writer.addCommand(cmd)
                    
            except Exception, e:
                traceback.print_exc(e)
                traceback.print_exc()
                cmd = self.cmdFactory.makeErrorMessage(seq, "Unexpected exception in processNetCommand: %s\nInitial params: %s" % (str(e), (id, seq, text)))
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
        thread.additionalInfo.pydev_state = PyDB.STATE_SUSPEND
        thread.stop_reason = stop_reason

    def doWaitSuspend(self, thread, frame, event, arg):
        """ busy waits until the thread state changes to RUN 
        it expects thread's state as attributes of the thread.
        Upon running, processes any outstanding Stepping commands.
        """
        self.processInternalCommands()
        cmd = self.cmdFactory.makeThreadSuspendMessage(id(thread), frame, thread.stop_reason)
        self.writer.addCommand(cmd)
        
        info = thread.additionalInfo
        while info.pydev_state == PyDB.STATE_SUSPEND and not self.finishDebuggingSession:            
            self.processInternalCommands()
            time.sleep(0.2)
            
        #process any stepping instructions 
        if info.pydev_step_cmd == CMD_STEP_INTO:
            info.pydev_step_stop = None
            
        elif info.pydev_step_cmd == CMD_STEP_OVER:
            if event is 'return': # if we are returning from the function, stop in parent
                info.pydev_step_stop = frame.f_back
            else:
                info.pydev_step_stop = frame
                
        elif info.pydev_step_cmd == CMD_STEP_RETURN:
            info.pydev_stop_on_return_count_1 = True
            info.pydev_return_call_count = 0
            info.pydev_step_stop = frame.f_back
 
        cmd = self.cmdFactory.makeThreadRunMessage(id(thread), info.pydev_step_cmd)
        self.writer.addCommand(cmd)
                

    def trace_dispatch(self, frame, event, arg):
        ''' This is the main callback for the debugger. 
        
        It is called for each new context we enter       
        After we hit some breakpoint, it is called for every line executed (until we exit debug state).
        
        We also decorate the thread we are in with info about the debugging.
        The attributes added are:
            pydev_state
            pydev_step_stop
            pydev_step_cmd
            pydev_notify_kill 
        '''
        if self.finishDebuggingSession:
            #that was not working very well because jython gave some socket errors
            threads = threading.enumerate()
            for t in threads: 
                if hasattr(t, 'doKill'):
                    t.doKill()
            time.sleep(0.2) #give them some time to release their locks...
            try:
                import java.lang.System
                java.lang.System.exit(0)
            except:
                sys.exit(0)

        if event not in ('call', 'line', 'return', 'exception'): #jython has an 'exception' event that must be treated too (surprise, surprise)
            return None
        
        
        t = threading.currentThread()
        ctx = PyDBCtx_GetCtx(frame, t)
        base = ctx.base

        if base in DONT_TRACE: #we don't want to debug threading or anything related to pydevd
            return None

        # if thread is not alive, cancel trace_dispatch processing
        if not t.isAlive():
            self.processThreadNotAlive(id(t))
            return None # suspend tracing
        
        try:
            additionalInfo = t.additionalInfo
        except AttributeError:
            additionalInfo = PyDBAdditionalThreadInfo()
            t.additionalInfo = additionalInfo
            
        filename = ctx.filename
        released = False
        ctx.acquire()
        try:
    
            # Let's check to see if we are in a line that has a breakpoint. If we don't have a breakpoint, 
            # we will return nothing for the next trace
            #also, after we hit a breakpoint and go to some other debugging state, we have to force the set trace anyway,
            #so, that's why the additional checks are there.
            if not self.breakpoints.has_key(filename) and additionalInfo.pydev_state == PyDB.STATE_RUN and \
               additionalInfo.pydev_step_stop is None and additionalInfo.pydev_step_cmd is None:
                #print 'skipping', base, frame.f_lineno, additionalInfo.pydev_state, additionalInfo.pydev_step_stop, additionalInfo.pydev_step_cmd
                return self.stopTracingFrame(frame)
    
            else:
                #print 'NOT skipped', base, frame.f_lineno, additionalInfo.pydev_state, additionalInfo.pydev_step_stop, additionalInfo.pydev_step_cmd
                #We just hit a breakpoint or we are already in step mode. Either way, let's trace this frame
                frame.f_trace = self.trace_dispatch
            
            
            
            try:
                line = int(frame.f_lineno)
                if additionalInfo.pydev_state != PyDB.STATE_SUSPEND and self.breakpoints.has_key(filename) and self.breakpoints[filename].has_key(line):
                    #ok, hit breakpoint, now, we have to discover if it is a conditional breakpoint
                    # lets do the conditional stuff here
                    condition = self.breakpoints[filename][line][1]
    
                    if condition is not None:
                        try:
                            val = eval(condition, frame.f_globals, frame.f_locals)
                            if not val:
                                return None
                                
                        except:
                            print >> sys.stderr, 'Error while evaluating expression'
                            traceback.print_exc()
                            return self.stopTracingFrame(frame)
                    
                    #when we hit a breakpoint, we set the tracing function for the callers of the current frame, because
                    #we may have to do some return
                    SetTraceForParents(frame, self.trace_dispatch)
                    self.setSuspend(t, CMD_SET_BREAK)
                    
                # if thread has a suspend flag, we suspend with a busy wait
                if additionalInfo.pydev_state == PyDB.STATE_SUSPEND:
                    released = True
                    ctx.release()
                    self.doWaitSuspend(t, frame, event, arg)
                    return self.trace_dispatch
                
            except:
                traceback.print_exc()
                raise
    
            #step handling. We stop when we hit the right frame
            try:
                if additionalInfo.pydev_step_cmd == CMD_STEP_INTO and event in ('line', 'return'):
                    self.setSuspend(t, CMD_STEP_INTO)
                    released = True
                    ctx.release()
                    self.doWaitSuspend(t, frame, event, arg)      
                    
                
                elif additionalInfo.pydev_step_cmd == CMD_STEP_OVER and event in ('line', 'return'): 
                    if additionalInfo.pydev_step_stop == frame:
                        self.setSuspend(t, CMD_STEP_OVER)
                        released = True
                        ctx.release()
                        self.doWaitSuspend(t, frame, event, arg)
                    
                    
                elif additionalInfo.pydev_step_cmd == CMD_STEP_RETURN:
                    if event == 'return':
                        additionalInfo.pydev_return_call_count += 1
                    elif event == 'call':
                        additionalInfo.pydev_return_call_count -= 1
                        
                    if additionalInfo.pydev_stop_on_return_count_1 and additionalInfo.pydev_return_call_count == 1 \
                        and additionalInfo.pydev_step_stop == frame.f_back  and event in ('line', 'return'):
                        
                        additionalInfo.pydev_return_call_count == 0
                        additionalInfo.pydev_stop_on_return_count_1 = False
                        self.setSuspend(t, CMD_STEP_RETURN)
                        released = True
                        ctx.release()
                        self.doWaitSuspend(t, frame, event, arg)
                        
                    if additionalInfo.pydev_step_stop == frame and event in ('line', 'return'):
                        self.setSuspend(t, CMD_STEP_RETURN)
                        released = True
                        ctx.release()
                        self.doWaitSuspend(t, frame, event, arg)
            except:
                traceback.print_exc()
                additionalInfo.pydev_step_cmd = None
        finally:
            if not released:
                ctx.release()
        
        
        #if we are quitting, let's stop the tracing
        retVal = None
        if not self.quitting:
            retVal = self.trace_dispatch

        return retVal

    def stopTracingFrame(self, frame):
        '''Removes the f_trace hook from the frame and returns None
        '''
        
        if hasattr(frame, 'f_trace'):
            del frame.f_trace
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

        sys.settrace(self.trace_dispatch) 
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
    retVal['type'] = ''
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
        elif (argv[i] == '--type'):
            del argv[i]
            retVal['type'] = argv[i]
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
        else:
            raise ValueError, "unexpected option " + argv[i]
    return retVal

def usage(doExit=0):
    print 'Usage:'
    print 'pydevd.py --port=N [(--client hostname) | --server] --file executable [file_options]'
    if doExit:
        sys.exit(0)


def setupType():
    global type
    try:
        import java.lang
        type = 'jython'
    except:
        type = 'python'


def SetTraceForAll(frame, dispatch_func):
    frame.f_trace = dispatch_func
    SetTraceForParents(frame, dispatch_func)
    
def SetTraceForParents(frame, dispatch_func):
    frame = frame.f_back
    while frame:
        frame.f_trace = dispatch_func
        frame = frame.f_back

def settrace(host='localhost', stdoutToServer = False, stderrToServer = False):
    '''
    @param host: the user may specify another host, if the debug server is not in the same machine
    @param stdoutToServer: when this is true, the stdout is passed to the debug server
    @param stderrToServer: when this is true, the stderr is passed to the debug server
    so that they are printed in its console and not in this process console.
    '''
    
    global connected
    global bufferStdOutToServer
    global bufferStdErrToServer
    
    if not connected :
        connected = True  
        bufferStdOutToServer = stdoutToServer
        bufferStdErrToServer = stderrToServer
        
        setupType()
        
        debugger = PyDB()
        debugger.connect(host, 5678)
        
        net = NetCommand(str(CMD_THREAD_CREATE), 0, '<xml><thread name="pydevd.reader" id="-1"/></xml>')
        debugger.writer.addCommand(net)
        net = NetCommand(str(CMD_THREAD_CREATE), 0, '<xml><thread name="pydevd.writer" id="-1"/></xml>')
        debugger.writer.addCommand(net)
        
        if bufferStdOutToServer:
            sys.stdoutBuf = pydevd_io.IOBuf()
            sys.stdout = pydevd_io.IORedirector(sys.stdout, sys.stdoutBuf)
            
        if bufferStdErrToServer:
            sys.stderrBuf = pydevd_io.IOBuf()
            sys.stderr = pydevd_io.IORedirector(sys.stderr, sys.stderrBuf)
            
        SetTraceForParents(sys._getframe(), debugger.trace_dispatch)
        
        t = threading.currentThread()      
        try:
            additionalInfo = t.additionalInfo
        except AttributeError:
            additionalInfo = PyDBAdditionalThreadInfo()
            t.additionalInfo = additionalInfo
  
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
            additionalInfo = PyDBAdditionalThreadInfo()
            t.additionalInfo = additionalInfo
            
        sys.settrace(debugger.trace_dispatch)
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
 
    setupType()

    global type
    type = setup['type']

    debugger = PyDB()
    debugger.connect(setup['client'], setup['port'])
    debugger.run(setup['file'], None, None)
    