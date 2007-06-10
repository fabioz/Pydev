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
