import sys
debugToggle = 0
def debug(name, value=None):
    if debugToggle == 0: return
    if value == None:
        print >> sys.stderr, "DBG:",name
    else:
        print >> sys.stderr, "DBG:%s = %s" % (name, value)
        
        
