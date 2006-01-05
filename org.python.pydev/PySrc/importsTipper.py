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
    f = None
    mod = _imp(name)
    if inspect.ismodule(mod) and hasattr(mod, '__file__'):
        f = mod.__file__
        
    components = name.split('.')

    for comp in components[1:]:
        mod = getattr(mod, comp)
        if inspect.ismodule(mod) and hasattr(mod, '__file__'):
            f = mod.__file__
    return f, mod


def GenerateTip( data ):
    data = data.replace( '\n', '' )
    if data.endswith( '.' ):
        data = data.rstrip( '.' )
        
    f, mod = Find( data )
    #print >> open('temp.txt', 'w'), f
    tips = GenerateImportsTipForModule( mod )
    return f,tips
    
    


def GenerateImportsTipForModule( mod ):
    '''
    @param mod: the module from where we should get the completions
    '''
    ret = []
    
    dirComps = dir( mod )
    if hasattr(mod, '__dict__'):
        dirComps.append('__dict__')
        
    getCompleteInfo = True
    
    if len(dirComps) > 1000:
        #ok, we don't want to let our users wait forever... 
        #no complete info for you...
        
        getCompleteInfo = False
    
    dontGetDocsOn = (float, int, str, tuple, list, type)
    for d in dirComps:

        args = ''

        if getCompleteInfo:
            obj = getattr(mod, d)
            retType = TYPE_BUILTIN

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
                    args = '()'

                retType = TYPE_FUNCTION
            
            
            #check if we have to get docs
            getDoc = True
            for class_ in dontGetDocsOn:
                if isinstance(obj, class_):
                    getDoc = False
                    break
                    
            doc = ''
            if getDoc:
                #no need to get this info... too many constants are defined and 
                #makes things much slower (passing all that through sockets takes quite some time)
                doc = inspect.getdoc( obj )
            
            #add token and doc to return - assure only strings.
            ret.append(   (d, doc, args, str(retType))   )
            
        else: #getCompleteInfo == False
        
            #ok, no complete info, let's try to do this as fast and clean as possible
            #so, no docs for this kind of information, only the signatures
            ret.append(   (d, '', args, TYPE_BUILTIN_AS_STR)   )
            
    return ret


    
    