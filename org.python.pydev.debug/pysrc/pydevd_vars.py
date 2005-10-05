""" pydevd_vars deals with variables:
    resolution/conversion to XML.
"""
from types import *
import urllib
import threading
import sys

try:
    __setFalse = False
except:
    False = 0
    True = 1

class InspectStub:    
    def isbuiltin(self, args):       
        #return isinstance(args, types.BuiltinFunctionType)
        return False
    def isroutine(self, object):       
        return False
       
try:
    import inspect
except:
#    print "passou por except in import inspect"
    inspect = InspectStub()

try:
    import java.lang
except:
    pass

#types does not include a MethodWrapperType
try:
   MethodWrapperType = type([].__str__)   
except:
   MethodWrapperType = None


class VariableError(Exception):
    def __init__(self, message):
        Exception.__init__(self, message)

class Resolver:
    def resolve(self, var, attribute):        
        return getattr(var, attribute)
    
    def getDictionary(self,var):
#        print "Resolver"
               
        if MethodWrapperType:
            return self._getPyDictionary(var)
        else:
            return self._getJyDictionary(var)        

    def _getJyDictionary(self,obj):
        ret = {}
        found = java.util.HashMap()
    
        #from java.lang.reflect import Field
        #from java.lang.reflect import Method
    
        if hasattr(obj, '__class__') and obj.__class__ ==  java.lang.Class:
#            print "dirObj"
            declaredMethods = obj.getDeclaredMethods()
            declaredFields = obj.getDeclaredFields()
            for i in range(len(declaredMethods)):                
                name = declaredMethods[i].getName()
                ret[name] = declaredMethods[i].toString()
                found.put(name, 1)
                
            for i in range(len(declaredFields)):
                name = declaredFields[i].getName()
                found.put(name, 1)
                #if declaredFields[i].isAccessible():
                ret[name] = declaredFields[i].get( declaredFields[i] )                    
    
        #this simple dir does not always get all the info, that's why we have the part before
        #(e.g.: if we do a dir on String, some methods that are from other interfaces such as 
        #charAt don't appear)        
        d = []
        if hasattr(obj, '__dict__'):
            d = dir(obj)        
#        print "d",d
        for name in d:
            if found.get(name) is not 1:
                try:
                    ret[name] = getattr(obj, name)
                except:
                    #import traceback
                    #traceback.print_exc()
                    pass
        ret['type'] = type(obj).__name__
        
        return ret

    def _getPyDictionary(self,var):
        filterPrivate = False
        filterSpecial = True
        filterFunction = True
        filterBuiltIn = True
        
        names = dir(var)
        d = {}
        
        #Be aware that the order in which the filters are applied attempts to 
        #optimize the operation by removing as many items as possible in the 
        #first filters, leaving fewer items for later filters
        if filterSpecial:
            names = [n for n in names if not (n.startswith('__') and n.endswith('__') )]        
        if filterBuiltIn or filterFunction:
            nametemp = []
            for n in names:                
                attr = getattr(var, n)                
                if filterBuiltIn:
                    if inspect.isbuiltin(attr):                        
                        continue                
                if filterFunction:
                    isinst = False
                    if inspect.isroutine(attr) or isinstance(attr, MethodWrapperType): 
                        continue
                
                nametemp.append(n)
                
            names = nametemp
        
        if filterPrivate:
            names = [n for n in names if not (n.startswith('_') and not n.endswith('__') )]     
        
        for n in names:
            d[ n ] = getattr(var, n)        
        d['type'] = type(var).__name__
        return d        
                
class DictResolver(Resolver):
    def resolve(self, dict, key):
        return dict[key]
    
    def getDictionary(self, dict):
#        print "DictResolver"
        return dict

class TupleResolver(Resolver): #to enumerate tuples and lists
    def resolve(self, var, attribute):
        return var[int(attribute)]
    
    def getDictionary(self, var):
#        print "TupleResolver"
        #return dict( [ (i, x) for i, x in enumerate(var) ] )
        # modified 'cause jython does not have enumerate support
        d = {}
        for i, item in zip(range(len(var)), var):
            d[ i ] = item        
        return d
        
class InstanceResolver(Resolver):
    def resolve(self, var, attribute):        
        return getattr(var, attribute)
    
    def getDictionary(self,obj):
#        print "InstanceResolver"
        ret = {}       
        
        declaredFields = obj.__class__.getDeclaredFields()
#        print "declared fields, len", len(declaredFields)
        for i in range(len(declaredFields)):
            name = declaredFields[i].getName()
            #if declaredFields[i].isAccessible():
            try:                
                ret[name] = getattr( obj, name )                
            except:
                #import traceback
                #traceback.print_exc()
                # I don't know why I'm getting an exception
                pass        
        
        ret['type'] = type(obj).__name__        
        return ret
        
defaultResolver = Resolver()
dictResolver = DictResolver()
tupleResolver = TupleResolver()
instanceResolver = InstanceResolver()

