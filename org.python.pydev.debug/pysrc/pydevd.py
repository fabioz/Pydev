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
    
    Commands:
 
    NUMBER   NAME            FROM*     ARGUMENTS           RESPONSE      NOTE
100 series: program execution
    101      RUN             RDB       -                   -
    102      LIST_THREADS    RDB                           RETURN with XML listing of all threads
    103      THREAD_CREATED  PYDB      -                   XML with thread information
500 series: errors
    501      ERROR         either      -                   This is reserved for unexpected errors.
900 series diagnostics/ok
    901      VERSION       either      Version string (1.0)
    902      RETURN		   either      Depends on caller    -
                                  
    * RDB - remote debugger, the java end
    * PYDB - pydevd, the python end
"""
import sys
import threading
import types
from Queue import *
from socket import *

VERSION_STRING = "1.0"

CMD_RUN = 101
CMD_LIST_THREADS = 102
CMD_THREAD_CREATED = 103
CMD_ERROR = 501 
CMD_VERSION = 901
CMD_RETURN = 902

Debugger = None

__all__ = ();

_trace = 2

def log(level, s):
    """ levels are: 
        0 most serious warnings/errors
        1 warnings/significant events
        2 informational trace
    """
    if (level <= _trace):
        print s 


class ReaderThread(threading.Thread):
    """ reader thread reads and dispatches commands in an infinite loop """
    
    def __init__(self, sock):
        threading.Thread.__init__(self)
        self.sock = sock
        self.setName("pydevd.Reader")
        self.setDaemon(True)
     
    def run(self):
        buffer = ""
        while(True):
            buffer += self.sock.recv(1024)
            while (buffer.find('\n') != -1):
                [command, buffer] = buffer.split('\n', 1)
                log(2, "received command " + command)
                args = command.split('\t', 2)
                PyDB.instance.processNetCommand(int(args[0]), int(args[1]), args[2])

class WriterThread(threading.Thread):
    """ writer thread writes out the commands in an infinite loop """
    def __init__(self, sock):
        threading.Thread.__init__(self)
        self.sock = sock
        self.setName("pydevd.Writer")
        self.setDaemon(True)
        self.cmdQueue = Queue()
       
    def addCommand(self, cmd):
        """ cmd is NetCommand """
        self.cmdQueue.put(cmd)
        
    def run(self):
        """ just loop and write responses """
        while(True):
            cmd = self.cmdQueue.get(1)
            log(2, "sending cmd " + cmd.getOutgoing())
            self.sock.sendall(cmd.getOutgoing())

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
        if text: text.replace("\n","\\n") # make sure we have no carriage returns
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
        return str(cmd) + '\t' + str(seq) + '\t' + str(payload)+ "\n"

class NetCommandFactory:
    
    def __init_(self):
       self.next_seq = 0

    def threadToXML(self, thread):
        """ thread information as XML """
        cmdText = '<thread name="' + thread.getName() + '"'
        cmdText += ' id="' + str(id(thread)) + '" />'
        return cmdText

    def makeErrorMessage(self, seq, text):
        if (seq == 0):
            seq = getNextSeq()
        cmd = NetCommand(CMD_ERROR, seq, text)
        return cmd;

    def makeThreadCreatedMessage(self,thread):
        cmdText = "<xml>" + self.threadToXML(thread) + "</xml>"
        return NetCommand(CMD_THREAD_CREATED, 0, cmdText)
 
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
            return self.makeErrorMessage(self, seq, sys.exc_info()[0])

    def makeVersionMessage(self, seq):
        try:
            return NetCommand(CMD_VERSION, seq, VERSION_STRING)
        except:
            return self.makeErrorMessage(self, seq, sys.exc_info()[0])

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

    def __init__(self):
        PyDB.instance = self
        self.reader = None
        self.writer = None
        self.quitting = None
        self.cmdFactory = NetCommandFactory() 
        self.cmdQueue = {}

    def initializeNetwork(self, sock):
        sock.settimeout(None) # infinite, no timeouts from now on
        self.reader = ReaderThread(sock)
        self.reader.start()
        self.writer = WriterThread(sock)
        self.writer.start()
        
    def startServer(self, port):
        """ binds to a port, waits for the debugger to connect """
        # TODO untested
        s = socket(AF_INET, SOCK_STREAM)
        s.bind(port)
        s.listen(1)
        newSock, addr = s.accept()
        self.initializeNetwork(newSock)

    def startClient(self, host, port):
        """ connects to a host/port """
        log(1, "Connecting to " + host + ":" + str(port))
        try:
            s = socket(AF_INET, SOCK_STREAM);
            s.settimeout(10) # seconds
            s.connect((host, port))
            log(1, "Connected.")
            self.initializeNetwork(s)
        except timeout, e:
            print "server timed out after 10 seconds, could not connect to " + host + ":" + str(port)
            print "Exiting. Bye!"
            sys.exit(1)

    def connect(self, host, port):
        if (host):
            self.startClient(host, port)
        else:
            self.startServer(port)
             
    def processNetCommand(self, id, seq, text):
        cmd = None
        if (id == CMD_VERSION): # response is version number
            cmd = self.cmdFactory.makeVersionMessage(seq)
        elif (id == CMD_LIST_THREADS): # response is a list of threads
            cmd = self.cmdFactory.makeListThreadsMessage(seq)
        else:
            cmd = self.cmdFactory.makeErrorCommand(seq, "unexpected command " + str(id))
                       
        if cmd: 
            self.writer.addCommand(cmd)

    def getQueueForThread(self, thread):
        """ returns intenal command queue for a given thread.
        if new queue is created, notify the RDB about it """
        myID = id(thread)
        queue = None
        try:
            return self.cmdQueue[myID]
        except KeyError:
            self.cmdQueue[myID] = Queue()
            cmd = self.cmdFactory.makeThreadCreatedMessage(thread)
            print "found a new thread"
            self.writer.addCommand(cmd)
        return self.cmdQueue[myID]
        
    def processQueuedEvents(self, frame, event, arg):
        queue = self.getQueueForThread(threading.currentThread())
        
    def trace_dispatch(self, frame, event, arg):
        self.processQueuedEvents(frame, event, arg)
        if self.quitting:
            return # None
        if event == 'line':
            return self.dispatch_line(frame)
        if event == 'call':
            return self.dispatch_call(frame, arg)
        if event == 'return':
            return self.dispatch_return(frame, arg)
        if event == 'exception':
            return self.dispatch_exception(frame, arg)
        print 'bdb.Bdb.dispatch: unknown debugging event:', `event`
        return self.trace_dispatch

    def dispatch_line(self, frame):
        print "L " + str(frame)
        return self.trace_dispatch

    def dispatch_call(self, frame, arg):
        print "C " + str(frame) + " " + str(arg)
        return self.trace_dispatch

    def dispatch_return(self, frame, arg):
        print "R " + str(frame) + " " + str(arg)
        return self.trace_dispatch

    def dispatch_exception(self, frame, arg):
        print "E " + str(frame) + " " + str(arg)
        return self.trace_dispatch

    def run(self, cmd, globals=None, locals=None):    
        if globals is None:
            import __main__
            globals = __main__.__dict__
        if locals is None:
            locals = globals

        if not isinstance(cmd, types.CodeType):
            cmd = cmd+'\n'

        threading.settrace(self.trace_dispatch) # for all threads
        sys.settrace(self.trace_dispatch) # for this thread
        try:
            try:
                exec cmd in globals, locals
            except:
                print sys.stderr, "Debugger exiting with exception"
                raise
        finally:
            print "Quitting now"
            self.quitting = 1
  
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
    log(1, "Exit function called. Bye")

if __name__ == '__main__':
    print sys.stderr, "pydev debugger"
    # parse the command line. --file is our last argument that is required
    try:
        setup = processCommandLine(sys.argv)
    except ValueError, e:
        print e
        usage(1)
    log(2, "Executing file " + setup['file'])
    log(2, "arguments:" + str(sys.argv))
 
    import atexit
    atexit.register(quittingNow)

    debugger = PyDB()
    debugger.connect(setup['client'], setup['port'])
    debugger.run('execfile(' + `setup['file']` + ')', None, None)
    