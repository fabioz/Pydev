import inspect
import sys

def find_class( module, name):
    __import__(module)
    mod   = sys.modules[module]
    klass = getattr(mod, name)
    return klass


def FindClass( p_full_class_ ):
    import types
    type    = p_full_class_.split('.')
    module_ = '.'.join( type[:-1] )
    class_  = type[-1]

    if class_ == 'NoneType':
        return types.NoneType

    if module_:
        return find_class( module_, class_ )
    else:
        return eval( class_ )

def _genMod( toks ):
    ret = ''
    
    for t in toks:
        if len(ret) > 0:
            ret += '.'
        ret += t
        
    return ret

def ImportMod(name):
    '''
    Method used to import a module from a string.
    '''
    components = name.split('.')
    
    raised = False
    for c in components:
        if not raised:
            try:
                mod = __import__(c)
            except:
                raised = True
        
        if raised:
            mod = getattr(mod, c)
            
    return mod



def GenerateTip( data ):
    data = data.replace( '\n', '' )
    if data.endswith( '.' ):
        data = data.rstrip( '.' )
    

    try:
        mod = FindClass( data )
    except:
        mod = ImportMod( data )
    return GenerateImportsTipForModule( mod )
    
    


def GenerateImportsTipForModule( mod ):
    ret = []
    
    for d in dir( mod ):
        
        args = ""
        
        obj = getattr(mod, d)
        if inspect.isfunction(obj) or inspect.ismethod (obj):
        
            args, vargs, kwargs, defaults = inspect.getargspec( obj )
            if len( args ) > 0 and args[0] == 'self':
                args = args[1:]
                
            r = '('
            for a in ( args ):
                if len( r ) > 1:
                    r += ', '
                r += str( a )
            r += ')'
            args = r
            #print obj, args

        #add token and doc to return
        ret.append(   (d, inspect.getdoc( getattr( mod, d ) ), args)   )

    return ret


    
    