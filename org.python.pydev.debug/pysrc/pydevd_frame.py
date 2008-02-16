from pydevd_comm import * #@UnusedWildImport
from pydevd_constants import * #@UnusedWildImport
import traceback #@Reimport

#=======================================================================================================================
# PyDBFrame
#=======================================================================================================================
class PyDBFrame:
    '''This makes the tracing for a given frame, so, the trace_dispatch
    is used initially when we enter into a new context ('call') and then
    is reused for the entire context.
    '''
    
    def __init__(self, *args):
        #args = mainDebugger, filename, base, additionalInfo, t, frame
        #yeap, much faster than putting in self and the getting it from self later on
        self._args = args[:-1]
    
    def setSuspend(self, *args, **kwargs):
        self._args[0].setSuspend(*args, **kwargs)
        
    def doWaitSuspend(self, *args, **kwargs):
        self._args[0].doWaitSuspend(*args, **kwargs)
    
    def trace_dispatch(self, frame, event, arg):
        #jython has an 'exception' event that must be treated too (strangely it is called when doing a wild import)
        if event not in ('line', 'call', 'return', 'exception'):
            return None
            
        mainDebugger, filename, additionalInfo, thread = self._args
        
        breakpoint = mainDebugger.breakpoints.get(filename)
        
        #print 'frame: trace_dispatch', self.base, frame.f_lineno, event, frame.f_code.co_name

        can_skip = additionalInfo.pydev_state == STATE_RUN and additionalInfo.pydev_step_stop is None \
            and additionalInfo.pydev_step_cmd is None
            
        # Let's check to see if we are in a function that has a breakpoint. If we don't have a breakpoint, 
        # we will return nothing for the next trace
        #also, after we hit a breakpoint and go to some other debugging state, we have to force the set trace anyway,
        #so, that's why the additional checks are there.
        if not breakpoint:
            if can_skip:
                return None

        else:
            #checks the breakpoint to see if there is a context match in some function
            curr_func_name = frame.f_code.co_name
            
            #global context is set with an empty name
            if curr_func_name in ('?', '<module>'):
                curr_func_name = ''
                
            for _b, condition, func_name in breakpoint.itervalues():
                #will match either global or some function
                if func_name in ('None', curr_func_name):
                    break
                
            else: # if we had some break, it won't get here (so, that's a context that we want to skip)
                if can_skip:
                    #print 'skipping', frame.f_lineno, additionalInfo.pydev_state, additionalInfo.pydev_step_stop, additionalInfo.pydev_step_cmd
                    return None
                
        #We may have hit a breakpoint or we are already in step mode. Either way, let's check what we should do in this frame
        #print 'NOT skipped', base, frame.f_lineno, additionalInfo.pydev_state, additionalInfo.pydev_step_stop, additionalInfo.pydev_step_cmd

        
        try:
            line = frame.f_lineno
            
            #return is not taken into account for breakpoint hit because we'd have a double-hit in this case
            #(one for the line and the other for the return).
            if event != 'return' and additionalInfo.pydev_state != STATE_SUSPEND and breakpoint is not None \
                and breakpoint.has_key(line):
                
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
                
                self.setSuspend(thread, CMD_SET_BREAK)
                
            # if thread has a suspend flag, we suspend with a busy wait
            if additionalInfo.pydev_state == STATE_SUSPEND:
                self.doWaitSuspend(thread, frame, event, arg)
                return self.trace_dispatch
            
        except:
            traceback.print_exc()
            raise
        
        #step handling. We stop when we hit the right frame
        try:
            if additionalInfo.pydev_step_cmd == CMD_STEP_INTO and event in ('line', 'return'):
                self.setSuspend(thread, CMD_STEP_INTO)
                self.doWaitSuspend(thread, frame, event, arg)      
                
            elif additionalInfo.pydev_step_cmd in (CMD_STEP_OVER, CMD_STEP_RETURN):
                self.setSuspend(thread, additionalInfo.pydev_step_cmd)
                
                if event == 'return':
                    #if we're in a return, we want it to appear to the user in the previous frame!
                    self.doWaitSuspend(thread, frame.f_back, event, arg)
                    
                elif event == 'line' and additionalInfo.pydev_step_stop == frame:
                    self.doWaitSuspend(thread, frame, event, arg)
                
                    
        except:
            traceback.print_exc()
            additionalInfo.pydev_step_cmd = None
        
        #if we are quitting, let's stop the tracing
        retVal = None
        if not mainDebugger.quitting:
            retVal = self.trace_dispatch

        return retVal
    
    if USE_PSYCO_OPTIMIZATION:
        try:
            import psyco
        except ImportError:
            pass #ok, psyco not available
        else:
            trace_dispatch = psyco.proxy(trace_dispatch)
