from nose.plugins.multiprocess import MultiProcess, MultiProcessTestRunner
from nose.plugins.base import Plugin
import time
import sys
import pydev_runfiles_xml_rpc

#===============================================================================
# PydevPlugin
#===============================================================================
class PydevPlugin(Plugin):
    
    def reportCond(self, cond, address, captured_output='', error_contents=''):
        '''
        @param cond: fail, error, ok
        @param address: list(location, test)
            E.g.: ['D:\\src\\mod1\\hello.py', 'TestCase.testMet1']
        '''
        pydev_runfiles_xml_rpc.NotifyTest(cond, captured_output, error_contents, *address)
        
        
    def convertAddr(self, addr):
        '''
        @param addr: tuple
            Something as:
            ('D:\\workspaces\\temp\\test_workspace\\pytesting1\\src\\mod1\\hello.py', 'mod1.hello', 'TestCase.testMet1')
        '''
        return [addr[0], addr[2]]
    
    def getIoFromError(self, err):
        from StringIO import StringIO
        s = StringIO()
        etype, value, tb = err
        import traceback;traceback.print_exception(etype, value, tb, file=s)
        return s.getvalue()
    
    def getCapturedOutput(self, test):
        if test.capturedOutput:
            return test.capturedOutput
        return ''
    
    def addError(self, test, err):
        self.reportCond(
            'error', 
            self.convertAddr(test.address()), 
            self.getCapturedOutput(test), 
            self.getIoFromError(err), 
        )


    def addFailure(self, test, err):
        self.reportCond(
            'fail', 
            self.convertAddr(test.address()), 
            self.getCapturedOutput(test), 
            self.getIoFromError(err), 
        )


    def addSuccess(self, test):
        self.reportCond(
            'ok', 
            self.convertAddr(test.address()), 
            self.getCapturedOutput(test), 
        )
        
        
PYDEV_NOSE_PLUGIN_SINGLETON = PydevPlugin()

        
        


original = MultiProcessTestRunner.consolidate
#===============================================================================
# NewConsolidate
#===============================================================================
def NewConsolidate(self, result, batch_result):
    '''
    Used so that it can work with the multiprocess plugin. 
    Monkeypatched because nose seems a bit unsupported at this time (ideally
    the plugin would have this support by default).
    '''
    ret = original(self, result, batch_result)
    
    parent_frame = sys._getframe().f_back
    #addr is something as D:\pytesting1\src\mod1\hello.py:TestCase.testMet4
    #so, convert it to what reportCond expects
    addr = parent_frame.f_locals['addr']
    i = addr.rindex(':')
    addr = [addr[:i], addr[i+1:]]
    
    output, testsRun, failures, errors, errorClasses = batch_result
    if failures or errors:
        for failure in failures:
            PYDEV_NOSE_PLUGIN_SINGLETON.reportCond('fail', addr)
            
        for error in errors:
            PYDEV_NOSE_PLUGIN_SINGLETON.reportCond('error', addr)
    else:
        PYDEV_NOSE_PLUGIN_SINGLETON.reportCond('ok', addr)
        
    
    return ret
    
MultiProcessTestRunner.consolidate = NewConsolidate
