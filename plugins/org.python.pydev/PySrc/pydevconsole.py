try:
    from code import InteractiveConsole
except ImportError:
    from pydevconsole_code_for_ironpython import InteractiveConsole 

import os
import sys

try:
    False
    True
except NameError: # version < 2.3 -- didn't have the True/False builtins
    import __builtin__
    setattr(__builtin__, 'True', 1) #Python 3.0 does not accept __builtin__.True = 1 in its syntax
    setattr(__builtin__, 'False', 0)

from pydev_imports import xmlrpclib

#=======================================================================================================================
# BaseStdIn
#=======================================================================================================================
class BaseStdIn:
    
    def readline(self, *args, **kwargs):
        #sys.stderr.write('Cannot readline out of the console evaluation\n') -- don't show anything
        #This could happen if the user had done input('enter number).<-- upon entering this, that message would appear,
        #which is not something we want.
        return '\n'
    
    def isatty(self):    
        return False #not really a file
        
    def write(self, *args, **kwargs):
        pass #not available StdIn (but it can be expected to be in the stream interface)
        
    def flush(self, *args, **kwargs):
        pass #not available StdIn (but it can be expected to be in the stream interface)
       
    def read(self, *args, **kwargs):
        #in the interactive interpreter, a read and a readline are the same.
        return self.readline()
    
sys.stdin = BaseStdIn()
    
#=======================================================================================================================
# StdIn
#=======================================================================================================================
class StdIn(BaseStdIn):
    '''
        Object to be added to stdin (to emulate it as non-blocking while the next line arrives)
    '''
    
    def __init__(self, interpreter, host, client_port):
        self.interpreter = interpreter
        self.client_port = client_port
        self.host = host
    
    def readline(self, *args, **kwargs):
        #Ok, callback into the client to see get the new input
        server = xmlrpclib.Server('http://%s:%s' % (self.host, self.client_port))
        requested_input = server.RequestInput()
        if not requested_input:
            return '\n' #Yes, a readline must return something (otherwise we can get an EOFError on the input() call).
        return requested_input


        
try:
    class ExecState:
        FIRST_CALL = True
        PYDEV_CONSOLE_RUN_IN_UI = False #Defines if we should run commands in the UI thread.
        
    from org.python.pydev.core.uiutils import RunInUiThread #@UnresolvedImport
    from java.lang import Runnable #@UnresolvedImport
    class Command(Runnable):
    
        def __init__(self, interpreter, line):
            self.interpreter = interpreter
            self.line = line
            
        def run(self):
            if ExecState.FIRST_CALL:
                ExecState.FIRST_CALL = False
                sys.stdout.write('\nYou are now in a console within Eclipse.\nUse it with care as it can halt the VM.\n')
                sys.stdout.write('Typing a line with "PYDEV_CONSOLE_TOGGLE_RUN_IN_UI"\nwill start executing all the commands in the UI thread.\n\n')
                
            if self.line == 'PYDEV_CONSOLE_TOGGLE_RUN_IN_UI':
                ExecState.PYDEV_CONSOLE_RUN_IN_UI = not ExecState.PYDEV_CONSOLE_RUN_IN_UI
                if ExecState.PYDEV_CONSOLE_RUN_IN_UI:
                    sys.stdout.write('Running commands in UI mode. WARNING: using sys.stdin (i.e.: calling raw_input()) WILL HALT ECLIPSE.\n')
                else:
                    sys.stdout.write('No longer running commands in UI mode.\n')
                self.more = False
            else:
                self.more = self.interpreter.push(self.line)
                
            
    def Sync(runnable):
        if ExecState.PYDEV_CONSOLE_RUN_IN_UI:
            return RunInUiThread.sync(runnable)
        else:
            return runnable.run()
        
except:
    #If things are not there, define a way in which there's no 'real' sync, only the default execution.
    class Command:
    
        def __init__(self, interpreter, line):
            self.interpreter = interpreter
            self.line = line
            
        def run(self):
            self.more = self.interpreter.push(self.line)
        
    def Sync(runnable):
        runnable.run()


try:
    try:
        execfile #Not in Py3k
    except NameError:
        from pydev_imports import execfile
        import builtins #@UnresolvedImport -- only Py3K
        builtins.execfile = execfile
        
except:
    pass


