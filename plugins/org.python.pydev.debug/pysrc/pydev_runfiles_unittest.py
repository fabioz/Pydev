import unittest as python_unittest
import pydev_runfiles_xml_rpc
import time

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
        original = python_unittest.TextTestRunner._makeResult(self)
        return PydevTestResult(original)


#=======================================================================================================================
# PydevTestResult
#=======================================================================================================================
class PydevTestResult(python_unittest.TestResult):
    
    def __init__(self, wrapped):
        python_unittest.TestResult.__init__(self)
        self.wrapped = wrapped


    def startTest(self, test):
        python_unittest.TestResult.startTest(self, test)
        self.wrapped.startTest(test)
        self.start_time = time.time()


    def stopTest(self, test):
        self.end_time = time.time()
        python_unittest.TestResult.stopTest(self, test)
        self.wrapped.stopTest(test)
        if self.wasSuccessful():
            captured_output, error_contents = '', ''
            test_name = test.__class__.__name__+"."+test._testMethodName
            pydev_runfiles_xml_rpc.NotifyTest(
                'ok', captured_output, error_contents, test.__pydev_pyfile__, test_name, self.end_time-self.start_time)


    def addError(self, test, err):
        python_unittest.TestResult.addError(self, test, err)
        self.wrapped.addError(test, err)


    def addFailure(self, test, err):
        python_unittest.TestResult.addFailure(self, test, err)
        self.wrapped.addFailure(test, err)


    def addSuccess(self, test):
        python_unittest.TestResult.addSuccess(self, test)
        self.wrapped.addSuccess(test)


    def wasSuccessful(self):
        return python_unittest.TestResult.wasSuccessful(self)
    

    def stop(self):
        self.wrapped.stop()
        return python_unittest.TestResult.stop(self)
    
    
    separator1 = '=' * 70
    separator2 = '-' * 70


    def printErrors(self):
        self.wrapped.printErrors()

    def printErrorList(self, flavour, errors):
        self.wrapped.printErrorList(flavour, errors)



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


