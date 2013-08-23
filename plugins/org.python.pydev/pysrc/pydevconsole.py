from __future__ import nested_scopes  # Jython 2.1 support
try:
    from code import InteractiveConsole
except ImportError:
    from pydevconsole_code_for_ironpython import InteractiveConsole 

import os
import sys

try:
    False
    True
except NameError:  # version < 2.3 -- didn't have the True/False builtins
    import __builtin__
    setattr(__builtin__, 'True', 1)  # Python 3.0 does not accept __builtin__.True = 1 in its syntax
    setattr(__builtin__, 'False', 0)

import threading
import atexit
from pydev_imports import SimpleXMLRPCServer, Queue
from pydev_console_utils import BaseStdIn, StdIn, BaseInterpreterInterface

        
try:
    class ExecState:
        FIRST_CALL = True
        PYDEV_CONSOLE_RUN_IN_UI = False  # Defines if we should run commands in the UI thread.
        
    from org.python.pydev.core.uiutils import RunInUiThread  # @UnresolvedImport
    from java.lang import Runnable  # @UnresolvedImport
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
    # If things are not there, define a way in which there's no 'real' sync, only the default execution.
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
        execfile  # Not in Py3k
    except NameError:
        from pydev_imports import execfile
        import builtins  # @UnresolvedImport -- only Py3K
        builtins.execfile = execfile
        
except:
    pass


#=======================================================================================================================
# InterpreterInterface
#=======================================================================================================================
class InterpreterInterface(BaseInterpreterInterface):
    '''
        The methods in this class should be registered in the xml-rpc server.
    '''
    
    def __init__(self, host, client_port):
        self.client_port = client_port
        self.host = host
        try:
            import pydevd  # @UnresolvedImport
            if pydevd.GetGlobalDebugger() is None:
                raise RuntimeError()  # Work as if the debugger does not exist as it's not connected.
        except:
            self.namespace = globals()
        else:
            # Adapted from the code in pydevd
            # patch provided by: Scott Schlesier - when script is run, it does not 
            # pretend pydevconsole is not the main module, and
            # convince the file to be debugged that it was loaded as main
            sys.modules['pydevconsole'] = sys.modules['__main__']
            sys.modules['pydevconsole'].__name__ = 'pydevconsole'            
            
            from imp import new_module
            m = new_module('__main__')
            sys.modules['__main__'] = m
            ns = m.__dict__
            try:
                ns['__builtins__'] = __builtins__
            except NameError:
                pass  # Not there on Jython...
            self.namespace = ns
        self.interpreter = InteractiveConsole(self.namespace)
        self._input_error_printed = False

        
    def doAddExec(self, line):
        command = Command(self.interpreter, line)
        Sync(command)
        return command.more


    def getNamespace(self):
        return self.namespace
    
        
    def getCompletions(self, text, act_tok):
        try:
            from _pydev_completer import Completer
            completer = Completer(self.namespace, None)
            return completer.complete(act_tok)
        except:
            import traceback;traceback.print_exc()
            return []
        
    
    def close(self):
        sys.exit(0)
        
    
try:
    from pydev_ipython_console import InterpreterInterface
except:
    sys.stderr.write('PyDev console: using default backend (IPython not available).\n')
    pass  # IPython not available, proceed as usual.
    
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
# ThreadedXMLRPCServer
#=======================================================================================================================
class ThreadedXMLRPCServer(SimpleXMLRPCServer):
    
    def __init__(self, addr, main_loop, **kwargs):
        SimpleXMLRPCServer.__init__(self, addr, **kwargs)
        self.main_loop = main_loop
        self.resp_queue = Queue.Queue()


    def register_function(self, fn, name):
        def proxy_fn(*args, **kwargs):
            def main_loop_cb():
                try:
                    try:
                        sys.exc_clear()
                    except:
                        pass  # Not there in Jython 2.1
                    self.resp_queue.put(fn(*args, **kwargs))
                except:
                    import traceback;traceback.print_exc()
                    self.resp_queue.put(None)
            self.main_loop.call_in_main_thread(main_loop_cb)
            return self.resp_queue.get(block=True)
        
        SimpleXMLRPCServer.register_function(self, proxy_fn, name)


#=======================================================================================================================
# MainLoop
#=======================================================================================================================
class MainLoop:
    
    ui_name = 'Not defined'
    
    def run(self):
        """Run the main loop of the GUI library.  This method should not
        return.
        """
        raise NotImplementedError

    def call_in_main_thread(self, cb):
        """Given a callable `cb`, pass it to the main loop of the GUI library
        so that it will eventually be called in the main thread.  It's OK but
        not compulsory for this method to block until the main thread has
        finished processing `cb`; as such, this method must not be called from
        the main thread.
        """
        raise NotImplementedError



import pydev_guisupport


