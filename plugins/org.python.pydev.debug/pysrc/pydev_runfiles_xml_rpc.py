try:
    try:
        import xmlrpclib
    except ImportError:
        import xmlrpc.client as xmlrpclib
except ImportError:
    import _pydev_xmlrpclib as xmlrpclib

import pydevd_constants

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
            _ServerHolder.SERVER = xmlrpclib.Server('http://%s:%s' % ('localhost', port))
        else:
            #Create a null server, so that we keep the interface even without any connection.
            _ServerHolder.SERVER = pydevd_constants.Null()
        
    _ServerHolder.SERVER.notifyConnected()
    
    
#=======================================================================================================================
# NotifyTest
#=======================================================================================================================
def NotifyTestsCollected(tests_count):
    _ServerHolder.SERVER.notifyTestsCollected(tests_count)
    
    
#=======================================================================================================================
# NotifyTest
#=======================================================================================================================
def NotifyTest(cond, captured_output, error_contents, file, test, time):
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
    _ServerHolder.SERVER.notifyTest(cond, captured_output, error_contents, file, test, time)

#=======================================================================================================================
# NotifyTestRunFinished
#=======================================================================================================================
def NotifyTestRunFinished():
    _ServerHolder.SERVER.notifyTestRunFinished()