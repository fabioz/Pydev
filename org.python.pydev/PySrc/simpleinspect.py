'''

@author Fabio Zadrozny 
'''

def GenerateTip (__eraseThisV):
    d = dict()
    d['__eraseThisTips'] = []
    exec __eraseThisV in d
    return d['__eraseThisTips']
