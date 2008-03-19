from code import InteractiveConsole
from StringIO import StringIO
import time
import threading
import sys

try:
    __setFalse = False
except:
    import __builtin__
    __builtin__.True = 1
    __builtin__.False = 0

#=======================================================================================================================
# NoReadWrite
#=======================================================================================================================
class NoReadWrite:
    
    def read(self, *args, **kwargs):
        pass
        
    def write(self, *args, **kwargs):
        pass

#=======================================================================================================================
# StdIn
#=======================================================================================================================
class StdIn:
    '''
        Object to be added to stdin (to emulate it as non-blocking while the next line arrives)
    '''
    
    def __init__(self, interpreter, push_thread):
        self.interpreter = interpreter
        self.readline_return = None
        self.push_thread = push_thread
    
    
    def readline(self, *args, **kwargs): #@UnusedVariable
        #a better option (instead of a thread) would be to callback into the client (so, the client should also 
        #have a server so that we can talk 2 ways here).
        self.push_thread.need_input = True
        while self.readline_return is None:
            time.sleep(0.01)
        return self.readline_return
        
    #in the interactive interpreter, a read and a readline are the same.
    read = readline
        
#=======================================================================================================================
# PushThread
#=======================================================================================================================
class PushThread(threading.Thread):
    '''
        We need to add things to the interpreter because the user may end up adding a raw_input()
        and we don't want to block the console if that happens.
        
        Another option would be having the client as another Xml-rpc server, so, instead of making
        this thread it would just callback into the client for an answer.
    '''
    
    def __init__(self, interpreter, line):
        threading.Thread.__init__(self)
        self.interpreter = interpreter
        self.line = line
        self.need_input = False
        
    def run(self):
        try:
            self.more = self.interpreter.push(self.line)
        finally:
            del self.interpreter
            del self.line

#=======================================================================================================================
# InterpreterInterface
#=======================================================================================================================
class InterpreterInterface:
    '''
        The methods in this class should be registered in the xml-rpc server.
    '''
    
    def __init__(self):
        self.namespace = {}
        self.interpreter = InteractiveConsole(self.namespace)
        self.std_in_that_needs_input = None
        self.push_thread_that_needs_input = None
        
    def addExec(self, line):
        #f_opened = open('c:/temp/a.txt', 'a')
        #f_opened.write(line+'\n')
        
        original_out = sys.stdout
        original_err = sys.stderr
        original_in = sys.stdin
        
        
        out = sys.stdout = StringIO()
        err = sys.stderr = StringIO()
        
        if self.std_in_that_needs_input is not None:
            std_in = sys.stdin = self.std_in_that_needs_input
            push_thread = self.push_thread_that_needs_input
            push_thread.need_input = False
        else:
            push_thread = PushThread(self.interpreter, line)
            std_in = sys.stdin = StdIn(self, push_thread)
        
        try:
            if self.std_in_that_needs_input is not None:
                self.std_in_that_needs_input.readline_return = line
            else:
                push_thread.start()
                
            while not hasattr(push_thread, 'more') and not push_thread.need_input:
                time.sleep(0.01)
            
            if push_thread.need_input:
                self.push_thread_that_needs_input = push_thread
                self.std_in_that_needs_input = std_in
                more = False
                need_input = True
            else:
                self.push_thread_that_needs_input = None
                self.std_in_that_needs_input = None
                more = push_thread.more
                need_input = False
                
        finally:
        
            out = out.getvalue()
            err = err.getvalue()
            
            sys.stdout = original_out
            sys.stderr = original_err
            sys.stdin = original_in
            
        #f_opened.write('returning\n')
        #f_opened.write(str(out))
        #f_opened.write('\n')
        #f_opened.write(str(err))
        #f_opened.write('\n')
        #f_opened.write(str(more))
        #f_opened.write('\n')
        #f_opened.write(str(need_input))
        #f_opened.write('\nend returning\n')
        return out, err, more, need_input
            
    def getCompletions(self, text):
        from _completer import Completer
        completer = Completer(self.namespace, None)
        return completer.complete(text)
        
    
    def getDescription(self, text):
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
                if not sys.platform.startswith("java"):
                    #Python
                    import inspect
                    doc = inspect.getdoc(obj) 
                    if doc is not None:
                        return doc
                        
                else:
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
        
    def close(self):
        sys.exit()
        
    
#=======================================================================================================================
# main
#=======================================================================================================================
if __name__ == '__main__':
    port = sys.argv[1]
    port = int(port)
    
    try:
        from SimpleXMLRPCServer import SimpleXMLRPCServer
    except ImportError:
        from _pydev_SimpleXMLRPCServer import SimpleXMLRPCServer
        
    interpreter = InterpreterInterface()
    try:
        server = SimpleXMLRPCServer(('localhost', port)) #allow_none cannot be passed to jython
    except TypeError:
        server = SimpleXMLRPCServer(('localhost', port))
        
    server.register_function(interpreter.addExec)
    server.register_function(interpreter.getCompletions)
    server.register_function(interpreter.getDescription)
    server.register_function(interpreter.close)
    
    #we don't want output to anywhere (only to the server -- with sockets!)
    sys.stderr = NoReadWrite()
    sys.stdout = NoReadWrite()
    
    server.serve_forever()

    