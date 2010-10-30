try:
    try:
        import xmlrpclib
    except ImportError:
        import xmlrpc.client as xmlrpclib
except ImportError:
    import _pydev_xmlrpclib as xmlrpclib


#=======================================================================================================================
# InitializeServer
#=======================================================================================================================
def InitializeServer(port):
    global SERVER
    SERVER = xmlrpclib.Server('http://%s:%s' % ('localhost', port))
    SERVER.notifyConnected()
    
    
#=======================================================================================================================
# NotifyTest
#=======================================================================================================================
def NotifyTest(cond, captured_output, error_contents, file, test):
    '''
    @param cond: ok, fail, error
    @param file: the tests file (c:/temp/test.py)
    @param test: the test ran (i.e.: TestCase.test1)
    '''
    SERVER.notifyTest(cond, captured_output, error_contents, file, test)
