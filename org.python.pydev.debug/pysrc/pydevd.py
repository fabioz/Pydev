import weakref
from pydevd_comm import *

DONT_TRACE = ('pydevd.py' , 'threading.py', 'pydevd_vars.py')

connected = False

    
class PyDBCtx:
    '''This class is used to keep track of the contexts we pass through (acting as a cache for them).
    '''

    threadToCtx = dict()
    
    @staticmethod
    def GetCtxs():
        ret = []
        for v in PyDBCtx.threadToCtx.values():
            ret += v.values()
        return ret
    
    @staticmethod
    def GetCtx(frame, currThread):
        #we create a context for each thread
        threadId = id(currThread)
        ctxs = PyDBCtx.threadToCtx.get(threadId)
        if ctxs is None:
            ctxs = dict()
            PyDBCtx.threadToCtx[threadId] = ctxs
            
        #and for each thread, the code for the frame
        key = frame.f_code
        ctx = ctxs.get(key)
        if ctx is None:
            ctx = PyDBCtx(frame, threadId)
            ctxs[key] = ctx

        return ctx
        
    @staticmethod
    def SetTraceForAllFileCtxs(f):
        g = GetGlobalDebugger()
        if g:
            for ctx in PyDBCtx.GetCtxs():
                if ctx.filename == f:
                    ctx.frame.f_trace = g.trace_dispatch
            
    def __init__(self, frame, threadId):
        self.filename = NormFile(frame.f_code.co_filename)
        self.base = os.path.basename( self.filename )
        self.frame = frame
        self.thread_id = threadId
        
    def __str__(self):
        return 'PyDBCtx [%s %s %s]' % (self.base, self.thread_id, self.frame)
        
class PyDBCommandThread(PyDBDaemonThread):
    
    def __init__(self, pyDb):
        PyDBDaemonThread.__init__(self)
        self.pyDb = pyDb

    def run(self):
        sys.settrace(None) # no debugging on this thread
        while True:
            self.pyDb.processInternalCommands()
            time.sleep(0.1)
            

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

    def __init__(self):
        SetGlobalDebugger(self)
        self.reader = None
        self.writer = None
        self.quitting = None
        self.cmdFactory = NetCommandFactory() 
        self.cmdQueue = {}     # the hash of Queues. Key is thread id, value is thread
        self.breakpoints = {}
        self.readyToRun = False

    def initializeNetwork(self, sock):
        try:
            sock.settimeout(None) # infinite, no timeouts from now on - jython does not have it
        except:
            pass
        self.writer = WriterThread(sock)
        self.writer.start()
        self.reader = ReaderThread(sock)
        self.reader.start()        
        time.sleep(0.1) # give threads time to start        
        
    def startServer(self, port):
        """ binds to a port, waits for the debugger to connect """
        # TODO untested
        s = socket(AF_INET, SOCK_STREAM)
        s.bind(('', port))
        s.listen(1)
        newSock, addr = s.accept()
        self.initializeNetwork(newSock)

    def startClient(self, host, port):
        """ connects to a host/port """
        pydevd_log(1, "Connecting to " + host + ":" + str(port))
        try:
            s = socket(AF_INET, SOCK_STREAM);
            try:
                s.settimeout(10) # seconds - jython does not have it
            except:
                pass

            s.connect((host, port))
            pydevd_log(1, "Connected.")
            self.initializeNetwork(s)
        except:
            print "server timed out after 10 seconds, could not connect to " + host + ":" + str(port)
            print "Exiting. Bye!"
            sys.exit(1)

    def connect(self, host, port):
        if host:
            self.startClient(host, port)
        else:
            self.startServer(port)
        
#        PyDBCommandThread(self).start()

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
                    cmd = self.cmdFactory.makeThreadCreatedMessage(t)
            if cmd:
                pydevd_log(2, "found a new thread " + str(thread_id))
                self.writer.addCommand(cmd)
            else:
                pydevd_log(0, "could not find thread by id to register")
                
        return self.cmdQueue[thread_id]
        
    def postInternalCommand(self, int_cmd, thread_id):
        """ if thread_id is *, post to all """