#=======================================================================================================================
# InterpreterInterface
#=======================================================================================================================
class InterpreterInterface:
    '''
        The methods in this class should be registered in the xml-rpc server.
    '''
    
    def __init__(self, host, client_port):
        self.client_port = client_port
        self.host = host
        self.namespace = globals()
        self.interpreter = InteractiveConsole(self.namespace)
        self._input_error_printed = False


        
    def addExec(self, line):
        #f_opened = open('c:/temp/a.txt', 'a')
        #f_opened.write(line+'\n')
        original_in = sys.stdin
        try:
            help = None
            if 'pydoc' in sys.modules:
                pydoc = sys.modules['pydoc'] #Don't import it if it still is not there.
                
                
                if hasattr(pydoc, 'help'):
                    #You never know how will the API be changed, so, let's code defensively here
                    help = pydoc.help
                    if not hasattr(help, 'input'):
                        help = None
        except:
            #Just ignore any error here
            pass
            
        more = False
        try:
            sys.stdin = StdIn(self, self.host, self.client_port)
            try:
                if help is not None:
                    #This will enable the help() function to work.
                    try:
                        try:
                            help.input = sys.stdin 
                        except AttributeError:
                            help._input = sys.stdin 
                    except:
                        help = None
                        if not self._input_error_printed:
                            self._input_error_printed = True
                            sys.stderr.write('\nError when trying to update pydoc.help.input\n')
                            sys.stderr.write('(help() may not work -- please report this as a bug in the pydev bugtracker).\n\n')
                            import traceback;traceback.print_exc()
                
                try:
                    command = Command(self.interpreter, line)
                    Sync(command)
                    more = command.more
                finally:
                    if help is not None:
                        try:
                            try:
                                help.input = original_in
                            except AttributeError:
                                help._input = original_in
                        except:
                            pass
                        
            finally:
                sys.stdin = original_in
        except SystemExit:
            raise
        except:
            import traceback;traceback.print_exc()
        
        #it's always false at this point
        need_input = False
        return more, need_input
        
            
    def getCompletions(self, text):
        try:
            from _completer import Completer
            completer = Completer(self.namespace, None)
            return completer.complete(text)
        except:
            import traceback;traceback.print_exc()
            return []
        
    
    def getDescription(self, text):
        try:
            obj = None
            if '.' not in text:
                try:
                    obj = self.namespace[text]
                except KeyError:
                    return ''
                    
            else:
                try:
                    splitted = text.split('.')
                    obj = self.namespace[splitted[0]]
                    for t in splitted[1:]:
                        obj = getattr(obj, t)
                except:
                    return ''
                    
                
            if obj is not None:
                try:
                    if sys.platform.startswith("java"):
                        #Jython
                        doc = obj.__doc__
                        if doc is not None:
                            return doc
                        
                        import jyimportsTipper
                        is_method, infos = jyimportsTipper.ismethod(obj)
                        ret = ''
                        if is_method:
                            for info in infos:
                                ret += info.getAsDoc()
                            return ret
                            
                    else:
                        #Python and Iron Python
                        import inspect #@UnresolvedImport
                        doc = inspect.getdoc(obj) 
                        if doc is not None:
                            return doc
                except:
                    pass
                    
            try:
                #if no attempt succeeded, try to return repr()... 
                return repr(obj)
            except:
                try:
                    #otherwise the class 
                    return str(obj.__class__)
                except:
                    #if all fails, go to an empty string 
                    return ''
        except:
            import traceback;traceback.print_exc()
            return ''
        
    
    def close(self):
        sys.exit(0)
        
    
    
#=======================================================================================================================
# _DoExit
#=======================================================================================================================
def _DoExit(*args):
    '''
        We have to override the exit because calling sys.exit will only actually exit the main thread,
        and as we're in a Xml-rpc server, that won't work.
    '''
    
    try:
        import java.lang.System
        java.lang.System.exit(1)
    except ImportError:
        if len(args) == 1:
            os._exit(args[0])
        else:
            os._exit(0)
    
    
#=======================================================================================================================
# StartServer
#=======================================================================================================================
def StartServer(host, port, client_port):
    #replace exit (see comments on method)
    #note that this does not work in jython!!! (sys method can't be replaced).
    sys.exit = _DoExit
    
    from pydev_imports import SimpleXMLRPCServer
    interpreter = InterpreterInterface(host, client_port)
    server = SimpleXMLRPCServer((host, port), logRequests=False)

    
    if True:
        server.register_function(interpreter.addExec)
        server.register_function(interpreter.getCompletions)
        server.register_function(interpreter.getDescription)
        server.register_function(interpreter.close)
        server.serve_forever()
        
    else:
        #This is still not finished -- that's why the if True is there :)
        from pydev_imports import Queue
        queue_requests_received = Queue.Queue() #@UndefinedVariable
        queue_return_computed = Queue.Queue() #@UndefinedVariable
        
        def addExec(line):
            queue_requests_received.put(('addExec', line))
            return queue_return_computed.get(block=True)
        
        def getCompletions(text):
            queue_requests_received.put(('getCompletions', text))
            return queue_return_computed.get(block=True)
        
        def getDescription(text):
            queue_requests_received.put(('getDescription', text))
            return queue_return_computed.get(block=True)
        
        def close():
            queue_requests_received.put(('close', None))
            return queue_return_computed.get(block=True)
            
        server.register_function(addExec)
        server.register_function(getCompletions)
        server.register_function(getDescription)
        server.register_function(close)
        try:
            import PyQt4.QtGui #We can only start the PyQt4 loop if we actually have access to it.
        except ImportError:
            print('Unable to process gui events (PyQt4.QtGui not imported)')
            server.serve_forever()
        else:
            import threading
            class PydevHandleRequestsThread(threading.Thread):
                
                def run(self):
                    while 1:
                        #This is done on a thread (so, it may be blocking or not blocking, it doesn't matter)
                        #anyways, the request will be put on a queue and the return will be gotten from another
                        #one -- and those queues are shared with the main thread.
                        server.handle_request()
                    
            app = PyQt4.QtGui.QApplication([])
            def serve_forever():
                """Handle one request at a time until doomsday."""
                while 1:
                    try:
                        try:
                            func, param = queue_requests_received.get(block=True,timeout=1.0/20.0) #20 loops/second
                            attr = getattr(interpreter, func)
                            if param is not None:
                                queue_return_computed.put(attr(param))
                            else:
                                queue_return_computed.put(attr())
                        except Queue.Empty: #@UndefinedVariable
                            pass
                        
                        PyQt4.QtGui.qApp.processEvents()
                    except:
                        import traceback;traceback.print_exc()
                    
            PydevHandleRequestsThread().start()
            serve_forever()


    
#=======================================================================================================================
# main
#=======================================================================================================================
if __name__ == '__main__':
    port, client_port = sys.argv[1:3]
    import pydev_localhost
    StartServer(pydev_localhost.get_localhost(), int(port), int(client_port))
    
