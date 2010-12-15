from pydev_imports import xmlrpclib
import pydevd_constants
import traceback



#=======================================================================================================================
# _ServerHolder
#=======================================================================================================================
class _ServerHolder:
    '''
    Helper so that we don't have to use a global here.
    '''
    SERVER = None


#=======================================================================================================================
# SetServer
#=======================================================================================================================
def SetServer(server):
    _ServerHolder.SERVER = server


#=======================================================================================================================
# InitializeServer
#=======================================================================================================================
def InitializeServer(port):
    if _ServerHolder.SERVER is None:
        if port is not None:
            import pydev_localhost
            _ServerHolder.SERVER = xmlrpclib.Server('http://%s:%s' % (pydev_localhost.get_localhost(), port))
        else:
            #Create a null server, so that we keep the interface even without any connection.
            _ServerHolder.SERVER = pydevd_constants.Null()
        
    try:
        _ServerHolder.SERVER.notifyConnected()
    except:
        traceback.print_exc()

    
    
#=======================================================================================================================
# notifyTest
#=======================================================================================================================
def notifyTestsCollected(tests_count):
    assert tests_count is not None
    try:
        _ServerHolder.SERVER.notifyTestsCollected(tests_count)
    except:
        traceback.print_exc()
    
    
#=======================================================================================================================
# notifyStartTest
#=======================================================================================================================
def notifyStartTest(file, test):
    '''
    @param file: the tests file (c:/temp/test.py)
    @param test: the test ran (i.e.: TestCase.test1)
    '''
    assert file is not None
    if test is None:
        test = '' #Could happen if we have an import error importing module.
        
    try:
        _ServerHolder.SERVER.notifyStartTest(file, test)
    except:
        traceback.print_exc()

    
#=======================================================================================================================
# notifyTest
#=======================================================================================================================
def notifyTest(cond, captured_output, error_contents, file, test, time):
    '''
    @param cond: ok, fail, error
    @param captured_output: output captured from stdout
    @param captured_output: output captured from stderr
    @param file: the tests file (c:/temp/test.py)
    @param test: the test ran (i.e.: TestCase.test1)
    @param time: float with the number of seconds elapsed
    '''
    assert cond is not None
    assert captured_output is not None
    assert error_contents is not None
    assert file is not None
    if test is None:
        test = '' #Could happen if we have an import error importing module.
    assert time is not None
    try:
        _ServerHolder.SERVER.notifyTest(cond, captured_output, error_contents, file, test, time)
    except:
        traceback.print_exc()

#=======================================================================================================================
# notifyTestRunFinished
#=======================================================================================================================
def notifyTestRunFinished():
    try:
        _ServerHolder.SERVER.notifyTestRunFinished()
    except:
        traceback.print_exc()
    
    