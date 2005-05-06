""" pydevd_vars deals with variables:
    resolution/conversion to XML.
"""
from types import *
import urllib
import threading
import sys
import inspect

class VariableError(Exception):
    def __init__(self, message):
        Exception.__init__(self, message)

class Resolver(object):
    def resolve(self, var, attribute):
        return getattr(var, attribute)
        
    def getDictionary(self, var):
        filterPrivate = False
        filterSpecial = True
        filterFunction = True
        filterBuiltIn = True
        
        names = dir(var)
        
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
                    if inspect.ismethod(attr) or inspect.isfunction(attr):
                        continue
                
                nametemp.append(n)
                
            names = nametemp
        
        if filterPrivate:
            names = [n for n in names if not (n.startswith('_') and not n.endswith('__') )]
        
        d = dict( [ (n, getattr(var, n)) for n in names])
        d['type'] = type(var).__name__
        return d
                
class DictResolver(Resolver):
    def resolve(self, dict, key):
        return dict[key]
    
    def getDictionary(self, dict):
        return dict

class TupleResolver(Resolver): #to enumerate tuples and lists
    def resolve(self, var, attribute):
        return var[int(attribute)]
    
    def getDictionary(self, var):
        return dict( [ (i, x) for i, x in enumerate(var) ] )
        
defaultResolver = Resolver()
dictResolver = DictResolver()
tupleResolver = TupleResolver()

typeMap = {NoneType : (NoneType, NoneType.__name__, None), \
    IntType : (IntType, IntType.__name__, None), \
    LongType : (LongType, LongType.__name__, None), \
    FloatType : (FloatType, FloatType.__name__, None), \
    BooleanType : (BooleanType, BooleanType.__name__, None), \
    ComplexType : (ComplexType, ComplexType.__name__, None), \
    StringType : (StringType, StringType.__name__, None), \
    UnicodeType : (UnicodeType, UnicodeType.__name__, None), \
    BufferType : (BufferType, BufferType.__name__, None), \
    TupleType : (TupleType, TupleType.__name__, tupleResolver), \
    ListType : (ListType, ListType.__name__, tupleResolver), \
    DictType : (DictType, DictType.__name__, dictResolver) }

def getType(o):
    global typeMap
    """ returns a triple (typeObject, typeString, resolver
        resolver != None means that variable is a container, 
        and should be displayed as a hierarchy.
        Use the resolver to get its attributes.
        
        All container objects should have a resolver.
    """
    if type(o) in typeMap:
        return typeMap[type(o)]
    else:
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
#    print "Calling frameVarsToXML"
    xml = ""
    keys = frame.f_locals.keys()
    keys.sort()
    for k in keys:
        try:
            v = frame.f_locals[k]
            xml += varToXML(v, str(k))
        except Exception, e:
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
    for k in attrList:
#        print >>sys.stderr, "resolving ", k
#        print >>sys.stderr, "attribute is ", k
        (type, typeName, resolver) = getType(var)
        var = resolver.resolve(var, k)
    
#    print >>sys.stderr, "Got variable", var
    (type, typeName, resolver) = getType(var)
    return resolver.getDictionary(var)

    
def evaluateExpression( thread, frame_id, expression ):
    """returns the result of the evaluated expression"""
    frame = findFrame(thread, frame_id)
    result = None    
    try:
        result = eval( expression, frame.f_globals, frame.f_locals )
    except Exception, e:
        result = str( e )
    return result
