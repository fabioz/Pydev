import inspect
import sys


#completion types.
TYPE_UNKNOWN = -1
TYPE_IMPORT = 0
TYPE_CLASS = 1
TYPE_FUNCTION = 2
TYPE_ATTR = 3
TYPE_BUILTIN = 4
TYPE_PARAM = 5


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
    
    


def GenerateImportsTipForModule( mod ):
    ret = []
    
    for d in dir( mod ):
        
        args = ''
        obj = getattr(mod, d)
        type = TYPE_BUILTIN

        if inspect.ismethod(obj) or inspect.isbuiltin(obj) or inspect.isfunction(obj) or inspect.isroutine(obj):
            try:
                args, vargs, kwargs, defaults = inspect.getargspec( obj )
                    
                r = ''
                for a in ( args ):
                    if len( r ) > 0:
                        r += ', '
                    r += str( a )
                args = '(%s)' % (r)
            except TypeError:

            #print obj, args
                args = '()'
            type = TYPE_FUNCTION

        #add token and doc to return - assure only strings.
        ret.append(   (d, inspect.getdoc( obj ), args, str(type))   )

    return ret


    
    