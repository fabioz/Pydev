from pydevd_comm import * #@UnusedWildImport
from pydevd_constants import STATE_RUN, STATE_SUSPEND 
import traceback

#jython has an 'exception' event that must be treated too (strangely it is called when doing a wild import)
ACCEPTED_EVENTS = {'call':1, 'line':1, 'return':1, 'exception':1}

class PyDBFrame:
    '''This makes the tracing for a given frame, so, the trace_dispatch
    is used initially when we enter into a new context ('call') and then
    reused for the entire context
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
        if event not in ACCEPTED_EVENTS:
            return None
        
        additionalInfo = self.additionalInfo

        # Let's check to see if we are in a line that has a breakpoint. If we don't have a breakpoint, 
        # we will return nothing for the next trace
        #also, after we hit a breakpoint and go to some other debugging state, we have to force the set trace anyway,
        #so, that's why the additional checks are there.
        breakpoint = self.breakpoints.get(self.filename, None)
        if breakpoint is None and additionalInfo.pydev_state == STATE_RUN and \
           additionalInfo.pydev_step_stop is None and additionalInfo.pydev_step_cmd is None:
            #print 'skipping', self.base, frame.f_lineno, additionalInfo.pydev_state, additionalInfo.pydev_step_stop, additionalInfo.pydev_step_cmd
            return None

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
                
            
            elif additionalInfo.pydev_step_cmd == CMD_STEP_OVER and event in ('line', 'return'): 
                if additionalInfo.pydev_step_stop == frame:
                    self.setSuspend(t, CMD_STEP_OVER)
                    self.doWaitSuspend(t, frame, event, arg)
                
                
            elif additionalInfo.pydev_step_cmd == CMD_STEP_RETURN:
                if event == 'return':
                    additionalInfo.pydev_return_call_count += 1
                elif event == 'call':
                    additionalInfo.pydev_return_call_count -= 1
                    
                if additionalInfo.pydev_stop_on_return_count_1 and additionalInfo.pydev_return_call_count == 1 \
                    and additionalInfo.pydev_step_stop == frame.f_back and event in ('line', 'return'):
                    
                    additionalInfo.pydev_return_call_count == 0
                    additionalInfo.pydev_stop_on_return_count_1 = False
                    self.setSuspend(t, CMD_STEP_RETURN)
                    self.doWaitSuspend(t, frame, event, arg)
                    
                if additionalInfo.pydev_step_stop == frame and event in ('line', 'return'):
                    self.setSuspend(t, CMD_STEP_RETURN)
                    self.doWaitSuspend(t, frame, event, arg)
        except:
            traceback.print_exc()
            additionalInfo.pydev_step_cmd = None
        
        #if we are quitting, let's stop the tracing
        retVal = None
        if not self.mainDebugger.quitting:
            retVal = self.trace_dispatch

        return retVal
