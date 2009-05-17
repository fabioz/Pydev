'''
@author Grig Gheorghiu
'''

import unittest, sys, traceback, os
progdir = os.path.dirname(sys.argv[0])
sys.path.append(progdir)
import unittest2

HOST = '127.0.0.1'  
DEBUG = 1     

class SocketTestRunner(unittest2.TestListener):
    def __init__(self, port, test_dir, test_modules):
        self._host = HOST
        self._port = port
        self._socket = None
        self._test_dir = os.path.normpath(os.path.abspath(test_dir))
        self._test_modules = test_modules
        self._suite = unittest.TestSuite()
        self._debug = DEBUG
        if self._debug:
            self._log = open("\\SocketTestRunner.log", "w")
        self.log_msg("test_dir = %s" % self._test_dir)

    def __del__(self):
        if self._debug:
            self._log.close()

    def log_msg(self, msg):
        if not self._debug:
            return
        self._log.write('%s\n' % (msg,))
        
    def log_exception(self, msg=""):
        if not self._debug:
            return
        if msg:
            self.log_msg(msg)
        err = sys.exc_info()
        trace = traceback.extract_tb(err[2])
        for tb_entry in trace:
            filename, line_number, function_name, text = tb_entry
            line = "File \"%s\", line %s, in %s\n%s" % (filename, line_number, function_name, text)
            self.log_msg(line)
            
    def openClientSocket(self):
        import socket
        self._socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            self._socket.connect((self._host, self._port))
        except:
            self.log_exception("Exception in openClientSocket")
            
    def writeSocket(self, msg):
        if type(msg) == type(""):
            self.log_msg(msg)
            self._socket.send(msg + "\n")
        else:
            for tb_entry in msg:
                filename, line_number, function_name, text = tb_entry
                line = "File \"%s\", line %s, in %s\n%s\n" % (filename, line_number, function_name, text)
                self.log_msg(line)
                self._socket.send(line)
        
    def runTests(self):
        try:
            os.chdir(self._test_dir)
            sys.path.append(self._test_dir)
            for test_module in self._test_modules:
                self.log_msg("test_module = %s" % test_module)
                loader = unittest.TestLoader()
                suite = loader.loadTestsFromName(test_module)
                self._suite.addTest(suite)
                self.log_msg("suite %s" % str(suite))
        except:
            self.log_exception("Exception in runTests")
        self.openClientSocket()
        self.writeSocket("starting tests " + str(self._suite.countTestCases()))
        result = unittest2.TestResultWithListeners()
        result.addListener(self)
        self._suite.run(result)
        self.writeSocket("ending tests")
            
    def addError(self, test, err):
        self.writeSocket("failing test " + str(test))
        self.writeSocket(traceback.extract_tb(err[2]))
        self.writeSocket("END TRACE")
        
    def addFailure(self, test, err):
        self.addError(test, err)

    def endTest(self, test):
        pass

    def startTest(self, test):
        self.writeSocket("starting test " + str(test))

if __name__ == "__main__":
    port = int(sys.argv[1])
    test_dir = sys.argv[2]
    test_modules = sys.argv[3:]
    runner = SocketTestRunner(port, test_dir, test_modules)
    runner.runTests()
