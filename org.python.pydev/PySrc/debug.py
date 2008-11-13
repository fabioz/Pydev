import sys
debugToggle = 0
def debug(name, value=None):
    if debugToggle == 0: return
    if value == None:
        sys.stderr.write("DBG: %s\n" % (name,))
    else:
        sys.stderr.write("DBG:%s = %s\n" % (name, value))
        
        
