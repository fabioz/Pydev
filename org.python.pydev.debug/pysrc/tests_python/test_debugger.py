'''
    The idea is that we record the commands sent to the debugger and reproduce them from this script
    (so, this works as the client, which spaws the debugger as a separate process and communicates
    to it as if it was run from the outside)
'''
import unittest 
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

PYDEVD_FILE = NormFile('../pydevd.py')

SHOW_WRITES_AND_READS = False


import subprocess
import sys
import socket
import threading
import time

#=======================================================================================================================
# ReaderThread
#=======================================================================================================================
class ReaderThread(threading.Thread):
    
    def __init__(self, sock):
        threading.Thread.__init__(self)
        self.setDaemon(True)
        self.sock = sock
        self.lastReceived = None
        
    def run(self):
        try:
            buf = ''
            while True:
                l = self.sock.recv(1024)
                buf += l
                
                if '\n' in buf:
                    self.lastReceived = buf
                    buf = ''
                    
                if SHOW_WRITES_AND_READS:
                    print 'Test Reader Thread Received %s' % self.lastReceived.strip()
        except:
            pass #ok, finished it
    
    def DoKill(self):
        self.sock.close()
    
#=======================================================================================================================
# AbstractWriterThread
#=======================================================================================================================
class AbstractWriterThread(threading.Thread):
    
    def __init__(self):
        threading.Thread.__init__(self)
        self.setDaemon(True)
        
    def DoKill(self):
        self.readerThread.DoKill()
        self.sock.close()
        
    def Write(self, s):
        last = self.readerThread.lastReceived
        if SHOW_WRITES_AND_READS:
            print 'Test Writer Thread Written %s' % (s,)
        self.sock.send(s+'\n')
        time.sleep(0.2)
        
        i = 0
        while last == self.readerThread.lastReceived and i < 10:
            i += 1
            time.sleep(0.1)
        
    
    def StartSocket(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind(('', port))
        s.listen(1)
        newSock, addr = s.accept()
        if SHOW_WRITES_AND_READS:
            print 'Test Writer Thread Socket:', newSock, addr
            
        readerThread = self.readerThread = ReaderThread(newSock)
        readerThread.start()
        self.sock = newSock
        
        self._sequence = -1
        #initial command is always the version
        self.WriteVersion()
    
    def NextSeq(self):
        self._sequence += 2
        return self._sequence
            
    def WaitForBreakpointHit(self):
        i = 0
        #wait for hit breakpoint
        while not 'stop_reason="111"' in self.readerThread.lastReceived:
            i += 1
            time.sleep(1)
            if i >= 10:
                raise AssertionError('After %s seconds, a break was not hit.' % i)
            
        #we have something like <xml><thread id="12152656" stop_reason="111"><frame id="12453120" ...
        splitted = self.readerThread.lastReceived.split('"')
        threadId = splitted[1]
        frameId = splitted[5]
        return threadId, frameId
        

    def WriteMakeInitialRun(self):
        self.Write("101\t%s\t" % self.NextSeq())
        
    def WriteVersion(self):
        self.Write("501\t%s\t1.0" % self.NextSeq())
        
    def WriteAddBreakpoint(self, line, func):
        self.Write("111\t%s\t%s\t%s\t**FUNC**%s\tNone" % (self.NextSeq(), self.TEST_FILE, line, func))

    def WriteGetFrame(self, threadId, frameId):
        self.Write("114\t%s\t%s\t%s\tFRAME" % (self.NextSeq(), threadId, frameId))
        
    def WriteStepOver(self, threadId):
        self.Write("108\t%s\t%s" % (self.NextSeq(), threadId,))
        
    def WriteRunThread(self, threadId):
        self.Write("106\t%s\t%s" % (self.NextSeq(), threadId,))
        
    def WriteKillThread(self, threadId):
        self.Write("104\t%s\t%s" % (self.NextSeq(), threadId,))
        
#=======================================================================================================================
# WriterThreadCase2
#=======================================================================================================================
class WriterThreadCase2(AbstractWriterThread):
    
    TEST_FILE = NormFile('_debugger_case2.py')
        
    def run(self):
        self.StartSocket()
        self.WriteAddBreakpoint(3, 'Call4') #seq = 3
        self.WriteMakeInitialRun()
        
        threadId, frameId = self.WaitForBreakpointHit()
        
        self.WriteGetFrame(threadId, frameId)
        
        self.WriteAddBreakpoint(14, 'Call2')
        
        self.WriteRunThread(threadId)
        
        threadId, frameId = self.WaitForBreakpointHit()
        
        self.WriteGetFrame(threadId, frameId)
        
        self.WriteRunThread(threadId)
        
        assert 15 == self._sequence, 'Expected 15. Had: %s'  % self._sequence
        
#=======================================================================================================================
# WriterThreadCase1
#=======================================================================================================================
class WriterThreadCase1(AbstractWriterThread):
    
    TEST_FILE = NormFile('_debugger_case1.py')
        
    def run(self):
        self.StartSocket()
        self.WriteAddBreakpoint(6, 'SetUp')
        self.WriteMakeInitialRun()
        
        threadId, frameId = self.WaitForBreakpointHit()
        
        self.WriteGetFrame(threadId, frameId)

        self.WriteStepOver(threadId)
        
        self.WriteGetFrame(threadId, frameId)
        
        self.WriteRunThread(threadId)
        
        assert 13 == self._sequence, 'Expected 13. Had: %s'  % self._sequence
        
        
#=======================================================================================================================
# Test
#=======================================================================================================================
class Test(unittest.TestCase):
    
    def CheckCase(self, writerThreadClass):
        writerThread = writerThreadClass()
        writerThread.start()
        
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
            writerThread.TEST_FILE
        ]
        
        process = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        class ProcessReadThread(threading.Thread):
            def run(self):
                self.resultStr = None
                self.resultStr = process.stdout.read()
                process.stdout.close()
                
            def DoKill(self):
                process.stdout.close()
                
        processReadThread = ProcessReadThread()
        processReadThread.setDaemon(True)
        processReadThread.start()
        
        while writerThread.isAlive():
            time.sleep(.2)
        
        for i in range(10):
            if processReadThread.resultStr is None:
                time.sleep(.5)
            else:
                break
        else:
            writerThread.DoKill()
            
        if processReadThread.resultStr is None:
            self.fail("The other process may still be running -- and didn't give any output")
            
        if 'TEST SUCEEDED' not in processReadThread.resultStr:
            self.fail(processReadThread.resultStr)
            
    def testCase1(self):
        self.CheckCase(WriterThreadCase1)
        
    def testCase2(self):
        self.CheckCase(WriterThreadCase2)
        
        

#=======================================================================================================================
# Main        
#=======================================================================================================================
if __name__ == '__main__':
    unittest.TextTestRunner().run(unittest.makeSuite(Test))

