from pydev_imports import xmlrpclib
import sys

#=======================================================================================================================
# Null
#=======================================================================================================================
class Null:
    """
    Gotten from: http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/68205
    """

    def __init__(self, *args, **kwargs):
        return None

    def __call__(self, *args, **kwargs):
        return self

    def __getattr__(self, mname):
        return self

    def __setattr__(self, name, value):
        return self

    def __delattr__(self, name):
        return self

    def __repr__(self):
        return "<Null>"

    def __str__(self):
        return "Null"
    
    def __len__(self):
        return 0
    
    def __getitem__(self):
        return self
    
    def __setitem__(self, *args, **kwargs):
        pass
    
    def write(self, *args, **kwargs):
        pass
    
    def __nonzero__(self):
        return 0
    
    

#=======================================================================================================================
# BaseStdIn
#=======================================================================================================================
class BaseStdIn:
    
    def __init__(self, *args, **kwargs):
        try:
            self.encoding = sys.stdin.encoding
        except:
            #Not sure if it's available in all Python versions...
            pass
    
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
    
#=======================================================================================================================
# StdIn
#=======================================================================================================================
class StdIn(BaseStdIn):
    '''
        Object to be added to stdin (to emulate it as non-blocking while the next line arrives)
    '''
    
    def __init__(self, interpreter, host, client_port):
        BaseStdIn.__init__(self)
        self.interpreter = interpreter
        self.client_port = client_port
        self.host = host
    
    def readline(self, *args, **kwargs):
        #Ok, callback into the client to get the new input
        server = xmlrpclib.Server('http://%s:%s' % (self.host, self.client_port))
        requested_input = server.RequestInput()
        if not requested_input:
            return '\n' #Yes, a readline must return something (otherwise we can get an EOFError on the input() call).
        return requested_input
    
    
    

#=======================================================================================================================
# BaseInterpreterInterface
#=======================================================================================================================
class BaseInterpreterInterface:
    
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
                    more = self.doAddExec(line)
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
    
    
    def doAddExec(self, line):
        '''
        Subclasses should override.
        
        @return: more (True if more input is needed to complete the statement and False if the statement is complete).
        '''
        raise NotImplementedError()
    
    
    def getNamespace(self):
        '''
        Subclasses should override.
        
        @return: dict with namespace.
        '''
        raise NotImplementedError()
    
        
    
    def getDescription(self, text):
        try:
            obj = None
            if '.' not in text:
                try:
                    obj = self.getNamespace()[text]
                except KeyError:
                    return ''
                    
            else:
                try:
                    splitted = text.split('.')
                    obj = self.getNamespace()[splitted[0]]
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