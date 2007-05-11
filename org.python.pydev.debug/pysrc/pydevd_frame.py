from pydevd_comm import * #@UnusedWildImport
from pydevd_constants import STATE_RUN, STATE_SUSPEND 
import traceback

#jython has an 'exception' event that must be treated too (strangely it is called when doing a wild import)
ACCEPTED_EVENTS = {'call':1, 'line':1, 'return':1, 'exception':1}

class PyDBFrame:
    '''This makes the tracing for a given frame, so, the trace_dispatch
    is used initially when we enter into a new context ('call') and then
    is reused for the entire context
    '''
    
    def __init__(self, mainDebugger, filename, base, additionalInfo, t):
        self.additionalInfo = additionalInfo
        self.mainDebugger = mainDebugger
        self.t = t
        self.filename = filename
        self.base = base
        self.breakpoints = self.mainDebugger.breakpoints
    
    def setSuspend(self, *args, **kwargs):
        self.mainDebugger.setSuspend(*args, **kwargs)
        
    def doWaitSuspend(self, *args, **kwargs):
        self.mainDebugger.doWaitSuspend(*args, **kwargs)
    
    def trace_dispatch(self, frame, event, arg):
        if not ACCEPTED_EVENTS.has_key(event):
            return None
        
        additionalInfo = self.additionalInfo
        breakpoint = self.breakpoints.get(self.filename, None)
        
        #print 'dispatch', self.base, frame.f_lineno, event, frame.f_code.co_name, frame

        probably_skips = False
        # Let's check to see if we are in a function that has a breakpoint. If we don't have a breakpoint, 
        # we will return nothing for the next trace
        #also, after we hit a breakpoint and go to some other debugging state, we have to force the set trace anyway,
        #so, that's why the additional checks are there.
        if not breakpoint:
            #print 'skipping', self.base, frame.f_lineno, additionalInfo.pydev_state, additionalInfo.pydev_step_stop, additionalInfo.pydev_step_cmd
            probably_skips = True

        else:
            #checks the breakpoint to see if there is a context match in some function
            curr_func_name = frame.f_code.co_name
            
            #global context is set with an empty name
            if curr_func_name == '?':
                curr_func_name = ''
                
            for b, condition, func_name in breakpoint.values():
                #will match either global or some function
                if func_name == curr_func_name:
                    break
                
            else:
                #print 'skipping', curr_func_name, self.base, frame.f_lineno, additionalInfo.pydev_state, additionalInfo.pydev_step_stop, additionalInfo.pydev_step_cmd
                probably_skips = True

        if probably_skips:
            if additionalInfo.pydev_state == STATE_RUN and additionalInfo.pydev_step_stop is None and additionalInfo.pydev_step_cmd is None:
                return None
            
            if additionalInfo.pydev_step_cmd in (CMD_STEP_OVER, CMD_STEP_RETURN) and additionalInfo.pydev_step_stop != frame:
                return None
                
        #We just hit a breakpoint or we are already in step mode. Either way, let's trace this frame
        #print 'probably_skips', probably_skips, additionalInfo.pydev_step_cmd == CMD_STEP_OVER, additionalInfo.pydev_step_stop != frame
        #print 'NOT skipped', self.base, frame.f_lineno, additionalInfo.pydev_state, additionalInfo.pydev_step_stop, additionalInfo.pydev_step_cmd
        frame.f_trace = self.trace_dispatch

        
        t = self.t #thread
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
                            return self.trace_dispatch
                            
                    except:
                        print >> sys.stderr, 'Error while evaluating expression'
                        traceback.print_exc()
                        return self.trace_dispatch
                
                self.setSuspend(t, CMD_SET_BREAK)
                
            # if thread has a suspend flag, we suspend with a busy wait
            if additionalInfo.pydev_state == STATE_SUSPEND:
                self.doWaitSuspend(t, frame, event, arg)
                return self.trace_dispatch
            
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
        if not self.mainDebugger.quitting:
            retVal = self.trace_dispatch

        return retVal
