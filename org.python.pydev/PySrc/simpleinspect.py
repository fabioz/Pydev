'''

@author Fabio Zadrozny 
'''

def GenerateTip (__eraseThisV):
    d = dict()
    d['__eraseThisTips'] = []
    dGlobals = dict() #we don't want the globals 
    exec __eraseThisV in dGlobals, d
    return d['__eraseThisTips']
