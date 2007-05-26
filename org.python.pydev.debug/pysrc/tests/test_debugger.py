'''
    The idea is that we record the commands sent to the debugger and reproduce them from this script
    (so, this works as the client, which spaws the debugger as a separate process and communicates
    to it as if it was run from the outside)
'''
port = 13333

import os
def NormFile(filename):
    try:
        rPath = os.path.realpath #@UndefinedVariable
    except:
        # jython does not support os.path.realpath
        # realpath is a no-op on systems without islink support
        rPath = os.path.abspath   
    return os.path.normcase(rPath(filename))

TEST_FILE = NormFile('_debugger_case1.py')
PYDEVD_FILE = NormFile('../pydevd.py')

args = [
'python',
PYDEVD_FILE, 
'--type', 
'python', 
'--client', 
'localhost', 
'--port', 
str(port), 
'--file', 
TEST_FILE]


import subprocess
import sys
import socket
import threading
import time

class ReaderThread(threading.Thread):
    
    def __init__(self, sock):
        threading.Thread.__init__(self)
        self.setDaemon(True)
        self.sock = sock
        self.lastReceived = None
        
    def run(self):
        try:
            while True:
                self.lastReceived = self.sock.recv(1024)
                print 'Test Reader Thread Received %s' % self.lastReceived.strip()
        except:
            pass #ok, finished it
    
class WriterThread(threading.Thread):
    
    def __init__(self):
        threading.Thread.__init__(self)
        self.setDaemon(True)
    
    def run(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind(('', port))
        s.listen(1)
        newSock, addr = s.accept()
        print 'Test Writer Thread Received', newSock, addr
        readerThread = self.readerThread = ReaderThread(newSock)
        readerThread.start()
        self.newSock = newSock

        self.Write("501\t1\t1.0")
        
        #add breakpoint
        self.Write("111\t3\t%s\t39\t**FUNC**SetUp\tNone" % (TEST_FILE,))
        
        #run
        self.Write("101\t5\t")
        
        #wait for hit breakpoint
        while not 'stop_reason="111"' in readerThread.lastReceived:
            time.sleep(1)
            
        #we have something like <xml><thread id="12152656" stop_reason="111"><frame id="12453120" ...
        splitted = readerThread.lastReceived.split('"')
        threadId = splitted[1]
        frameId = splitted[5]
        
        #get frame
        self.Write("114\t7\t%s\t%s\tFRAME" % (threadId, frameId))

        #step over
        self.Write("108\t9\t%s" % (threadId,))
        
        #get frame
        self.Write("114\t11\t%s\t%s\tFRAME" % (threadId, frameId))
        
        #run
        self.Write("106\t13\t%s" % (threadId,))
        
        
    def Write(self, s):
        last = self.readerThread.lastReceived
        print 'Test Writer Thread Written %s' % (s,)
        self.newSock.send(s+'\n')
        while last == self.readerThread.lastReceived:
            time.sleep(0.2)
        
WriterThread().start()

process = subprocess.Popen(args)
process.wait()