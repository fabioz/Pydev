""" pydevd - a debugging daemon
This is the daemon you launch for python remote debugging.

Protocol:
each command has a format:
    id\tsequence-num\ttext
    id: protocol command number
    sequence-num: each request has a sequence number. Sequence numbers
    originating at the debugger are odd, sequence numbers originating
    at the daemon are even. Every response uses the same sequence number
    as the request.
    payload: it is protocol dependent. When response is a complex structure, it
    is returned as XML. Each attribute value is urlencoded, and then the whole
    payload is urlencoded again to prevent stray characters corrupting protocol/xml encodings

    Commands:
 
    NUMBER   NAME            FROM*     ARGUMENTS           RESPONSE      NOTE
100 series: program execution
    101      RUN             RDB       -                   -
    102      LIST_THREADS    RDB                           RETURN with XML listing of all threads
    103      THREAD_CREATE   PYDB      -                   XML with thread information
    104		 THREAD_KILL     RDB	   id			       kills the thread
                             PYDB      id                  nofies RDB that thread was killed
	105		 THREAD_SUSPEND  RDB       XML of the stack,   suspends the thread
									   reason for suspension
							 PYDB      id                  notifies RDB that thread was suspended
	106      THREAD_RUN      RDB       id                  resume the thread
	                         PYDB      id \t reason        notifies RDB that thread was resumed
	107      STEP_INTO       RDB       thread_id
	108      STEP_OVER       RDB       thread_id
	109      STEP_RETURN     RDB       thread_id
	110		 GET_VARIABLE	 RDB       var_locator		   GET_VARIABLE with XML of var content
									   see code for definition
	111      SET_BREAK       RDB       file/line of the breakpoint
	112      REMOVE_BREAK    RDB       file/line of the return
    113      EVALUATE_EXPRESSION RDB   expression          result of evaluating the expression

500 series diagnostics/ok
    901      VERSION       either      Version string (1.0)               Currently just used at startup
    902      RETURN		   either      Depends on caller    -
900 series: errors
    501      ERROR         either      -                   This is reserved for unexpected errors.
                                  
    * RDB - remote debugger, the java end
    * PYDB - pydevd, the python end
"""


try:
    __setFalse = False
except:
    False = 0
    True = 1


import time
import sys
import threading
import Queue as PydevQueue
from socket import socket
from socket import AF_INET
from socket import SOCK_STREAM
from socket import error
import urllib
import pydevd_vars

VERSION_STRING = "1.0"

CMD_RUN = 101
CMD_LIST_THREADS = 102
CMD_THREAD_CREATE = 103
CMD_THREAD_KILL = 104
CMD_THREAD_SUSPEND = 105
CMD_THREAD_RUN = 106
CMD_STEP_INTO = 107
CMD_STEP_OVER = 108
CMD_STEP_RETURN = 109
CMD_GET_VARIABLE = 110
CMD_SET_BREAK = 111
CMD_REMOVE_BREAK = 112
CMD_EVALUATE_EXPRESSION = 113
CMD_VERSION = 501
CMD_RETURN = 502
CMD_ERROR = 901 

Debugger = None

__all__ = ();
    
pydevd_trace = 0

def pydevd_log(level, s):
    """ levels are: 
    	0 most serious warnings/errors
        1 warnings/significant events
        2 informational trace
    """
    if (level <= pydevd_trace):
        print >>sys.stderr, s 


class ReaderThread(threading.Thread):
    """ reader thread reads and dispatches commands in an infinite loop """
    
    def __init__(self, sock):
        threading.Thread.__init__(self)
        self.sock = sock
        self.setName("pydevd.Reader")
        self.setDaemon(True)
     
    def run(self):
        sys.settrace(None) # no debugging on this thread
        buffer = ""
        try:
            while(True):
#                print >>sys.stderr, "waiting for input"
                buffer += self.sock.recv(1024)
#                print >>sys.stderr, "received input"
                while (buffer.find('\n') != -1):
                    [command, buffer] = buffer.split('\n', 1)
                    pydevd_log(1, "received command " + command)
                    args = command.split('\t', 2)
