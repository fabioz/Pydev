import copy
import os
import sys
import os.path 
import inspect

_accessibleModules = {}

def isModuleWithinList(mod, lis, dbg = False):
    for l in lis:
        if l[0] == mod:
            if dbg:
                print 'returning', l
            return l
    return None
    
def ParseDir(d):
    ret = []
    if os.path.exists(d) and os.path.isdir(d):
        contents = os.listdir(d)
        
        for f in contents:
            absolute = os.path.join(d, f)
            
            if os.path.isfile(absolute):
                m = None
                
                #if it is a file, just check if it is a valid module.
                if f.endswith('.py'):
                    m = stripExtension(f, ret, '.py')

                if f.endswith('.pyc'):
                    m = stripExtension(f, ret, '.pyc')

                if f.endswith('.pyd'):
                    m = stripExtension(f, ret, '.pyd')

                if f.endswith('.dll'):
                    m = stripExtension(f, ret, '.dll')

                if f.endswith('.pyo'):
                    m = stripExtension(f, ret, '.pyo')

                if m is not None and isModuleWithinList(m, ret) is None:
                    ret.append((m,absolute))
                    
            elif os.path.isdir(absolute):
                contents2 = os.listdir(absolute)
                
                if '__init__.py' in contents2 or '__init__.pyc' in contents2:
                    ret.append((f,absolute))
                    
    return ret

def stripExtension(f, ret, ext):
    return f[0:-len(ext)]

def GenerateTip(data):
    data = data.replace('\n','')
    if data.endswith('.'):
        data = data.rstrip('.')
    
    if data.strip() == '':
        return GenerateImportsTip([])
    
    splitted = data.split('.')
    return GenerateImportsTip(splitted)


def GenerateImportsTip(tokenList, pth = sys.path, completeModule = ''):
    pythonPath = pth

    #first, just get the root modules
    mods = []
    for d in pythonPath:
        if _accessibleModules.get(d, None) == None:
            mods += ParseDir(d)
    
    if len(tokenList) == 0:
        return mods
    
    else:
        token = tokenList[0]
        if len(completeModule) > 0:
            completeModule += '.'
        completeModule += token
        
        
        newTokenList = tokenList[1:]
        mod = isModuleWithinList(token , mods) 

        if mod is not None:

            if os.path.isfile(mod[1]):
                mod = myImport(completeModule)
                return GenerateImportsTipForModule(newTokenList, mod)
                
            elif os.path.isdir(mod[1]):
                return GenerateImportsTip(newTokenList, [mod[1]], completeModule)
        
        else:
            mod = myImport(completeModule)
            return GenerateImportsTipForModule(newTokenList, mod)
    
    raise RuntimeError('Unable to complete.')
    
def myImport(name):
    mod = __import__(name)
    components = name.split('.')
    for comp in components[1:]:
        mod = getattr(mod, comp)
    return mod


def GenerateImportsTipForModule(tokenList, mod):
    ret = []
    
    while len(tokenList) > 0:
        mod = getattr(mod, tokenList.pop(0))
        
    for d in dir(mod):
        ret.append([d,inspect.getdoc(getattr(mod, d))])
    return ret


    
    