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

class Resolver:
    """ prototype resolver class.
    Every container type should have a resolver that converts its members 
    into a dictionary """

    def resolve(self, var, attribute):
        if attribute is "implement":
            return "me"
    
    def getDictionary(self, var):
        return { "implement" : "me", "volunteers" : "needed" }

class DictResolver(Resolver):
    def resolve(self, var, attribute):
        return var[attribute]
    
    def getDictionary(self, var):
        return var

class ObjectResolver(Resolver):
    def resolve(self, var, attribute):
        if attribute is "__class__": return var.__class__ 
        return getattr(var, attribute)
    
    def getDictionary(self, var):
        if hasattr(var, '__dict__'):
            newDict = vars(var).copy()
            newDict['__class__'] = var.__class__
        else: 
            newDict = {}
            for d in dir(var):
                v = getattr(var, d)
                if inspect.ismethod(v) or inspect.isbuiltin(v) or inspect.isfunction(v) or inspect.iscode(v) or\
                   str(type(v)) == "<type 'method-wrapper'>": #could not discover anything on inpect to get this.
                    pass
                else:
                    newDict[d] = v
        return newDict

class TupleResolver(Resolver):
    def resolve(self, var, attribute):
        return var[int(attribute)]
    
    def getDictionary(self, var):
        newDict = {}
        i = 0;
        for x in var:
            newDict[i] = x
            i += 1
        return newDict
        
class FunctionResolver(Resolver):
    def resolve(self, var, attribute):
        return getattr(var, attribute)
        
    def getDictionary(self, var):
        return { "__doc__" : var.func_doc, "func_name" : var.func_name, 
            "func_defaults" : var.func_defaults, "__module__" : var.__module__,
            "func_code" : var.func_code, "func_globals" : var.func_globals,
            "func_dict" : var.func_dict, "func_closure" : var.func_closure}
            
class AbstractSpecialResolver(Resolver):
    def resolve(self, var, attribute):
        #internal object attributes are "special" and not accessible using getattr
        d = self.getDictionary(var)
        return d[attribute]
            
class MethodResolver(AbstractSpecialResolver):
    def getDictionary(self, var):
        return { "im_self" : var.im_self , "im_func" : var.im_func , 
            "im_class" : var.im_class  }
            
class CodeResolver(Resolver):
    def resolve(self, var, attribute):
        return getattr(var, attribute)
        
    def getDictionary(self, var):
        return { "co_name" : var.co_name , "co_argcount" : var.co_argcount, 
            "co_nlocals" : var.co_nlocals, "co_varnames" : var.co_varnames, 
            "co_cellvars" : var.co_cellvars , "co_freevars" : var.co_freevars , 
            "co_code" : var.co_code , "co_consts" : var.co_consts , 
            "co_names" : var.co_names , "co_filename" : var.co_filename , 
            "co_firstlineno" : var.co_firstlineno , "co_lnotab" : var.co_lnotab , 
            "co_stacksize" : var.co_stacksize, "co_flags" : hex(var.co_flags) }
            
class ClassResolver(AbstractSpecialResolver):
    def getDictionary(self, var):
        return { "__name__" : var.__name__ , "__module__" : var.__module__ , 
            "__dict__" : var.__dict__ , "__bases__" : var.__bases__ , 
            "__doc__" : var.__doc__  }
            
class ModuleResolver(Resolver):
    def resolve(self, var, attribute):
        if attribute is "__name__": return var.__name__ 
        if attribute is "__doc__": return var.__doc__ 
        if attribute is "__file__": return var.__file__ 
        return getattr(var, attribute)
    
    def getDictionary(self, var):
        newDict = vars(var).copy()
        newDict['__name__'] = var.__name__
        newDict['__doc__'] = var.__doc__
        try:
            newDict['__file__'] = var.__file__
        except:
            pass
        return newDict
        
class BuiltInResolver(AbstractSpecialResolver):
    def getDictionary(self, var):
        return { "__name__" : var.__name__ , "__module__" : var.__module__ , 
            "__self__" : var.__self__ ,    "__doc__" : var.__doc__  }
            
class FileResolver(AbstractSpecialResolver):
    def getDictionary(self, var):
        return { "closed" : var.closed , "encoding" : var.encoding, 
            "mode" : var.mode,    "name" : var.name, 
            "newlines" : var.newlines, "softspace" : var.softspace }
            
