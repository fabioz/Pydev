""" pydevd_vars deals with variables:
	resolution/conversion to XML.
"""
from types import *
import urllib
import threading
import sys

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
		newDict = vars(var).copy()
		newDict['__class__'] = var.__class__
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
		
defaultResolver = Resolver()
dictResolver = DictResolver()
objectResolver = ObjectResolver()
tupleResolver = TupleResolver()

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
	if isinstance(o, FunctionType): return (FunctionType, "FunctionType", defaultResolver)
	if isinstance(o, LambdaType): return (LambdaType, "LambdaType", defaultResolver)
	if isinstance(o, GeneratorType): return (GeneratorType, "GeneratorType", None)
	if isinstance(o, ClassType): return (ClassType, "ClassType", defaultResolver)
	if isinstance(o, UnboundMethodType): return (UnboundMethodType, "UnboundMethodType", defaultResolver)
	if isinstance(o, InstanceType): return (InstanceType, "InstanceType", defaultResolver)
	if isinstance(o, BuiltinFunctionType): return (BuiltinFunctionType, "built-in function", None)
	if isinstance(o, BuiltinMethodType): return (BuiltinMethodType, "built-in method", None)
	if isinstance(o, ModuleType): return (ModuleType, "ModuleType", defaultResolver)
	if isinstance(o, FileType): return (FileType, "FileType", defaultResolver)
	if isinstance(o, XRangeType): return (XRangeType, "XRangeType", defaultResolver)
	if isinstance(o, TracebackType): return (TracebackType, "TracebackType", defaultResolver)
	if isinstance(o, FrameType): return (FrameType, "FrameType", defaultResolver)
	if isinstance(o, SliceType): return (SliceType, "SliceType", defaultResolver)
	if isinstance(o, EllipsisType): return (EllipsisType, "Ellipsis", defaultResolver)
	if isinstance(o, DictProxyType): return (DictProxyType, "DictProxyType", defaultResolver)
	if isinstance(o, NotImplementedType): return (NotImplementedType, "Not implemented type", None)
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
#	print "Calling frameVarsToXML"
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
#	print "thread is ", str(thread)
#	print "current thread is", str(threading.currentThread)
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
#		print >>sys.stderr, "resolving ", k
#		print >>sys.stderr, "attribute is ", k
		(type, typeName, resolver) = getType(var)
		var = resolver.resolve(var, k)
	
#	print >>sys.stderr, "Got variable", var
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