#        print "Posting internal command for ", str(thread_id)
        if thread_id == "*":
            for k in self.cmdQueue.keys(): 
                self.cmdQueue[k].put(int_cmd)
                
        else:
            queue = self.getInternalQueue(thread_id)
            queue.put(int_cmd)

    def processInternalCommands(self):
        threads = threading.enumerate()
        for t in threads:
            if not isinstance(t, PyDBDaemonThread):
                queue = self.getInternalQueue(id(t))
                try:
                    while True:
                        int_cmd = queue.get(False)
                        pydevd_log(2, "processign internal command " + str(int_cmd))
                        int_cmd.doIt(self)
                except PydevQueue.Empty:
                    pass # this is how we exit
      
    def processNetCommand(self, id, seq, text):
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
                    t.pydev_state = PyDB.STATE_RUN
                    t.pydev_step_cmd = None
                    
            elif id == CMD_STEP_INTO or id == CMD_STEP_OVER or id == CMD_STEP_RETURN:
                #we received some command to make a single step
                t = pydevd_findThreadById(text)
                if t:
                    t.pydev_state = PyDB.STATE_RUN
                    t.pydev_step_cmd = id
                    
                    
            elif id == CMD_GET_VARIABLE:
                #we received some command to get a variable
                #the text is: thread\tstackframe\tLOCAL|GLOBAL\tattributes*
                
                thread_id, frame_id, scopeattrs = text.split('\t', 2)
                if scopeattrs.find('\t') != -1: # there are attibutes beyond scope
                    (scope, attrs) = scopeattrs.split('\t', 1)
                else:
                    (scope, attrs) = (scopeattrs, None)
                t = pydevd_findThreadById(thread_id)
                if t:
                    int_cmd = InternalGetVariable(seq, t, frame_id, scope, attrs)
                    self.postInternalCommand(int_cmd, thread_id)
                else:
                    cmd = self.cmdFactory.makeErrorMessage(seq, "could not find thread for variable")
                    
                    
            elif id == CMD_SET_BREAK:
                #command to add some breakpoint.
                # text is file\tline. Add to breakpoints dictionary
                file, line, condition = text.split( '\t', 2 )
                file = NormFile( file )
                
                line = int( line )
                if self.breakpoints.has_key( file ):
                    breakDict = self.breakpoints[file]
                else:
                    breakDict = {}

                if len(condition) <= 0 or condition == None or "None" == condition:
                    breakDict[line] = (True, None)
                else:
                    breakDict[line] = (True, condition)
                    
                self.breakpoints[file] = breakDict
                PyDBCtx.SetTraceForAllFileCtxs(file)
                
            elif id == CMD_REMOVE_BREAK:
                #command to remove some breakpoint
                #text is file\tline. Remove from breakpoints dictionary
                file, line = text.split('\t', 1)
                file = NormFile(file)
                line = int(line)
                
                if self.breakpoints.has_key(file):
                    if self.breakpoints[file].has_key(line):
                        del self.breakpoints[file][line]
                        keys = self.breakpoints[file].keys()
                        if len(keys) is 0:
                            del self.breakpoints[file]
                    else:
                        print sys.stderr, "breakpoint not found", file, str(line)
                        
            elif id == CMD_EVALUATE_EXPRESSION:
                #command to evaluate the given expression
                #text is: thread\tstackframe\tLOCAL\texpression
                thread_id, frame_id, scope, expression = text.split('\t', 3)
                t = pydevd_findThreadById(thread_id)
                if t:
                    int_cmd = InternalEvaluateExpression(seq, t, frame_id, expression)
                    self.postInternalCommand(int_cmd, thread_id)
                else:
                    cmd = self.cmdFactory.makeErrorMessage(seq, "could not find thread for expression")
                    
                    
            else:
                #I have no idea what this is all about
                cmd = self.cmdFactory.makeErrorMessage(seq, "unexpected command " + str(id))
                
            if cmd: 
                self.writer.addCommand(cmd)
                
        except Exception, e:
            import traceback
            traceback.print_exc(e)
            cmd = self.cmdFactory.makeErrorMessage(seq, "Unexpected exception in processNetCommand:" + str(e))
            self.writer.addCommand(cmd)

    def processThreadNotAlive(self, thread):
        """ if thread is not alive, cancel trace_dispatch processing """
        wasNotified = False
        
        try:
            wasNotified = thread.pydev_notify_kill
        except AttributeError:
            thread.pydev_notify_kill = False
            
        if not wasNotified:
            cmd = self.cmdFactory.makeThreadKilledMessage(id(thread))
            self.writer.addCommand(cmd)
            thread.pydev_notify_kill = True

    def setSuspend(self, thread, stop_reason):
        thread.pydev_state = PyDB.STATE_SUSPEND
        thread.stop_reason = stop_reason

    def doWaitSuspend(self, thread, frame, event, arg):
        """ busy waits until the thread state changes to RUN 
        it expects thread's state as attributes of the thread.
        Upon running, processes any outstanding Stepping commands.
        """
        #print >>sys.stderr, "thread suspended", thread.getName()
        self.processInternalCommands()

        cmd = self.cmdFactory.makeThreadSuspendMessage(id(thread), frame, thread.stop_reason)
        self.writer.addCommand(cmd)

        while thread.pydev_state == PyDB.STATE_SUSPEND:            
            time.sleep(0.1)
            
        #process any stepping instructions 
        if thread.pydev_step_cmd == CMD_STEP_INTO:
            thread.pydev_step_stop = None
            
        elif thread.pydev_step_cmd == CMD_STEP_OVER:
            if event is 'return': # if we are returning from the function, stop in parent
                #print "Stepping back one"
                thread.pydev_step_stop = frame.f_back
            else:
                thread.pydev_step_stop = frame
                
        elif thread.pydev_step_cmd == CMD_STEP_RETURN:
            thread.pydev_step_stop = frame.f_back
 
        cmd = self.cmdFactory.makeThreadRunMessage(id(thread), thread.pydev_step_cmd)
        self.writer.addCommand(cmd)
                

    def trace_dispatch(self, frame, event, arg):
        ''' This is the main callback for the debugger. 
        
        It is called for each new context we enter       
        After we hit some breakpoint, it is called for every line executed (until we exit debug state).
        
        We also decorate the thread we are in with info about the debugging.
        The attributes added are:
            pydev_state
            pydev_step_stop
            pydev_step_cmo
        '''
        if event not in ('call', 'line', 'return'):
            return None
        
        t = threading.currentThread()
        ctx = PyDBCtx.GetCtx(frame, t)
        filename = ctx.filename
        base = ctx.base

        if base in DONT_TRACE: #we don't want to debug pydevd or threading
            return None


        # if thread is not alive, cancel trace_dispatch processing
        if not t.isAlive():
            self.processThreadNotAlive(t)
            return None # suspend tracing
        wasSuspended = False        

        #let's decorate the thread we are in with debugging info
        if not hasattr(t, 'pydev_state'):
            t.pydev_state = PyDB.STATE_RUN 
            
        if not hasattr(t, 'pydev_step_stop'):
            t.pydev_step_stop = None
            
        if not hasattr(t, 'pydev_step_cmd'):
            t.pydev_step_cmd = None
        
        # Let's check to see if we are in a line that has a breakpoint. If we don't have a breakpoint, 
        # we will return nothing for the next trace
        #also, after we hit a breakpoint and go to some other debugging state, we have to force the set trace anyway,
        #so, that's why the additional checks are there.
        if not self.breakpoints.has_key(filename) and t.pydev_state == PyDB.STATE_RUN and t.pydev_step_stop is None and t.pydev_step_cmd is None:
            #print 'skipping', base, frame.f_lineno, t.pydev_state, t.pydev_step_stop, t.pydev_step_cmd
            return self.stopTracingFrame(frame)

        else:
            #print 'NOT skipped', base, frame.f_lineno, t.pydev_state, t.pydev_step_stop, t.pydev_step_cmd
            #We just hit a breakpoint or we are already in step mode. Either way, let's trace this frame
            frame.f_trace = self.trace_dispatch
        
        
        
        try:
            line = int(frame.f_lineno)
            if t.pydev_state != PyDB.STATE_SUSPEND and self.breakpoints.has_key(filename) and self.breakpoints[filename].has_key(line):
                #ok, hit breakpoint, now, we have to discover if it is a conditional breakpoint
                # lets do the conditional stuff here
                condition = self.breakpoints[filename][line][1]

                if condition is not None:
                    try:
                        val = eval(condition, frame.f_globals, frame.f_locals)
                        if not val:# and not (t.pydev_step_cmd == CMD_STEP_INTO or t.pydev_step_cmd == CMD_STEP_OVER or t.pydev_step_cmd == CMD_STEP_RETURN ) :
                            return self.stopTracingFrame(frame)
                            
                    except:
                        print >> sys.stderr, 'Error while evaluating expression'
                        import traceback;traceback.print_exc()
                        return self.stopTracingFrame(frame)
                
                #when we hit a breakpoint, we set the tracing function for the callers of the current frame, because
                #we may have to do some return
                SetTraceForParents(frame, self.trace_dispatch)
                self.setSuspend(t, CMD_SET_BREAK)
                
            # if thread has a suspend flag, we suspend with a busy wait
            if t.pydev_state == PyDB.STATE_SUSPEND:
                wasSuspended = True
                self.doWaitSuspend(t, frame, event, arg)
                return self.trace_dispatch
            
        except:
            import traceback;traceback.print_exc()
            raise

        if not wasSuspended and (event == 'line' or event== 'return'):
            #step handling. We stop when we hit the right frame
            try:
                if t.pydev_step_cmd == CMD_STEP_INTO:
                    self.setSuspend(t, CMD_STEP_INTO)
                    self.doWaitSuspend(t, frame, event, arg)      
                    
                
                elif t.pydev_step_cmd == CMD_STEP_OVER or t.pydev_step_cmd == CMD_STEP_RETURN:
                    if t.pydev_step_stop == frame:
                        self.setSuspend(t, t.pydev_step_cmd)
                        self.doWaitSuspend(t, frame, event, arg)
            except:
                t.pydev_step_cmd = None
        
        
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
        import os
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
      
