'''
@author Fabio Zadrozny 
'''
import compiler


__eraseThisCurrDirModule = None

def CompleteFromDir(dir):
    '''
    This is necessary so that we get the imports from the same dir where the file
    we are completing is located.
    '''
    import sys
    global __eraseThisCurrDirModule
    if __eraseThisCurrDirModule is not None:
        del sys.path[__eraseThisCurrDirModule]

    sys.path.insert(0, dir)


def ReloadModules():
    '''
    Reload all the modules in sys.modules
    '''
    import sys
    for m,n in sys.modules.items():
        try:
            reload(n)
        except: #some errors may arise because some modules may not be reloaded...
            pass
    
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
''' % (token.replace(' ','.'),token.replace(' ','.'))
    
    
    
    import simpleinspect
    __eraseThisMsg = ''

    __eraseThisMsg += 'Compiling \n%s\n'%theDoc
    __eraseThis = compiler.compile(theDoc, 'temporary_file_completion.py', 'exec')
    __eraseThisMsg += 'Compiled'
    
    toReturn = simpleinspect.GenerateTip (__eraseThis)
    
    __eraseThisMsg += 'Getting self variables \n%s\n' % originalDoc
    if checkForSelf:
        toReturn += GetSelfVariables(originalDoc, token)
    
    return toReturn
    
class Visitor(compiler.visitor.ASTVisitor):

    def __init__(self, classToVisit):
        self.classToVisit = classToVisit
        self.selfAttribs = []
        
    def visitClass(self, node):
        if node.name == self.classToVisit:
            for n in node.getChildNodes():
                self.visit(n)
        
    def visitAssign(self, node):
        
        for n in node.getChildNodes():
            if isinstance(n,compiler.ast.AssAttr):
                try:
                    if n.expr.name == 'self' and (n.attrname,'Instance attribute') not in self.selfAttribs:
                        self.selfAttribs.append((n.attrname,'Instance attribute'))
                except:
                    pass
            
    
def GetSelfVariables(theDoc, classToVisit):
    ast = compiler.parse(theDoc)
    
    visitor = Visitor(classToVisit) 
    compiler.walk(ast, visitor)
    
    return visitor.selfAttribs
    
    
    
    
    
    
    
    
    
    

    
    
    
    
    
    
    
    
    
    