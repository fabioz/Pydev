'''
This module holds the constants used for specifying the states of the debugger.
'''

STATE_RUN     = 1
STATE_SUSPEND = 2

try:
    __setFalse = False
except:
    False = 0
    True = 1

DEBUG_TRACE_LEVEL = -1
DEBUG_TRACE_BREAKPOINTS = -1

DEBUG_RECORD_SOCKET_READS = False

#Optimize with psyco? This gave a 50% speedup in the debugger in tests 
USE_PSYCO_OPTIMIZATION = True


#===============================================================================
# Null
#===============================================================================
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
    
if __name__ == '__main__':
    if Null():
        print 'here'