#                    print "the args are", args[0], " 2 ", args[1], " 3 ", args[2]
                    PyDB.instance.processNetCommand(int(args[0]), int(args[1]), args[2])
#                print >>sys.stderr, "processed input"
        except:
            print >>sys.stderr, "Exception in reader thread"
            raise

class WriterThread(threading.Thread):
    """ writer thread writes out the commands in an infinite loop """
    def __init__(self, sock):
        threading.Thread.__init__(self)
        self.sock = sock
        self.setName("pydevd.Writer")
        self.setDaemon(True)
        self.cmdQueue = PydevQueue.Queue()
        if type=='python':
            self.timeout = 0
        else:
            self.timeout = 0.1
       
    def addCommand(self, cmd):
        """ cmd is NetCommand """
        self.cmdQueue.put(cmd)
        
    def run(self):
        """ just loop and write responses """
        sys.settrace(None) # no debugging on this thread
        try:
            while(True):
                cmd = self.cmdQueue.get(1)
                out = cmd.getOutgoing()
                pydevd_log(1, "sending cmd " + out)
                bytesSent = self.sock.send(out) #TODO: this does not guarantee that all message is sent (and jython does not have a send all)
                time.sleep(self.timeout)                
        except Exception, e:
            print >>sys.stderr, "Exception in writer thread", str(e)
        except:
            print >>sys.stderr, "Exception in writer thread"
            raise
    
    
class NetCommand:
    """ Commands received/sent over the network.
    
    Command can represent command received from the debugger,
    or one to be sent by daemon.
    """
    next_seq = 0 # sequence numbers
 
    def __init__(self, id, seq, text):
        """ smart handling of paramaters
        if sequence is 0, new sequence will be generated
        if text has carriage returns they'll be replaced"""
        self.id = id
        if (seq == 0): seq = self.getNextSeq()
        self.seq = seq
        self.text = text
        self.outgoing = self.makeMessage(id, seq, text)
  
    def getNextSeq(self):
        """ returns next sequence number """
        NetCommand.next_seq += 2
        return NetCommand.next_seq

    def getOutgoing(self):
        """ returns the outgoing message"""
        return self.outgoing
    
    def makeMessage(self, cmd, seq, payload):
        encoded = urllib.quote(str(payload), '/<>_=" \t')
        return str(cmd) + '\t' + str(seq) + '\t' + encoded + "\n"

class NetCommandFactory:
    
    def __init_(self):
        self.next_seq = 0

    def threadToXML(self, thread):
        """ thread information as XML """
        cmdText = '<thread name="' + urllib.quote(thread.getName()) + '"'
        cmdText += ' id="' + str(id(thread)) + '" />'
        return cmdText

    def makeErrorMessage(self, seq, text):
        cmd = NetCommand(CMD_ERROR, seq, text)
        print >>sys.stderr, "Error: ", text
        return cmd;

    def makeThreadCreatedMessage(self,thread):
        cmdText = "<xml>" + self.threadToXML(thread) + "</xml>"
        return NetCommand(CMD_THREAD_CREATE, 0, cmdText)
 
    def makeListThreadsMessage(self, seq):
        """ returns thread listing as XML """
        try:
            t = threading.enumerate()
            cmdText = "<xml>"
            for i in t:
                cmdText += self.threadToXML(i)
            cmdText += "</xml>"
            return NetCommand(CMD_RETURN, seq, cmdText)
        except:
            return self.makeErrorMessage(seq, sys.exc_info()[0])

    def makeVersionMessage(self, seq):
        try:
            return NetCommand(CMD_VERSION, seq, VERSION_STRING)
        except:
            return self.makeErrorMessage(seq, sys.exc_info()[0])
    
    def makeThreadKilledMessage(self, id):
        try:
            return NetCommand(CMD_THREAD_KILL, 0, str(id))
        except:
            return self.makeErrorMessage(0, sys.exc_info()[0])
    
    def makeThreadSuspendMessage(self, thread_id, frame, stop_reason):
        
        """ <xml>
        	<thread id="id">
 		       	<frame id="id" name="functionName " file="file" line="line">
        			<var variable stuffff....
        		</frame>
        	</thread>
       	"""
        try:
            cmdText = "<xml>"
            cmdText += '<thread id="' + str(thread_id) + '" ' + 'stop_reason="' + str(stop_reason) + '">'
            curFrame = frame
            while (curFrame):
