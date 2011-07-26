from IPython.frontend.prefilterfrontend import PrefilterFrontEnd
import sys

from pydev_console_utils import Null, StdIn, BaseInterpreterInterface

original_stdout = sys.stdout
original_stderr = sys.stderr


#=======================================================================================================================
# PyDevFrontEnd
#=======================================================================================================================
class PyDevFrontEnd(PrefilterFrontEnd):


    def __init__(self, *args, **kwargs):        
        PrefilterFrontEnd.__init__(self, *args, **kwargs)
        #Disable the output trap: we want all that happens to go to the output directly
        self.shell.output_trap = Null()
        self._curr_exec_lines = []
        self._continuation_prompt = ''
        
        
    def capture_output(self):
        pass
    
    def release_output(self):
        pass
    
    def continuation_prompt(self):
        return self._continuation_prompt
    
    def write(self, txt, refresh=True):
        original_stdout.write(txt)
        

    def new_prompt(self, prompt):
        self.input_buffer = ''
        #The java side takes care of this part.
        #self.write(prompt)
        
        
    def show_traceback(self):
        import traceback;traceback.print_exc()
        
    def write_out(self, txt, *args, **kwargs):
        original_stdout.write(txt)
    
    
    def write_err(self, txt, *args, **kwargs):
        original_stderr.write(txt)
    
    def addExec(self, line):
        if self._curr_exec_lines:
            if not line:
                self._curr_exec_lines.append(line)
                
                #Would be the line below, but we've set the continuation_prompt to ''.
                #buf = self.continuation_prompt() + ('\n' + self.continuation_prompt()).join(self._curr_exec_lines)
                buf = '\n'.join(self._curr_exec_lines)
                
                self.input_buffer = buf + '\n'
                if self._on_enter():
                    del self._curr_exec_lines[:]
                    return False #execute complete (no more)
                
                return True #needs more
            else:
                self._curr_exec_lines.append(line)
                return True #needs more
            
        else:
            
            self.input_buffer = line
            if not self._on_enter():
                #Did not execute
                self._curr_exec_lines.append(line)
                return True #needs more
            
            return False #execute complete (no more)
    

TYPE_ATTR = '3'

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
        self.interpreter = PyDevFrontEnd()
        self._input_error_printed = False


    def doAddExec(self, line):
        return bool(self.interpreter.addExec(line))

        
    def getCompletions(self, text):
        try:
#            _line, completions = self.interpreter.complete(text)
#            ret = []
#            append = ret.append
#            for completion in completions:
#                append((completion, '', '', TYPE_ATTR))
#            return ret
            from _completer import Completer
            completer = Completer(self.interpreter.shell.user_ns, None)
            return completer.complete(text)
        except:
            import traceback;traceback.print_exc()
            return []
        
    
    def getDescription(self, text):
        return 'Description of: '+str(text)
    
    def close(self):
        sys.exit(0)