#=======================================================================================================================
# QtMainLoop
#=======================================================================================================================
class QtMainLoop(MainLoop):
    
    ui_name = 'Qt4'
    
    def __init__(self):
        # On init we must check dependencies: if it raises no error, it's used.
        try:
            from PyQt4 import QtCore, QtGui
        except:
            from PySide import QtCore, QtGui
        
        class CallbackEvent(QtCore.QEvent):
        
            def __init__(self, cb=None):
                QtCore.QEvent.__init__(self, QtCore.QEvent.User)
                self.cb = cb
                
        self.CallbackEvent = CallbackEvent
        
        class Receiver(QtCore.QObject):
            
            def event(self, ev):
                if type(ev) is CallbackEvent:
                    ev.cb()
                    return True
                return False
        self._receiver = Receiver()
        
        self.app = pydev_guisupport.get_app_qt4()
        

    def run(self):
        while True:
            pydev_guisupport.start_event_loop_qt4(self.app)

    def call_in_main_thread(self, cb):
        # Send event to be handled on the event-loop.
        self.app.postEvent(self._receiver, self.CallbackEvent(cb))

#=======================================================================================================================
# WxMainLoop
#=======================================================================================================================
class WxMainLoop(MainLoop):
    
    ui_name = 'Wx'
    
    def __init__(self):
        # On init we must check dependencies: if it raises no error, it's used.
        import wx
        # If I pass redirect = False, the console does not work (and I don't know why).
        self.app = pydev_guisupport.get_app_wx(redirect=True)

    def run(self):
        while True:
            pydev_guisupport.start_event_loop_wx(self.app)

    def call_in_main_thread(self, cb):
        import wx
        wx.CallAfter(cb)


#=======================================================================================================================
# GtkMainLoop
#=======================================================================================================================
class GtkMainLoop(MainLoop):
    
    ui_name = 'Gtk'
    
    def __init__(self):
        # On init we must check dependencies: if it raises no error, it's used.
        import gtk
        import gobject
    
    def run(self):
        import gtk
        gtk.main()

    def call_in_main_thread(self, cb):
        import gobject
        gobject.idle_add(cb)


#=======================================================================================================================
# NoGuiMainLoop
#=======================================================================================================================
class NoGuiMainLoop(MainLoop):
    
    ui_name = 'no_gui'
    
    def __init__(self):
        self.queue = Queue.Queue()

    def run(self):
        while True:
            cb = self.queue.get(block=True)
            try:
                cb()
            except:
                import traceback;traceback.print_exc()

    def call_in_main_thread(self, cb):
        self.queue.put(cb)


#=======================================================================================================================
# StartServer
#=======================================================================================================================
def StartServer(host, port, client_port):
    # replace exit (see comments on method)
    # note that this does not work in jython!!! (sys method can't be replaced).
    sys.exit = _DoExit
    
    for loop_cls in (
        # WxMainLoop, --Removed because it doesn't seem to work with redirect=False 
        QtMainLoop,
        # GtkMainLoop
        ):
        try:
            main_loop = loop_cls()
            sys.stderr.write('Info: UI event loop integration active: %s\n' % (loop_cls.ui_name))
            break
        except:
            try:
                sys.exc_clear()
            except:
                pass  # Not there in Jython 2.1
    else:
        main_loop = NoGuiMainLoop()
        sys.stderr.write('Warning: No UI framework found to integrate event loop (supported: Qt, Gtk)\n')

    try:
        interpreter = InterpreterInterface(host, client_port)
        server = ThreadedXMLRPCServer((host, port), main_loop, logRequests=False)
    except:
        sys.stderr.write('Error starting server with host: %s, port: %s, client_port: %s\n' % (host, port, client_port))
        raise

    # Functions for basic protocol
    server.register_function(interpreter.addExec, 'addExec')
    server.register_function(interpreter.getCompletions, 'getCompletions')
    server.register_function(interpreter.getDescription, 'getDescription')
    server.register_function(interpreter.close, 'close')

    # Functions so that the console can work as a debugger (i.e.: variables view, expressions...)
    server.register_function(interpreter.connectToDebugger, 'connectToDebugger')
    server.register_function(interpreter.postCommand, 'postCommand')
    server.register_function(interpreter.hello, 'hello')

    try:
        atexit.register(server.shutdown)
    except:
        pass  # server.shutdown not there for jython 2.1
    server_thread = threading.Thread(target=server.serve_forever)
    server_thread.daemon = True
    server_thread.start()
    main_loop.run()

    
#=======================================================================================================================
# main
#=======================================================================================================================
if __name__ == '__main__':
    sys.stdin = BaseStdIn()
    port, client_port = sys.argv[1:3]
    import pydev_localhost
    StartServer(pydev_localhost.get_localhost(), int(port), int(client_port))
    
