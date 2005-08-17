import traceback
import java.lang
import sys

from org.python.core import PyReflectedFunction

from org.python import core

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
    if name.startswith('__builtin__'):
        if name == '__builtin__.str':
            name = 'org.python.core.PyString'
        elif name == '__builtin__.dict':
            name = 'org.python.core.PyDictionary'
            
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
    

class Info:
    
    def __init__(self, name, **kwargs):
        self.name = name
        self.doc = kwargs.get('doc', None)
        self.args = kwargs.get('args', None) #tuple of strings
        self.varargs = kwargs.get('varargs', None) #string
        self.kwargs = kwargs.get('kwargs', None) #string
        self.ret = kwargs.get('ret', None) #string
        
    def basicAsStr(self):
        '''@returns this class information as a string (just basic format)
        '''
        
        s = 'function:%s args=%s, varargs=%s, kwargs=%s, docs:%s' % \
            (str(self.name), str( self.args), str( self.varargs), str( self.kwargs), str( self.doc))
        return s
        
    def getAsDoc(self):
        s = '''%s
        
@doc %s

@params %s
@varargs %s
@kwargs %s

@return %s
''' % (str(self.name), str( self.doc), str( self.args), str( self.varargs), str( self.kwargs), str( self.ret))
        return s
        
def isclass(cls):
    return isinstance(cls, core.PyClass)

def ismethod(func):
    '''this function should return the information gathered on a function
    
    @param func: this is the function we want to get info on
    @return a tuple where:
        0 = indicates whether the parameter passed is a method or not
        1 = a list of classes 'Info', with the info gathered from the function
            this is a list because when we have methods from java with the same name and different signatures,
            we actually have many methods, each with its own set of arguments
    '''
    
    if isinstance(func, core.PyFunction):
        #ok, this is from python, created by jython
        #print '    PyFunction'
        
        def getargs(func_code):
            """Get information about the arguments accepted by a code object.
        
            Three things are returned: (args, varargs, varkw), where 'args' is
            a list of argument names (possibly containing nested lists), and
            'varargs' and 'varkw' are the names of the * and ** arguments or None."""
        
            nargs = func_code.co_argcount
            names = func_code.co_varnames
            args = list(names[:nargs])
            step = 0
        
            varargs = None
            if func_code.co_flags & func_code.CO_VARARGS:
                varargs = func_code.co_varnames[nargs]
                nargs = nargs + 1
            varkw = None
            if func_code.co_flags & func_code.CO_VARKEYWORDS:
                varkw = func_code.co_varnames[nargs]
            return args, varargs, varkw
        
        args = getargs(func.func_code)
        return 1, [Info(func.func_name, args = args[0], varargs = args[1],  kwargs = args[2], doc = func.func_doc)]
        
    if isinstance(func, core.PyMethod):
        #this is something from java itself, and jython just wrapped it...
        
        #things to play in func:
        #['__call__', '__class__', '__cmp__', '__delattr__', '__dir__', '__doc__', '__findattr__', '__name__', '_doget', 'im_class',
        #'im_func', 'im_self', 'toString']
        #print '    PyMethod'
        #that's the PyReflectedFunction... keep going to get it
        func = func.im_func

    if isinstance(func, PyReflectedFunction):
        #this is something from java itself, and jython just wrapped it...
        
        #print '    PyReflectedFunction'
        
        infos = []
        for i in range(len(func.argslist)):
            #things to play in func.argslist[i]:
                
            #'PyArgsCall', 'PyArgsKeywordsCall', 'REPLACE', 'StandardCall', 'args', 'compare', 'compareTo', 'data', 'declaringClass'
            #'flags', 'isStatic', 'matches', 'precedence']
            
            #print '        ', func.argslist[i].data.__class__
            #func.argslist[i].data.__class__ == java.lang.reflect.Method
            
            if func.argslist[i]:
                met = func.argslist[i].data
                name = met.getName()
                ret = met.getReturnType()
                parameterTypes = met.getParameterTypes()
                
                args = []
                for j in range(len(parameterTypes)):
                    paramTypesClass = parameterTypes[j]
                    try:
                        paramClassName = paramTypesClass.getName()
                    except:
                        paramClassName = paramTypesClass.getName(paramTypesClass)
                    #if the parameter equals [C, it means it it a char array, so, let's change it
                    if paramClassName == '[C':
                        paramClassName = 'char[]'
                    args.append(paramClassName)

                
                info = Info(name, args = args, ret = ret)
                #print info.basicAsStr()
                infos.append(info)

        return 1, infos
        
    return 0, None

def ismodule(mod):
    #java modules... do we have other way to know that?
    if not hasattr(mod, 'getClass') and not hasattr(mod, '__class__') \
       and hasattr(mod, '__name__'):
           return 1
           
    return isinstance(mod, core.PyModule)


def dirObj(obj):
    ret = []
    found = java.util.HashMap()

    if hasattr(obj, '__class__') and obj.__class__ ==  java.lang.Class:
        declaredMethods = obj.getDeclaredMethods()
        declaredFields = obj.getDeclaredFields()
        for i in range(len(declaredMethods)):
            name = declaredMethods[i].getName()
            ret.append(name)
            found.put(name, 1)
            
        for i in range(len(declaredFields)):
            name = declaredFields[i].getName()
            ret.append(name)
            found.put(name, 1)

    #this simple dir does not always get all the info, that's why we have the part before
    #(e.g.: if we do a dir on String, some methods that are from other interfaces such as 
    #charAt don't appear)
    d = dir(obj)
    for name in d:
        if found.get(name) is not 1:
            ret.append(name)
            
    return ret


def formatArg(arg):
    '''formats an argument to be shown
    '''
    
    s = str( arg )
    dot = s.rfind('.')
    if dot >= 0:
        s = s[dot+1:]
    
    s = s.replace(';','')
    return s
    
    
def GenerateImportsTipForModule( mod ):
    '''
    @param mod: the module from where we should get the completions
    '''
    ret = []
    
    dirComps = dirObj( mod )
    
    dontGetDocsOn = (float, int, str, tuple, list, type)
    for d in dirComps:

        args = ''
        doc = ''
        retType = TYPE_BUILTIN

        try:
            obj = getattr(mod, d)
        except:
            #that's ok, private things cannot be gotten...
            continue
        else:

            isMet = ismethod(obj)
            if isMet[0]:
                info = isMet[1][0]
                try:
                    args, vargs, kwargs = info.args, info.varargs, info.kwargs
                    doc = info.getAsDoc()
                    r = ''
                    for a in ( args ):
                        if len( r ) > 0:
                            r += ', '
                        r += formatArg(a)
                    args = '(%s)' % (r)
                except TypeError:
                    print traceback.print_exc()
                    args = '()'
    
                retType = TYPE_FUNCTION
                
            elif isclass(obj):
                retType = TYPE_CLASS
                
            elif ismodule(obj):
                retType = TYPE_IMPORT
        
        #add token and doc to return - assure only strings.
        ret.append(   (d, doc, args, str(retType))   )
        
            
    return ret

