import sys


#completion types.
TYPE_UNKNOWN = -1
TYPE_IMPORT = 0
TYPE_CLASS = 1
TYPE_FUNCTION = 2
TYPE_ATTR = 3
TYPE_BUILTIN = 4
TYPE_PARAM = 5

TYPE_BUILTIN_AS_STR = '4'

def _imp(name):
    try:
        return __import__(name)
    except:
        if '.' in name:
            sub = name[0:name.rfind('.')]
            return _imp(sub)
        else:
            s = 'Unable to import module: %s - sys.path: %s' % (str(name), sys.path)
            raise RuntimeError(s)

def Find( name ):
    mod = _imp(name)
    components = name.split('.')

    for comp in components[1:]:
        mod = getattr(mod, comp)
    return mod


def GenerateTip( data ):
    data = data.replace( '\n', '' )
    if data.endswith( '.' ):
        data = data.rstrip( '.' )
    
    mod = Find( data )
    tips = GenerateImportsTipForModule( mod )
    return tips
    

import org.python.core as _core

def isclass(cls):
    return isinstance(cls, _core.PyClass)

def isfunction(func):
    return isinstance(func, _core.PyFunction)

def ismodule(mod):
    return isinstance(mod, _core.PyModule)



def GenerateImportsTipForModule( mod ):
    '''
    @param mod: the module from where we should get the completions
    '''
    ret = []
    
    dirComps = dir( mod )
    
    dontGetDocsOn = (float, int, str, tuple, list, type)
    for d in dirComps:

        args = ''

        obj = getattr(mod, d)
        retType = TYPE_BUILTIN

        if isfunction(obj):
            try:
                args, vargs, kwargs, defaults = inspect.getargspec( obj )
                    
                r = ''
                for a in ( args ):
                    if len( r ) > 0:
                        r += ', '
                    r += str( a )
                args = '(%s)' % (r)
            except TypeError:
                args = '()'

            retType = TYPE_FUNCTION
        
        
        doc = ''
        #add token and doc to return - assure only strings.
        ret.append(   (d, doc, args, str(retType))   )
        
            
    return ret


    
    