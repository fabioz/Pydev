'''
@author Fabio Zadrozny 
'''

def GenerateTip (theDoc, token):
    '''
    Put in the doc the code so that we get the locals.
    '''

    if token is None:
    
        theDoc+= \
'''

import inspect as __eraseThisinspect
import copy as __eraseThiscopy

__eraseThisf = __eraseThisinspect.currentframe()
__eraseThislocs = __eraseThiscopy.copy(__eraseThisf.f_locals)

for __eraseThisd in __eraseThislocs: 
    if __eraseThisd.startswith('__eraseThis') == False : __eraseThisTips.append([__eraseThisd,None])

l = locals()
for t in __eraseThisTips:
    t[1] = __eraseThisinspect.getdoc(l[t[0]])
'''
    
    else : #just complete for token.
        theDoc+= \
'''

import inspect 

for d in dir(%s):
    __eraseThisTips.append([d,inspect.getdoc(getattr(%s, d))])
''' % (token,token)
    
        
    
    import simpleinspect
    import compiler
    __eraseThis = compiler.compile(theDoc, 'temporary', 'exec')

    simpleinspect.__eraseThisTips = []
    simpleinspect.GenerateTip (__eraseThis)
    toReturn = simpleinspect.__eraseThisTips
    simpleinspect.__eraseThisTips = []
    return toReturn
    

    