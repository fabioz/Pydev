try:
    try:
        import xmlrpclib
    except ImportError:
        import xmlrpc.client as xmlrpclib
except ImportError:
    import _pydev_xmlrpclib as xmlrpclib


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
        _ServerHolder.SERVER = xmlrpclib.Server('http://%s:%s' % ('localhost', port))
        
    _ServerHolder.SERVER.notifyConnected()
    
    
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
    _ServerHolder.SERVER.notifyTest(cond, captured_output, error_contents, file, test, time)