#                print cmdText
                myId = str(id(curFrame))
#                print "id is ", myId
                myName = curFrame.f_code.co_name
#                print "name is ", myName
                myFile = curFrame.f_code.co_filename
#                myFile = inspect.getsourcefile(curFrame) or inspect.getfile(frame)
#                print "file is ", myFile
                myLine = str(curFrame.f_lineno)
#                print "line is ", myLine
                cmdText += '<frame id="' + myId +'" name="' + myName + '" '
                cmdText += 'file="' + urllib.quote(myFile, '/>_= \t') + '" line="' + myLine + '">"'
                variables = pydevd_vars.frameVarsToXML(curFrame)
                cmdText += variables
                cmdText += "</frame>"
                curFrame = curFrame.f_back
            cmdText += "</thread></xml>"
            return NetCommand(CMD_THREAD_SUSPEND, 0, cmdText)
        except:
            return self.makeErrorMessage(0, str(sys.exc_info()[0]))

    def makeThreadRunMessage(self, id, reason):
        try:
            return NetCommand(CMD_THREAD_RUN, 0, str(id) + "\t" + str(reason))
        except:
            return self.makeErrorMessage(0, sys.exc_info()[0])

    def makeGetVariableMessage(self, seq, payload):
        try:
            return NetCommand(CMD_GET_VARIABLE, seq, payload)
        except Exception, e:
            return self.makeErrorMessage(seq, str(e))

            
    def makeEvaluateExpressionMessage(self, seq, payload):
        try:
            return NetCommand(CMD_EVALUATE_EXPRESSION, seq, payload)
        except Exception, e:
            return self.makeErrorMessage(seq, str(e))

INTERNAL_TERMINATE_THREAD = 1
INTERNAL_SUSPEND_THREAD = 2

class InternalThreadCommand:
    """ internal commands are generated/executed by the debugger.
    
    The reason for their existence is that some commands have to be executed
    on specific threads. These are the InternalThreadCommands that get
    get posted to PyDB.cmdQueue.
    """
    def __init__(self, id, payload):
        self.id = id

    def doIt(self, dbg):
        print "you have to override doIt"

class InternalTerminateThread:
    def __init__(self, thread_id):
        self.thread_id = thread_id
    
    def doIt(self, dbg):
        pydevd_log(1,  "killing " + str(self.thread_id))
        cmd = dbg.cmdFactory.makeThreadKilledMessage(self.thread_id)
        dbg.writer.addCommand(cmd)
        time.sleep(0.1)
        sys.exit()

class InternalGetVariable:
    """ gets the value of a variable """
    def __init__(self, seq, thread, frame_id, scope, attrs):
        self.sequence = seq
        self.thread = thread
        self.frame_id = frame_id
        self.scope = scope
        self.attributes = attrs
     
    def doIt(self, dbg):
        """ Converts request into python variable """
        try:
            xml = "<xml>"
            valDict = pydevd_vars.resolveCompoundVariable(self.thread, self.frame_id, self.scope, self.attributes)
            keys = valDict.keys()
            keys.sort()
            for k in keys:
                xml += pydevd_vars.varToXML(valDict[k], str(k))
            xml += "</xml>"
#            print >>sys.stderr, "done to xml"
            cmd = dbg.cmdFactory.makeGetVariableMessage(self.sequence, xml)
#            print >>sys.stderr, "sending command"
            dbg.writer.addCommand(cmd)
        except Exception:
            exc_info = sys.exc_info()
            import StringIO
            s = StringIO.StringIO()
            import traceback;traceback.print_exception(exc_info[0], exc_info[1], exc_info[2], file = s)
            cmd = dbg.cmdFactory.makeErrorMessage(self.sequence, "Error resolving variables " + s.getvalue())

