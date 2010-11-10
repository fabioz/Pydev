import unittest as python_unittest
import pydev_runfiles_xml_rpc
import time
import pydevd_io

try:
    enumerate
except NameError:
    #Jython 2.1 does not have it
    def enumerate(lst):
        ret = []
        i = 0
        for a in lst:
            ret.append((i, a))
            i += 1
        return ret
    
    
#=======================================================================================================================
# PydevTextTestRunner
#=======================================================================================================================
class PydevTextTestRunner(python_unittest.TextTestRunner):
    
    def _makeResult(self):
        return PydevTestResult(self.stream, self.descriptions, self.verbosity)


_PythonTextTestResult = python_unittest.TextTestRunner()._makeResult().__class__

#=======================================================================================================================
# PydevTestResult
#=======================================================================================================================
class PydevTestResult(_PythonTextTestResult):
    

    def startTest(self, test):
        _PythonTextTestResult.startTest(self, test)
        self.buf = pydevd_io.StartRedirect(keep_original_redirection=True, std='both')
        self.start_time = time.time()
        self._current_errors_stack = []
        self._current_failures_stack = []


    def stopTest(self, test):
        end_time = time.time()
        pydevd_io.EndRedirect(std='both')
        
        _PythonTextTestResult.stopTest(self, test)
        
        captured_output = self.buf.getvalue()
        del self.buf
        error_contents = ''
        try:
            test_name = test.__class__.__name__+"."+test._testMethodName
        except AttributeError:
            #Support for jython 2.1 (__testMethodName is pseudo-private in the test case)
            test_name = test.__class__.__name__+"."+test._TestCase__testMethodName
            
        
        if not self._current_errors_stack and not self._current_failures_stack:
            pydev_runfiles_xml_rpc.NotifyTest(
                'ok', captured_output, error_contents, test.__pydev_pyfile__, test_name, end_time-self.start_time)
        else:
            error_contents = []
            for test, s in self._current_errors_stack+self._current_failures_stack:
                error_contents.append(s)
            
            error_contents = ('\n'+self.separator1).join(error_contents)
            if self._current_errors_stack and not self._current_failures_stack:
                pydev_runfiles_xml_rpc.NotifyTest(
                    'error', captured_output, error_contents, test.__pydev_pyfile__, test_name, end_time-self.start_time)
                
            elif self._current_failures_stack and not self._current_errors_stack:
                pydev_runfiles_xml_rpc.NotifyTest(
                    'fail', captured_output, error_contents, test.__pydev_pyfile__, test_name, end_time-self.start_time)
            
            else: #Ok, we got both, errors and failures. Let's mark it as an error in the end.
                pydev_runfiles_xml_rpc.NotifyTest(
                    'error', captured_output, error_contents, test.__pydev_pyfile__, test_name, end_time-self.start_time)
                


    def addError(self, test, err):
        _PythonTextTestResult.addError(self, test, err)
        self._current_errors_stack.append(self.errors[-1])


    def addFailure(self, test, err):
        _PythonTextTestResult.addFailure(self, test, err)
        self._current_failures_stack.append(self.failures[-1])



#=======================================================================================================================
# PydevTestSuite
#=======================================================================================================================
class PydevTestSuite(python_unittest.TestSuite):


    def run(self, result):
        for index, test in enumerate(self._tests):
            if result.shouldStop:
                break
            test(result)

            # Let the memory be released! 
            self._tests[index] = None

        return result


