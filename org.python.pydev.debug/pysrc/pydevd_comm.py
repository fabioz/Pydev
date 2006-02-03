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
    104      THREAD_KILL     RDB       id                  kills the thread
                             PYDB      id                  nofies RDB that thread was killed
    105      THREAD_SUSPEND  RDB       XML of the stack,   suspends the thread
                                       reason for suspension
                             PYDB      id                  notifies RDB that thread was suspended
    106      THREAD_RUN      RDB       id                  resume the thread
                             PYDB      id \t reason        notifies RDB that thread was resumed
    107      STEP_INTO       RDB       thread_id
    108      STEP_OVER       RDB       thread_id
    109      STEP_RETURN     RDB       thread_id
    110      GET_VARIABLE    RDB       var_locator         GET_VARIABLE with XML of var content
                                       see code for definition
    111      SET_BREAK       RDB       file/line of the breakpoint
    112      REMOVE_BREAK    RDB       file/line of the return
    113      EVALUATE_EXPRESSION RDB   expression          result of evaluating the expression

500 series diagnostics/ok
    901      VERSION         either      Version string (1.0)               Currently just used at startup
    902      RETURN          either      Depends on caller    -
900 series: errors
    501      ERROR           either      -                 This is reserved for unexpected errors.
                                  
    * RDB - remote debugger, the java end
    * PYDB - pydevd, the python end
"""
import traceback
import StringIO

try:
    __setFalse = False
except:
    False = 0
    True = 1


import os.path
import time
import sys
import threading
import Queue as PydevQueue
from socket import socket
from socket import AF_INET
from socket import SOCK_STREAM
from socket import error
import urllib
import string
import pydevd_vars


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
CMD_GET_FRAME = 114
CMD_VERSION = 501
CMD_RETURN = 502
CMD_ERROR = 901 

VERSION_STRING = "1.0"


#--------------------------------------------------------------------------------------------------- UTILITIES

pydevd_trace = -1

def pydevd_log(level, s):
    """ levels are: 
        0 most serious warnings/errors
        1 warnings/significant events
        2 informational trace
    """
    if level <= pydevd_trace:
        print >>sys.stderr, s 


def NormFile(filename):
    try:
        rPath = os.path.realpath
    except:
        # jython does not support os.path.realpath
        # realpath is a no-op on systems without islink support
        rPath = os.path.abspath    
    return os.path.normcase(rPath(filename))

globalDbg = None
def GetGlobalDebugger():
    return globalDbg

def SetGlobalDebugger(dbg):
    global globalDbg
    globalDbg = dbg


#------------------------------------------------------------------- ACTUAL COMM

class PyDBDaemonThread(threading.Thread):

    def __init__(self):
        threading.Thread.__init__(self)
        self.setDaemon(True)
        self.killReceived = False

    def doKill(self):
        self.killReceived = True
        if hasattr(self, 'sock'):
            try:
                self.sock.close()
            except:
                #just ignore that
                pass
            
class ReaderThread(PyDBDaemonThread):
    """ reader thread reads and dispatches commands in an infinite loop """
    
    def __init__(self, sock):
        PyDBDaemonThread.__init__(self)
        self.sock = sock
        self.setName("pydevd.Reader")
     
    def run(self):
        sys.settrace(None) # no debugging on this thread
        buffer = ""
        try:
            while not self.killReceived:
                buffer += self.sock.recv(1024)
                while buffer.find('\n') != -1:
                    command, buffer = buffer.split('\n', 1)
                    pydevd_log(1, "received command " + command)
                    args = command.split('\t', 2)
                    globalDbg.processNetCommand(int(args[0]), int(args[1]), args[2])
        except:
            traceback.print_exc()


#----------------------------------------------------------------------------------- SOCKET UTILITIES - WRITER
class WriterThread(PyDBDaemonThread):
    """ writer thread writes out the commands in an infinite loop """
    def __init__(self, sock):
        PyDBDaemonThread.__init__(self)
        self.sock = sock
        self.setName("pydevd.Writer")
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
            while not self.killReceived:
                cmd = self.cmdQueue.get(1)
                out = cmd.getOutgoing()
                pydevd_log(1, "sending cmd " + out)
                bytesSent = self.sock.send(out) #TODO: this does not guarantee that all message are sent (and jython does not have a send all)
                time.sleep(self.timeout)                
        except Exception, e:
            traceback.print_exc()
    
    




    
#------------------------------------------------------------------------------------ MANY COMMUNICATION STUFF
    
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
        name = pydevd_vars.makeValidXmlValue(thread.getName())
        cmdText = '<thread name="%s" id="%s" />' % (urllib.quote(name), id(thread) )
        return cmdText

    def makeErrorMessage(self, seq, text):
        cmd = NetCommand(CMD_ERROR, seq, text)
        #print >>sys.stderr, "Error: ", text
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
            <thread id="id" stop_reason="reason">
                    <frame id="id" name="functionName " file="file" line="line">
                    <var variable stuffff....
                </frame>
            </thread>
           """
        try:
            cmdTextList = ["<xml>"]
            cmdTextList.append('<thread id="%s" stop_reason="%s">' % (str(thread_id), str(stop_reason)))
            
            curFrame = frame
            while curFrame:
                #print cmdText
                myId = str(id(curFrame))
                #print "id is ", myId
                
                myName = curFrame.f_code.co_name
                #print "name is ", myName
                
                myFile = NormFile( curFrame.f_code.co_filename )                
                #myFile = inspect.getsourcefile(curFrame) or inspect.getfile(frame)
                #print "file is ", myFile
                
                myLine = str(curFrame.f_lineno)
                #print "line is ", myLine
                
                #the variables are all goten 'on-demand'
                #variables = pydevd_vars.frameVarsToXML(curFrame)

                variables = ''
                cmdTextList.append( '<frame id="%s" name="%s" ' % (myId , pydevd_vars.makeValidXmlValue(myName))) 
                cmdTextList.append( 'file="%s" line="%s">"'     % (urllib.quote(myFile, '/>_= \t'), myLine)) 
                cmdTextList.append( variables  ) 
                cmdTextList.append( "</frame>" ) 
                curFrame = curFrame.f_back
            
            cmdTextList.append( "</thread></xml>" )
            cmdText = ''.join(cmdTextList)
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

    def makeGetFrameMessage(self, seq, payload):
        try:
            return NetCommand(CMD_GET_FRAME, seq, payload)
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
    
    def canBeExecutedBy(self, thread_id):
        '''By default, it must be in the same thread to be executed
        '''
        return self.thread_id == thread_id

    def doIt(self, dbg):
        raise NotImplementedError("you have to override doIt")