def quittingNow():
    pydevd_log(1, "Debugger exiting. Over & out....\n")

def quittingNowJython():
    try:
        pydevd_log(1, "Debugger exiting. Over & out ( Jython )....\n")        
        import sys
        sys.exit(0)        
    except:
        print "Exception in quitting now jython"
        raise

def setupQuiting():
    global type
    type = 'jython'
    
    import atexit
    try:
        import java.lang
        atexit.register(quittingNowJython)
    except:
        atexit.register(quittingNow)
        type = 'python'


def SetTraceForAll(frame, dispatch_func):
    frame.f_trace = dispatch_func
    SetTraceForParents(frame, dispatch_func)
    
def SetTraceForParents(frame, dispatch_func):
    frame = frame.f_back
    while frame:
        frame.f_trace = dispatch_func
        frame = frame.f_back

def settrace():
    global connected
    if not connected :
        connected = True  
        
        setupQuiting()
        
        debugger = PyDB()
        debugger.connect('localhost', 5678)
        
        net = NetCommand(str(CMD_THREAD_CREATE), 0, '<xml><thread name="pydevd.reader" id="-1"/></xml>')
        debugger.writer.addCommand(net)
        net = NetCommand(str(CMD_THREAD_CREATE), 0, '<xml><thread name="pydevd.writer" id="-1"/></xml>')
        debugger.writer.addCommand(net)
        
        SetTraceForParents(sys._getframe(), debugger.trace_dispatch)
        
        t = threading.currentThread()        
        debugger.setSuspend(t, CMD_SET_BREAK)
        
        sys.settrace(debugger.trace_dispatch)
    
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
 
    setupQuiting()

    global type
    type = setup['type']

    debugger = PyDB()
    debugger.connect(setup['client'], setup['port'])
    debugger.run(setup['file'], None, None)
    