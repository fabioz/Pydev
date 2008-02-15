from pydevd_comm import * #@UnusedWildImport
from pydevd_constants import * #@UnusedWildImport
import traceback #@Reimport

#jython has an 'exception' event that must be treated too (strangely it is called when doing a wild import)
ACCEPTED_EVENTS = {'call':1, 'line':1, 'return':1, 'exception':1}

#=======================================================================================================================
# PyDBFrame
#=======================================================================================================================
class PyDBFrame:
    '''This makes the tracing for a given frame, so, the trace_dispatch
    is used initially when we enter into a new context ('call') and then
    is reused for the entire context.
    '''
    
    def setSuspend(self, *args, **kwargs):
        self.mainDebugger.setSuspend(*args, **kwargs)
        
    def doWaitSuspend(self, *args, **kwargs):
        self.mainDebugger.doWaitSuspend(*args, **kwargs)
    

    #the ignored args is so that we can match different interfaces
    def __init__(self, mainDebugger, filename, base, additionalInfo, t, actual_frame): 
        self.mainDebugger= mainDebugger

        #we can keep the function name here (as it won't change later)
        #checks the breakpoint to see if there is a context match in some function
        curr_func_name = actual_frame.f_code.co_name
        
        #global context is set with an empty name
        if curr_func_name == '?' or curr_func_name == '<module>':
            curr_func_name = ''
            
        #just making sure we don't access it later (because we'd create a cyclic reference if
        #we kept it alive by referencing it in the trace_dispatch function)
        del actual_frame 
        
        #just leave it in the namespace for fast access
        breakpoints = mainDebugger.breakpoints
            
        #it's 'inlined' for optimizing variables access (instead of attribute if it was a method)
        def trace_dispatch(frame, event, arg):
            if not ACCEPTED_EVENTS.has_key(event):
                return None
            
            breakpoint = breakpoints.get(filename)
            
            #print 'frame: trace_dispatch', base, frame.f_lineno, event, frame.f_code.co_name
    
            probably_skip_context = False
            # Let's check to see if we are in a function that has a breakpoint. If we don't have a breakpoint, 
            # we will return nothing for the next trace
            #also, after we hit a breakpoint and go to some other debugging state, we have to force the set trace anyway,
            #so, that's why the additional checks are there.
            if not breakpoint:
                probably_skip_context = True
    
            else:
                    
                for b, condition, func_name in breakpoint.itervalues():
                    #will match either global or some function
                    if func_name == 'None' or func_name == curr_func_name:
                        break
                    
                else: # if we had some break, it won't get here (so, that's a context that we probably want to skip -- see conditions below)
                    probably_skip_context = True
    
            if probably_skip_context:
                if additionalInfo.pydev_state == STATE_RUN and additionalInfo.pydev_step_stop is None and additionalInfo.pydev_step_cmd is None:
                    #print 'skipping', base, frame.f_lineno, additionalInfo.pydev_state, additionalInfo.pydev_step_stop, additionalInfo.pydev_step_cmd
                    return None
                
                if additionalInfo.pydev_step_cmd in (CMD_STEP_OVER, CMD_STEP_RETURN) and additionalInfo.pydev_step_stop != frame:
                    #print 'skipping', base, frame.f_lineno, additionalInfo.pydev_state, additionalInfo.pydev_step_stop, additionalInfo.pydev_step_cmd
                    return None
                    
            #We just hit a breakpoint or we are already in step mode. Either way, let's trace this frame
            #print 'NOT skipped', base, frame.f_lineno, additionalInfo.pydev_state, additionalInfo.pydev_step_stop, additionalInfo.pydev_step_cmd
    
            
            try:
                line = int(frame.f_lineno)
                if additionalInfo.pydev_state != STATE_SUSPEND and breakpoint is not None and breakpoint.has_key(line):
                    #ok, hit breakpoint, now, we have to discover if it is a conditional breakpoint
                    # lets do the conditional stuff here
                    condition = breakpoint[line][1]
    
                    if condition is not None:
                        try:
                            val = eval(condition, frame.f_globals, frame.f_locals)
                            if not val:
                                return trace_dispatch
                                
                        except:
                            print >> sys.stderr, 'Error while evaluating expression'
                            traceback.print_exc()
                            return trace_dispatch
                    
                    self.setSuspend(t, CMD_SET_BREAK)
                    
                # if thread has a suspend flag, we suspend with a busy wait
                if additionalInfo.pydev_state == STATE_SUSPEND:
                    self.doWaitSuspend(t, frame, event, arg)
                    return trace_dispatch
                
            except:
                traceback.print_exc()
                raise
    
            #step handling. We stop when we hit the right frame
            try:
                if additionalInfo.pydev_step_cmd == CMD_STEP_INTO and event in ('line', 'return'):
                    self.setSuspend(t, CMD_STEP_INTO)
                    self.doWaitSuspend(t, frame, event, arg)      
                    
                
                elif additionalInfo.pydev_step_cmd in (CMD_STEP_OVER, CMD_STEP_RETURN) and event in ('line', 'return'): 
                    if additionalInfo.pydev_step_stop == frame:
                        self.setSuspend(t, additionalInfo.pydev_step_cmd)
                        self.doWaitSuspend(t, frame, event, arg)
                    
                        
            except:
                traceback.print_exc()
                additionalInfo.pydev_step_cmd = None
            
            #if we are quitting, let's stop the tracing
            retVal = None
            if not mainDebugger.quitting:
                retVal = trace_dispatch
    
            return retVal
            
        self.trace_dispatch = trace_dispatch
