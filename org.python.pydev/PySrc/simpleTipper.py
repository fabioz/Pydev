'''
@author Fabio Zadrozny 
'''
import compiler

def GenerateTip (theDoc, token, checkForSelf):
    '''
    Put in the doc the code so that we get the locals.
    '''

    originalDoc = theDoc
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
    try:
        __eraseThis = compiler.compile(theDoc, 'temporary_file_completion.py', 'exec')
        
        simpleinspect.__eraseThisTips = []
        simpleinspect.GenerateTip (__eraseThis, token)
        toReturn = simpleinspect.__eraseThisTips
        simpleinspect.__eraseThisTips = []
        
        if checkForSelf:
            toReturn += GetSelfVariables(originalDoc, token)
        
        return toReturn
    except :
        import sys
        s = str(sys.exc_info()[1])
        print s
        return [('ERROR_COMPLETING',s)]
    
class Visitor(compiler.visitor.ASTVisitor):

    def __init__(self, classToVisit):
        self.classToVisit = classToVisit
        self.selfAttribs = []
        
    def visitClass(self, node):
#        print node.name
        if node.name == self.classToVisit:
            for n in node.getChildNodes():
                self.visit(n)
        
    def visitAssign(self, node):
        
        for n in node.getChildNodes():
            if isinstance(n,compiler.ast.AssAttr):
                if n.expr.name == 'self':
                    self.selfAttribs.append((n.attrname,'Instance attribute'))
            
    
def GetSelfVariables(theDoc, classToVisit):
    ast = compiler.parse(theDoc)
    
    visitor = Visitor(classToVisit) 
    compiler.walk(ast, visitor)
    
    return visitor.selfAttribs
    
    
    
    
    
    
    
    
    
    

    
    
    
    
    
    
    
    
    
    