import unittest
from java.lang.reflect import Method
from java.lang.reflect import Field
from java.lang import Class
from java.lang import System
from java.lang import String
from java.lang import Object
from java.lang.System import arraycopy
from java.lang.System import out
from java.io import OutputStream

from org.python.core import PyReflectedFunction
from org.python.core import PyReflectedField
from org.python.core import PyReflectedConstructor

from org.python import core
from org.python.core import PyReflectedFunction


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
        print '    PyFunction'
        
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
        print '    PyMethod'
        #that's the PyReflectedFunction... keep going to get it
        func = func.im_func

    if isinstance(func, PyReflectedFunction):
        #this is something from java itself, and jython just wrapped it...
        
        print '    PyReflectedFunction'
        
        infos = []
        for i in range(len(func.argslist)):
            #things to play in func.argslist[i]:
                
            #'PyArgsCall', 'PyArgsKeywordsCall', 'REPLACE', 'StandardCall', 'args', 'compare', 'compareTo', 'data', 'declaringClass'
            #'flags', 'isStatic', 'matches', 'precedence']
            
            #print '        ', func.argslist[i].data.__class__
            #func.argslist[i].data.__class__ == java.lang.reflect.Method
            
            met = func.argslist[i].data
            name = met.getName()
            ret = met.getReturnType()
            parameterTypes = met.getParameterTypes()
            
            args = []
            for j in range(len(parameterTypes)):
                paramTypesClass = parameterTypes[j]
                paramClassName = paramTypesClass.getName()
                #if the parameter equals [C, it means it it a char array, so, let's change it
                if paramClassName == '[C':
                    paramClassName = 'char[]'
                args.append(paramClassName)

                
            info = Info(name, args = args, ret = ret)
            print info.basicAsStr()
            infos.append(info)

        return 1, infos
        
    return 0, None

def ismodule(mod):
    return isinstance(mod, core.PyModule)


def dirObj(obj):
    return dir(obj)

class Test(unittest.TestCase):

    def setUp(self):
        unittest.TestCase.setUp(self)

    def tearDown(self):
        unittest.TestCase.tearDown(self)

    def testGettingInfoOnJython(self):
        
        print '\n\n--------------------------- Method'
        assert not ismethod(Method)[0]
        assert isclass(Method)
            
        print '\n\n--------------------------- System'
        assert not ismethod(System)[0]
        assert isclass(System)
            
        print '\n\n--------------------------- String'
        assert not ismethod(System)[0]
        assert isclass(String)
        assert len(dirObj(String)) > 10
            
        print '\n\n--------------------------- arraycopy'
        assert ismethod(arraycopy)[0]
        assert not isclass(arraycopy)
            
        print '\n\n--------------------------- out'
        assert ismethod(out)
        assert not isclass(out)
            
        print '\n\n--------------------------- out.println'
        isMet = ismethod(out.println)
        assert isMet[0]
        assert len(isMet[1]) == 10
        assert isMet[1][0].basicAsStr() == "function:println args=[], varargs=None, kwargs=None, docs:None"
        assert isMet[1][1].basicAsStr() == "function:println args=['long'], varargs=None, kwargs=None, docs:None"
        assert not isclass(out.println)
        
        print '\n\n--------------------------- str'
        isMet = ismethod(str)
        assert isMet[0]
        assert isMet[1][0].basicAsStr() == "function:str args=['org.python.core.PyObject'], varargs=None, kwargs=None, docs:None"
        assert not isclass(str)
        
        
        def met1():
            a=3
            return a
        
        print '\n\n--------------------------- met1'
        isMet = ismethod(met1)
        assert isMet[0]
        assert isMet[1][0].basicAsStr() == "function:met1 args=[], varargs=None, kwargs=None, docs:None"
        assert not isclass(met1)
        
        def met2(arg1, arg2, *vararg, **kwarg):
            '''docmet2'''
            
            a=1
            return a
        
        print '\n\n--------------------------- met2'
        isMet = ismethod(met2)
        assert isMet[0]
        assert isMet[1][0].basicAsStr() == "function:met2 args=['arg1', 'arg2'], varargs=vararg, kwargs=kwarg, docs:docmet2"
        assert not isclass(met2)
        


if __name__ == '__main__':
    unittest.main()
