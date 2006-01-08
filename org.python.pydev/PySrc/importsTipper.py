import os.path
import re
import linecache
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

def GetFile(mod):
    f = None
    try:
        f = inspect.getsourcefile(mod) or inspect.getfile(mod)
    except:
        if hasattr(mod,'__file__'):
            f = mod.__file__
            if string.lower(f[-4:]) in ['.pyc', '.pyo']:
                filename = f[:-4] + '.py'
                if os.path.exists(filename):
                    f = filename
            
    return f

def Find( name ):
    f = None
    parent = None
    
    mod = _imp(name)
    parent = mod
    foundAs = ''
    
    if inspect.ismodule(mod):
        f = GetFile(mod)
        
    components = name.split('.')

    for comp in components[1:]:
        mod = getattr(mod, comp)
        if inspect.ismodule(mod):
            f = GetFile(mod)
        else:
            if len(foundAs) > 0:
                foundAs += '.'
            foundAs += comp
            
    return f, mod, parent, foundAs

def Search( data ):
    '''@return file, line, col
    '''
    
    data = data.replace( '\n', '' )
    if data.endswith( '.' ):
        data = data.rstrip( '.' )
    f, mod, parent, foundAs = Find( data )
    try:
        return DoFind(f, mod), foundAs
    except:
        return DoFind(f, parent), foundAs
    
    
def DoFind(f, mod):
    if inspect.ismodule(mod):
        return f, 0, 0
    
    lines = linecache.getlines(f)
    if inspect.isclass(mod):
        name = mod.__name__
        pat = re.compile(r'^\s*class\s*' + name + r'\b')
        for i in range(len(lines)):
            if pat.match(lines[i]): 
                return f, i, 0
            
        return f, 0, 0

    if inspect.ismethod(mod):
        mod = mod.im_func
    if inspect.isfunction(mod):
        mod = mod.func_code
    if inspect.istraceback(mod):
        mod = mod.tb_frame
    if inspect.isframe(mod):
        mod = mod.f_code

    if inspect.iscode(mod):
        if not hasattr(mod, 'co_filename'):
            return None, 0, 0
        if not hasattr(mod, 'co_firstlineno'):
            return mod.co_filename, 0, 0
        
        lnum = mod.co_firstlineno - 1
        pat = re.compile(r'^(\s*def\s)|(.*(?<!\w)lambda(:|\s))|^(\s*@)')
        while lnum > 0:
            if pat.match(lines[lnum]): break
            lnum = lnum - 1
        return f, lnum, 0

    raise RuntimeError('Do not know about: '+data+' '+str(mod))
        
    
    
    
    
def GenerateTip( data ):
    data = data.replace( '\n', '' )
    if data.endswith( '.' ):
        data = data.rstrip( '.' )
        
    f, mod, parent, foundAs = Find( data )
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


    
    