#            cmd = dbg.cmdFactory.makeErrorMessage(self.sequence, "Error resolving variables " + str(e))
            dbg.writer.addCommand(cmd)

           
class InternalEvaluateExpression:
    """ gets the value of a variable """
    def __init__(self, seq, thread, frame_id, expression):
        self.sequence = seq
        self.thread = thread
        self.frame_id = frame_id
        self.expression = expression
     
    def doIt(self, dbg):
        """ Converts request into python variable """
        try:
            result = pydevd_vars.evaluateExpression( self.thread, self.frame_id, self.expression )
            xml = "<xml>"
            xml += pydevd_vars.varToXML(result, "")
            xml += "</xml>"
#            print >>sys.stderr, "done to xml"
            cmd = dbg.cmdFactory.makeEvaluateExpressionMessage(self.sequence, xml)
#            print >>sys.stderr, "sending command"
            dbg.writer.addCommand(cmd)
        except Exception, e:
            cmd = dbg.cmdFactory.makeErrorMessage(self.sequence, "Error evaluating expression " + str(e))
            dbg.writer.addCommand(cmd)
            import traceback
            traceback.print_exc(file=sys.stderr)


def pydevd_findThreadById(thread_id):
    try:
        int_id = int(thread_id)
#        print >>sys.stderr, "enumerating"
# there was a deadlock here when I did not remove the tracing function when thread was dead
        threads = threading.enumerate()
#        print >>sys.stderr, "done enumerating"
        for i in threads:
            if int_id == id(i): return i
        print >>sys.stderr, "could not find thread"
    except:
        print >>sys.stderr, "unexpected exceiton if findThreadById"
    return None


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
    
    instance = None
    STATE_RUN = 1
    STATE_SUSPEND = 2 # thread states

    def __init__(self):
        PyDB.instance = self
        self.reader = None
        self.writer = None
        self.quitting = None
        self.cmdFactory = NetCommandFactory() 
        self.cmdQueue = {}     # the hash of Queues. Key is thread id, value is thread
        self.breakpoints = {}
        self.readyToRun = False

    def initializeNetwork(self, sock):        
        #sock.settimeout(None) # infinite, no timeouts from now on - jython does not have it
        self.reader = ReaderThread(sock)
        self.reader.start()
        self.writer = WriterThread(sock)
        self.writer.start()
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
#            s.settimeout(10) # seconds - jython does not have it
            s.connect((host, port))
            pydevd_log(1, "Connected.")
            self.initializeNetwork(s)
        except:
            print "server timed out after 10 seconds, could not connect to " + host + ":" + str(port)
            print "Exiting. Bye!"
            sys.exit(1)

    def connect(self, host, port):
        if (host):
            self.startClient(host, port)
        else:
            self.startServer(port)

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
            if (cmd):
                pydevd_log(2, "found a new thread " + str(thread_id))
                self.writer.addCommand(cmd)
            else:
                pydevd_log(0, "could not find thread by id to register")
        return self.cmdQueue[thread_id]
        
    def postInternalCommand(self, int_cmd, thread_id):
        """ if thread_id is *, post to all """
#        print "Posting internal command for ", str(thread_id)
        if (thread_id == "*"):
            for k in self.cmdQueue.keys(): self.cmdQueue[k].put(int_cmd)
        else:
            queue = self.getInternalQueue(thread_id)
            queue.put(int_cmd)

    def processInternalCommands(self):
        queue = self.getInternalQueue(id(threading.currentThread()))
        try:
            while (True):
                int_cmd = queue.get(False)
                pydevd_log(2, "processign internal command " + str(int_cmd))
                int_cmd.doIt(self)
        except PydevQueue.Empty:
            pass # this is how we exit
      
    def processNetCommand(self, id, seq, text):
        try:
            cmd = None
            if (id == CMD_RUN):
                self.readyToRun = True
            elif (id == CMD_VERSION): # response is version number
                cmd = self.cmdFactory.makeVersionMessage(seq)
            elif (id == CMD_LIST_THREADS): # response is a list of threads
                cmd = self.cmdFactory.makeListThreadsMessage(seq)
            elif (id == CMD_THREAD_KILL):
                int_cmd = InternalTerminateThread(text)
                self.postInternalCommand(int_cmd, text)
            elif (id == CMD_THREAD_SUSPEND):