class TraceBackResolver(AbstractSpecialResolver):
    def getDictionary(self, var):
        return { "tb_next" : var.tb_next, "tb_frame" : var.tb_frame, 
            "tb_lineno" : var.tb_lineno,    "tb_lasti" : var.tb_lasti }
            
class FrameResolver(AbstractSpecialResolver):
    def getDictionary(self, var):
        return { "f_back" : var.f_back, "f_code" : var.f_code, 
            "f_locals" : var.f_locals,    "f_globals" : var.f_globals, 
            "f_builtins" : var.f_builtins, "f_restricted" : var.f_restricted,
            "f_lasti" : var.f_lasti, "f_trace" : var.f_trace, 
            "f_exc_type" : var.f_exc_type, "f_exc_value" : var.f_exc_value, 
            "f_exc_traceback" : var.f_exc_traceback, "f_lineno" : var.f_lineno }
            
class SliceResolver(AbstractSpecialResolver):
    def getDictionary(self, var):
        return { "start" : var.start, "stop" : var.stop, 
            "step" : var.step }
        
defaultResolver = Resolver()
dictResolver = DictResolver()
objectResolver = ObjectResolver()
tupleResolver = TupleResolver()
functionResolver = FunctionResolver()
methodResolver = MethodResolver()
codeResolver = CodeResolver()
classResolver = ClassResolver()
moduleResolver = ModuleResolver()
builtinResolver = BuiltInResolver()
fileResolver = FileResolver()
tracebackResolver = TraceBackResolver()
frameResolver = FrameResolver()
sliceResolver = SliceResolver()

def getType(o):
    """ returns a triple (typeObject, typeString, resolver
        resolver != None means that variable is a container, 
        and should be displayed as a hierarchy.
        Use the resolver to get its attributes.
        
        All container objects should have a resolver.
    """
    if isinstance(o, NoneType): return (NoneType, "None", None)
    if isinstance(o, IntType): return (IntType, "int", None)
    if isinstance(o, LongType): return (LongType, "long", None)
    if isinstance(o, FloatType): return (FloatType, "float", None)
    if isinstance(o, BooleanType): return (BooleanType, "boolean", None)
    if isinstance(o, ComplexType): return (ComplexType, "ComplexType", None)
    if isinstance(o, StringType): return (StringType, "string", None)
    if isinstance(o, UnicodeType): return (UnicodeType, "UnicodeType", None)
    if isinstance(o, BufferType): return (BufferType, "BufferType", None)
    if isinstance(o, TupleType): return (TupleType, "tuple", tupleResolver)
    if isinstance(o, ListType): return (ListType, "list", tupleResolver)
    if isinstance(o, DictType): return (DictType, "dict", dictResolver)
    if isinstance(o, FunctionType): return (FunctionType, "FunctionType", functionResolver)
    if isinstance(o, LambdaType): return (LambdaType, "LambdaType", functionResolver)
    if isinstance(o, GeneratorType): return (GeneratorType, "GeneratorType", None)
    if isinstance(o, ClassType): return (ClassType, "ClassType", classResolver)
    if isinstance(o, UnboundMethodType): return (UnboundMethodType, "UnboundMethodType", methodResolver)
    if isinstance(o, InstanceType): return (InstanceType, "InstanceType", defaultResolver)
    if isinstance(o, BuiltinFunctionType): return (BuiltinFunctionType, "built-in function", builtinResolver)
    if isinstance(o, BuiltinMethodType): return (BuiltinMethodType, "built-in method", builtinResolver)
    if isinstance(o, ModuleType): return (ModuleType, "ModuleType", moduleResolver)
    if isinstance(o, FileType): return (FileType, "FileType", fileResolver)
    if isinstance(o, XRangeType): return (XRangeType, "XRangeType", defaultResolver)
    if isinstance(o, TracebackType): return (TracebackType, "TracebackType", tracebackResolver)
    if isinstance(o, FrameType): return (FrameType, "FrameType", frameResolver)
    if isinstance(o, SliceType): return (SliceType, "SliceType", sliceResolver)
    if isinstance(o, EllipsisType): return (EllipsisType, "Ellipsis", defaultResolver)
    if isinstance(o, DictProxyType): return (DictProxyType, "DictProxyType", defaultResolver)
    if isinstance(o, NotImplementedType): return (NotImplementedType, "Not implemented type", None)
    if isinstance(o, CodeType): return(CodeType, "CodeType", codeResolver)
    if isinstance(o, ObjectType): return (ObjectType, "ObjectType", objectResolver)

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