class InternalTerminateThread(InternalThreadCommand):
    def __init__(self, thread_id):
        self.thread_id = thread_id

    def doIt(self, dbg):
        pydevd_log(1,  "killing " + str(self.thread_id))
        cmd = dbg.cmdFactory.makeThreadKilledMessage(self.thread_id)
        dbg.writer.addCommand(cmd)
        time.sleep(0.1)
        try:
            import java.lang.System
            java.lang.System.exit(0)
        except:
            sys.exit(0)

class InternalGetVariable(InternalThreadCommand):
    """ gets the value of a variable """
    def __init__(self, seq, thread_id, frame_id, scope, attrs):
        self.sequence = seq
        self.thread_id = thread_id
        self.frame_id = frame_id
        self.scope = scope
        self.attributes = attrs
     
    def doIt(self, dbg):
        """ Converts request into python variable """
        try:
            xml = "<xml>"            
            valDict = pydevd_vars.resolveCompoundVariable(self.thread_id, self.frame_id, self.scope, self.attributes)                        
            keys = valDict.keys()
            keys.sort()
            for k in keys:
                xml += pydevd_vars.varToXML(valDict[k], str(k))

            xml += "</xml>"
            cmd = dbg.cmdFactory.makeGetVariableMessage(self.sequence, xml)
            dbg.writer.addCommand(cmd)
        except Exception:
            exc_info = sys.exc_info()
            s = StringIO.StringIO()
            traceback.print_exception(exc_info[0], exc_info[1], exc_info[2], file = s)
            cmd = dbg.cmdFactory.makeErrorMessage(self.sequence, "Error resolving variables " + s.getvalue())
            dbg.writer.addCommand(cmd)


class InternalGetFrame(InternalThreadCommand):
    """ gets the value of a variable """
    def __init__(self, seq, thread_id, frame_id):
        self.sequence = seq
        self.thread_id = thread_id
        self.frame_id = frame_id
     
    def doIt(self, dbg):
        """ Converts request into python variable """
        try:
            xml = "<xml>"            
            frame = pydevd_vars.findFrame(self.thread_id, self.frame_id)
            xml += pydevd_vars.frameVarsToXML(frame)
            xml += "</xml>"
            cmd = dbg.cmdFactory.makeGetFrameMessage(self.sequence, xml)
            dbg.writer.addCommand(cmd)
        except Exception:
            exc_info = sys.exc_info()
            s = StringIO.StringIO()
            traceback.print_exception(exc_info[0], exc_info[1], exc_info[2], file = s)
            cmd = dbg.cmdFactory.makeErrorMessage(self.sequence, "Error resolving frame" + s.getvalue())
            dbg.writer.addCommand(cmd)

           

           
class InternalEvaluateExpression(InternalThreadCommand):
    """ gets the value of a variable """

    def __init__(self, seq, thread_id, frame_id, expression):
        self.sequence = seq
        self.thread_id = thread_id
        self.frame_id = frame_id
        self.expression = expression
    
    def doIt(self, dbg):
        """ Converts request into python variable """
        try:
            result = pydevd_vars.evaluateExpression( self.thread_id, self.frame_id, self.expression )
            xml = "<xml>"
            xml += pydevd_vars.varToXML(result, "")
            xml += "</xml>"
            cmd = dbg.cmdFactory.makeEvaluateExpressionMessage(self.sequence, xml)
            dbg.writer.addCommand(cmd)
        except Exception, e:
            traceback.print_exc()
            cmd = dbg.cmdFactory.makeErrorMessage(self.sequence, "Error evaluating expression " + str(e))
            dbg.writer.addCommand(cmd)


def pydevd_findThreadById(thread_id):
    try:
        thread_id = long(thread_id)
        # there was a deadlock here when I did not remove the tracing function when thread was dead
        threads = threading.enumerate()
        for i in threads:
            if thread_id == id(i): 
                return i
            
        print >>sys.stderr, "could not find thread %s" % thread_id
    except:
        traceback.print_exc()
        
    return None


