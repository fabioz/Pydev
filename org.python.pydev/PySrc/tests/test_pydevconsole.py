from StringIO import StringIO
import threading
import unittest
import sys
import os

sys.argv[0] = os.path.dirname(sys.argv[0]) 
sys.path.insert(1, os.path.join(  os.path.dirname( sys.argv[0] )) )

import pydevconsole

try:
    from SimpleXMLRPCServer import SimpleXMLRPCServer
except ImportError:
    from _pydev_SimpleXMLRPCServer import SimpleXMLRPCServer
    
try:
    import xmlrpclib
except ImportError:
    import _pydev_xmlrpclib as xmlrpclib
    

#=======================================================================================================================
# Test
#=======================================================================================================================
class Test(unittest.TestCase):

    def setUp(self):
        self.original_stdout = sys.stdout
        sys.stdout = StringIO()
        
        
    def tearDown(self):
        ret = sys.stdout #@UnusedVariable
        sys.stdout = self.original_stdout
        #print ret.getvalue() -- use to see test output

    def testConsoleRequests(self):
        client_port = 7992
        client_thread = self.startClientThread(client_port) #@UnusedVariable
        import time
        time.sleep(.3) #let's give it some time to start the threads
        
        interpreter = pydevconsole.InterpreterInterface('localhost', client_port)
        interpreter.addExec('class Foo:')
        interpreter.addExec('   CONSTANT=1')
        interpreter.addExec('')
        interpreter.addExec('foo=Foo()')
        interpreter.addExec('foo.__doc__=None')
        interpreter.addExec('val = raw_input()')
        interpreter.addExec('50')
        interpreter.addExec('print val')
        self.assertEqual(['50', 'input_request'], sys.stdout.getvalue().split())
        
        comps = interpreter.getCompletions('foo.')
        self.assert_(('CONSTANT', '', '', '3') in comps or ('CONSTANT', '', '', '4') in comps)
        
        comps = interpreter.getCompletions('"".')
        self.assert_(('__add__', 'x.__add__(y) <==> x+y', '', '3') in comps or ('__add__', '', '', '4') in comps)
        
        self.assert_(('AssertionError', '', '', '1') in interpreter.getCompletions(''))
        self.assert_(('RuntimeError', '', '', '1') not in interpreter.getCompletions('Assert'))
        
        self.assert_(('__doc__', None, '', '3') not in interpreter.getCompletions('foo.CO'))
        
        comps = interpreter.getCompletions('va')
        self.assert_(('val', '', '', '3') in comps or ('val', '', '', '4') in comps)
        
        interpreter.addExec('s = "mystring"')
        
        desc = interpreter.getDescription('val')
        self.assert_(desc.find('str(object) -> string') >= 0 or desc == "'input_request'")
        
        desc = interpreter.getDescription('val.join')
        self.assert_(desc.find('S.join(sequence) -> string') >= 0 or desc == "<builtin method 'join'>")

    
    def startClientThread(self, client_port):
        class ClientThread(threading.Thread):
            def __init__(self, client_port):
                threading.Thread.__init__(self)
                self.client_port = client_port
            def run(self):
                class HandleRequestInput:
                    def RequestInput(self):
                        return 'input_request'
                
                handle_request_input = HandleRequestInput()
                
                client_server = SimpleXMLRPCServer(('localhost', self.client_port), logRequests=False)
                client_server.register_function(handle_request_input.RequestInput)
                client_server.serve_forever()
                
        client_thread = ClientThread(client_port)
        client_thread.setDaemon(True)
        client_thread.start()
        return client_thread

        
    def testServer(self):
        client_port = 7991
        server_port = 7988
        class ServerThread(threading.Thread):
            def __init__(self, client_port, server_port):
                threading.Thread.__init__(self)
                self.client_port = client_port
                self.server_port = server_port
                
            def run(self):
                pydevconsole.StartServer('localhost', self.server_port, self.client_port)
        server_thread = ServerThread(client_port, server_port)
        server_thread.setDaemon(True)
        server_thread.start()

        client_thread = self.startClientThread(client_port) #@UnusedVariable
        
        import time
        time.sleep(.3) #let's give it some time to start the threads
        
        server = xmlrpclib.Server('http://localhost:%s' % server_port)
        server.addExec('class Foo:')
        server.addExec('    pass')
        server.addExec('')
        server.addExec('foo = Foo()')
        server.addExec('a = raw_input()')
        server.addExec('print a')
        self.assertEqual(['input_request'], sys.stdout.getvalue().split())
        
#=======================================================================================================================
# main        
#=======================================================================================================================
if __name__ == '__main__':
    unittest.main()