typeMap = {}
try:
    #jython does not have this types
   typeMap[BooleanType] = (BooleanType, BooleanType.__name__, None)
   typeMap[BufferType] = (BufferType, BufferType.__name__, None)   
   typeMap = {
       NoneType : (NoneType, NoneType.__name__, None),
       IntType : (IntType, IntType.__name__, None),
       LongType : (LongType, LongType.__name__, None),
       FloatType : (FloatType, FloatType.__name__, None),
       ComplexType : (ComplexType, ComplexType.__name__, None),
       StringType : (StringType, StringType.__name__, None),
       UnicodeType : (UnicodeType, UnicodeType.__name__, None),
       TupleType : (TupleType, TupleType.__name__, tupleResolver),
       ListType : (ListType, ListType.__name__, tupleResolver),
       DictType : (DictType, DictType.__name__, dictResolver)
   }
except:   
   from org.python import core
   typeMap = {
       core.PyNone : ( core.PyNone, core.PyNone.__name__, None),
       core.PyInteger : ( core.PyInteger, core.PyInteger.__name__, None),
       core.PyLong : ( core.PyLong, core.PyLong.__name__, None),
       core.PyFloat : ( core.PyFloat, core.PyFloat.__name__, None),
       core.PyComplex : ( core.PyComplex, core.PyComplex.__name__, None),
       core.PyString : ( core.PyString, core.PyString.__name__, None),       
       core.PyTuple : ( core.PyTuple, core.PyTuple.__name__, tupleResolver),
       core.PyList : ( core.PyList, core.PyList.__name__, tupleResolver),
       core.PyDictionary: (core.PyDictionary, core.PyDictionary.__name__, dictResolver),
       core.PyJavaInstance: (core.PyJavaInstance, core.PyJavaInstance.__name__, instanceResolver),
       core.PyStringMap: (core.PyStringMap, core.PyStringMap.__name__, dictResolver)       
   }   
   pass


def getType(o):
    """ returns a triple (typeObject, typeString, resolver
        resolver != None means that variable is a container, 
        and should be displayed as a hierarchy.
        Use the resolver to get its attributes.
        
        All container objects should have a resolver.
    """    
    
    try:        
        if type(o).__name__=='org.python.core.PyJavaInstance':
            return (type(o), type(o).__name__, instanceResolver)
        for t in typeMap.keys():            
            if isinstance(o, t):                
                return typeMap[t]
    except:
        print typeMap
        print typeMap.__class__
        print dir( typeMap )
        
    #no match return default        
    return (type(o), type(o).__name__, defaultResolver)

def varToXML(v, name):
    """ single variable or dictionary to xml representation """
    xml = ""
    (type, typeName, resolver) = getType(v)    
    
    value = str(v)
    
    xml += '<var name="' + name + '" type="' + typeName + '"'
    if value: xml += ' value="' + urllib.quote(value, '/>_= \t') + '"'
    if resolver is not None: xml += ' isContainer="True"'
    xml += ' />\n'
    return xml

def frameVarsToXML(frame):
    """ dumps frame variables to XML
    <var name="var_name" scope="local" type="type" value="value"/>
    """    
#    print "frameVarsToXML"
    xml = ""
    keys = frame.f_locals.keys()
    keys.sort()    
    for k in keys:
        try:            
            v = frame.f_locals[k]            
            xml += varToXML(v, str(k))
        except Exception, e:
            import traceback
            traceback.print_exc()
            print >>sys.stderr,"unexpected error, recovered safely", str(e)
    return xml

def findFrame(thread, frame_id):
    """ returns a frame on the thread that has a given frame_id """
#    print "thread is ", str(thread)
#    print "current thread is", str(threading.currentThread)
    if thread != threading.currentThread() : raise VariableError("findFrame: must execute on same thread")
    curFrame = sys._getframe()
    if frame_id == "*": return curFrame # any frame is specified with "*"
    lookingFor = int(frame_id)
    while (curFrame != None and lookingFor != id(curFrame)):
        curFrame = curFrame.f_back
    if curFrame == None : raise VariableError("findFrame: frame not found")
    return curFrame

def resolveCompoundVariable(thread, frame_id, scope, attrs):
    """ returns the value of the compound variable as a dictionary"""    
    frame = findFrame(thread, frame_id)    
    attrList = attrs.split('\t')
    if (scope == "GLOBAL"):        
        var = frame.f_globals
        del attrList[0] # globals are special, and they get a single dummy unused attribute
    else:
        var = frame.f_locals
#    print "attrList",attrList
#    print "var",var    
    for k in attrList:
#        print "k", k
        (type, typeName, resolver) = getType(var)              
        var = resolver.resolve(var, k)
    
#    print >>sys.stderr, "Got variable", var
    
    try:        
        (type, typeName, resolver) = getType(var)        
        return resolver.getDictionary(var)
    except:
        import traceback
        traceback.print_exc()

    
def evaluateExpression( thread, frame_id, expression ):
    """returns the result of the evaluated expression"""
#    print "evaluate expression"
    frame = findFrame(thread, frame_id)
    result = None    
    try:
        result = eval( expression, frame.f_globals, frame.f_locals )
    except Exception, e:
        result = str( e )
    return result