#                print >>sys.stderr, "About to suspend ", text
                t = pydevd_findThreadById(text)
                if t: self.setSuspend(t, CMD_THREAD_SUSPEND)
#               else: print >>sys.stderr, "Could not find thread ", t
            elif (id  == CMD_THREAD_RUN):
                t = pydevd_findThreadById(text)
                if t: 
                    t.pydev_state = PyDB.STATE_RUN
                    t.pydev_step_cmd = None
            elif (id == CMD_STEP_INTO or id == CMD_STEP_OVER or id == CMD_STEP_RETURN):
                t = pydevd_findThreadById(text)
                if t:
                    t.pydev_state = PyDB.STATE_RUN
                    t.pydev_step_cmd = id
            elif (id == CMD_GET_VARIABLE):
                 # text is: thread\tstackframe\tLOCAL|GLOBAL\tattributes*
                (thread_id, frame_id, scopeattrs) = text.split('\t', 2)
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
            elif (id == CMD_SET_BREAK):
                # text is file\tline. Add to breakpoints dictionary
                (file, line) = text.split('\t', 1)
                if self.breakpoints.has_key(file):
                    breakDict = self.breakpoints[file]
                else:
                    breakDict = {}
                breakDict[int(line)] = True
                self.breakpoints[file] = breakDict
                pydevd_log(1, "Set breakpoint at " + file + " " + line)
            elif (id == CMD_REMOVE_BREAK):
                # text is file\tline. Remove from breakpoints dictionary
                (file, line) = text.split('\t', 1)
                line = int(line)
                if self.breakpoints.has_key(file):
                    if self.breakpoints[file].has_key(line):
                        del self.breakpoints[file][line]
                        keys = self.breakpoints[file].keys()
                        if len(keys) is 0:
                            del self.breakpoints[file]
                    else:
                        print sys.stderr, "breakpoint not found", file, str(line)
            elif (id == CMD_EVALUATE_EXPRESSION):
                # text is: thread\tstackframe\tLOCAL\texpression
                (thread_id, frame_id, scope, expression) = text.split('\t', 3)
                t = pydevd_findThreadById(thread_id)
                if t:
                    int_cmd = InternalEvaluateExpression(seq, t, frame_id, expression)
                    self.postInternalCommand(int_cmd, thread_id)
                else:
                    cmd = self.cmdFactory.makeErrorMessage(seq, "could not find thread for expression")
            else:
                cmd = self.cmdFactory.makeErrorMessage(seq, "unexpected command " + str(id))
            pydevd_log(1, "processed command " + str (id))
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
            pydevd_log(1, "leaving stopped thread " + str(id(thread)))
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
#        print >>sys.stderr, "thread suspended", thread.getName()
     
        cmd = self.cmdFactory.makeThreadSuspendMessage(id(thread), frame, thread.stop_reason)
        self.writer.addCommand(cmd)
        while (thread.pydev_state == PyDB.STATE_SUSPEND):
 #           print "waiting on thread"
            self.processInternalCommands()
            time.sleep(0.1)
        try:
            """ process any stepping instructions """
            if (thread.pydev_step_cmd == CMD_STEP_INTO):
                thread.pydev_step_stop = None
            elif (thread.pydev_step_cmd == CMD_STEP_OVER):
                if (event is 'return'): # if we are returning from the function, stop in parent
#                    print "Stepping back one"
                    thread.pydev_step_stop = frame.f_back
                else:
                    thread.pydev_step_stop = frame
            elif (thread.pydev_step_cmd == CMD_STEP_RETURN):
                thread.pydev_step_stop = frame.f_back
        except AttributeError:
            thread.pydev_step_cmd = None # so we do ont thro
            pass
 
        pydevd_log(1, "thread resumed " + thread.getName())  
        cmd = self.cmdFactory.makeThreadRunMessage(id(thread), thread.pydev_step_cmd)
        self.writer.addCommand(cmd)
                
    def trace_dispatch(self, frame, event, arg):
        """ the main callback from the debugger """
        self.processInternalCommands()
        
        t = threading.currentThread()
        """ if thread is not alive, cancel trace_dispatch processing """
        if not t.isAlive():
            self.processThreadNotAlive(t)
            return None # suspend tracing
        
        wasSuspended = False
        

        try:
            """ breakpoints """
            file = frame.f_code.co_filename
            line = int(frame.f_lineno)
            if t.pydev_state != PyDB.STATE_SUSPEND and self.breakpoints.has_key(file) and self.breakpoints[file].has_key(line):
                self.setSuspend(t, CMD_SET_BREAK)
            """ if thread has a suspend flag, we suspend with a busy wait """
            if (t.pydev_state == PyDB.STATE_SUSPEND):
                wasSuspended = True
                self.doWaitSuspend(t, frame, event, arg)
                return self.trace_dispatch
        except AttributeError:
            t.pydev_state = PyDB.STATE_RUN # assign it to avoid future exceptions
        except:
            print >> sys.stderr, "Exception in trace_dispatch"
            print sys.exc_info()[0]
            raise

        if ( not wasSuspended and (event == 'line' or event== 'return')):
            """ step handling. We stop when we hit the right frame"""
            try:
                if (t.pydev_step_cmd == CMD_STEP_INTO):
                    self.setSuspend(t, CMD_STEP_INTO)
                    self.doWaitSuspend(t, frame, event, arg)                    
                elif (t.pydev_step_cmd == CMD_STEP_OVER or t.pydev_step_cmd == CMD_STEP_RETURN):
                    if (t.pydev_step_stop == frame):
                        self.setSuspend(t, t.pydev_step_cmd)
                        self.doWaitSuspend(t, frame, event, arg)
            except:
                t.pydev_step_cmd = None
        
        retVal = None
#        print "t3 ", str(id(t))
        if self.quitting:
            pass
#        elif event == 'line':
#            retVal =  self.dispatch_line(frame)
#        elif event == 'call':
#            retVal =  self.dispatch_call(frame, arg)
#        elif event == 'return':
#            retVal =  self.dispatch_return(frame, arg)
#        elif event == 'exception':
#            retVal =  self.dispatch_exception(frame, arg)
        else:
            retVal = self.trace_dispatch
#            print 'bdb.Bdb.dispatch: unknown debugging event:', `event`
#        print "b", str(id(t))
        return retVal

    def dispatch_line(self, frame):
#        myFile = inspect.getsourcefile(frame) or inspect.getfile(frame)
#        myLine = str(frame.f_lineno)  
#        print ' File "'+ myFile + '", line '+ myLine
        return self.trace_dispatch

    def dispatch_call(self, frame, arg):
#        print "C " + str(frame) + " " + str(arg)
        return self.trace_dispatch

    def dispatch_return(self, frame, arg):
#        print "R " + str(frame) + " " + str(arg)
        return self.trace_dispatch

    def dispatch_exception(self, frame, arg):
#        print "E " + str(frame) + " " + str(arg)
        return self.trace_dispatch

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
        
        
        
#        if not isinstance(cmd, types.CodeType):
#            cmd = cmd+'\n'

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
    if (doExit):
        sys.exit(0)

      
def quittingNow():
    pydevd_log(1, "Debugger exiting. Over & out....\n")

def quittingNowJython():
    pydevd_log(1, "Debugger exiting. Over & out ( Jython )....\n")
    import sys
    sys.exit(0)

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
 
    import atexit
    if setup['type']=='python':
        atexit.register(quittingNow)
    else:
        atexit.register(quittingNowJython)

    global type
    type = setup['type']

    debugger = PyDB()
    debugger.connect(setup['client'], setup['port'])
    debugger.run(setup['file'], None, None)
    