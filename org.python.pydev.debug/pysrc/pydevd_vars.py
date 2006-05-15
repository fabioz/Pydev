#@PydevCodeAnalysisIgnore that's because some things rely on jython, some on python... so, let's keep it like that
""" pydevd_vars deals with variables:
    resolution/conversion to XML.
"""
from types import *
import urllib
import threading
import sys
import pydevd_resolver
import traceback

#-------------------------------------------------------------------------- defining true and false for earlier versions

try:
    __setFalse = False
except:
    #early versions of python do not have it.
    False = 0
    True = 1

#let's see if we can use yield
try:
    def yieldStmt():yield(True)
    yieldStmt()
    hasYield = True
except:
    hasYield = False

#------------------------------------------------------------------------------------------------------ class for errors

class VariableError(RuntimeError):pass
class FrameNotFoundError(RuntimeError):pass


#------------------------------------------------------------------------------------------------------ resolvers in map

typeMap = {}
try:
    #jython does not have this types
   try:
       typeMap[BooleanType] = (BooleanType, BooleanType.__name__, None)
   except NameError:
       pass #early versions of python do not have it.
   typeMap[BufferType] = (BufferType, BufferType.__name__, None)   
   typeMap = {
       NoneType : (NoneType, NoneType.__name__, None),
       IntType : (IntType, IntType.__name__, None),
       LongType : (LongType, LongType.__name__, None),
       FloatType : (FloatType, FloatType.__name__, None),
       ComplexType : (ComplexType, ComplexType.__name__, None),
       StringType : (StringType, StringType.__name__, None),
       UnicodeType : (UnicodeType, UnicodeType.__name__, None),
       TupleType : (TupleType, TupleType.__name__, pydevd_resolver.tupleResolver),
       ListType : (ListType, ListType.__name__, pydevd_resolver.tupleResolver),
       DictType : (DictType, DictType.__name__, pydevd_resolver.dictResolver)
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
       core.PyTuple : ( core.PyTuple, core.PyTuple.__name__, pydevd_resolver.tupleResolver),
       core.PyList : ( core.PyList, core.PyList.__name__, pydevd_resolver.tupleResolver),
       core.PyDictionary: (core.PyDictionary, core.PyDictionary.__name__, pydevd_resolver.dictResolver),
       core.PyJavaInstance: (core.PyJavaInstance, core.PyJavaInstance.__name__, pydevd_resolver.instanceResolver),
       core.PyStringMap: (core.PyStringMap, core.PyStringMap.__name__, pydevd_resolver.dictResolver)       
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
            return (type(o), type(o).__name__, pydevd_resolver.instanceResolver)
        
        if type(o).__name__=='org.python.core.PyArray':
            return (type(o), type(o).__name__, pydevd_resolver.jyArrayResolver)    
        
        for t in typeMap.keys():            
            if isinstance(o, t):                
                return typeMap[t]
    except:
        traceback.print_exc()
        print typeMap
        print typeMap.__class__
        print dir( typeMap )
        
    #no match return default        
    return (type(o), type(o).__name__, pydevd_resolver.defaultResolver)


def makeValidXmlValue( s):
    return s.replace('<', '&lt;').replace('>', '&gt;')


def varToXML(v, name):
    """ single variable or dictionary to xml representation """
    type, typeName, resolver = getType(v)    
    
    try:
        if hasattr(v, '__class__'):
            try:
                cName = str(v.__class__)
                if cName.find('.') != -1:
                    cName = cName.split('.')[-1]
                
                elif cName.find("'") != -1: #does not have '.' (could be something like <type 'int'>)
                    cName = cName[cName.index("'")+1:]
                    
                if cName.endswith("'>"):
                    cName = cName[:-2]
            except:
                cName = str(v.__class__)
            value = '%s: %s' % (cName, v)
        else:
            value = str(v)
    except:
        try:
            value = `v`
        except:
            value = 'Unable to get repr for %s' % v.__class__
    
    xml = '<var name="%s" type="%s"' % (name, typeName)
    
    if value: 
        #cannot be too big... communication does not handle it.
        if len(value) >  200:
            value = value[0:200]
            value += '...'

        xmlValue = ' value="%s"' % (makeValidXmlValue(urllib.quote(value, '/>_= \t')))
    else:
        xmlValue = ''
        
    if resolver is not None: 
        xmlCont = ' isContainer="True"'
    else:
        xmlCont = ''
        
    return ''.join((xml, xmlValue, xmlCont, ' />\n'))

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
            traceback.print_exc()
            print >>sys.stderr,"unexpected error, recovered safely", str(e)
    return xml

if not hasYield:
    def iterFrames(initialFrame):
        '''NO-YIELD VERSION: Iterates through all the frames starting at the specified frame (which will be the first returned item)'''
        #cannot use yield
        frames = []
        
        while initialFrame is not None:
            frames.append(initialFrame)
            initialFrame = initialFrame.f_back
            
        return frames
else:
    def iterFrames(initialFrame):
        '''Iterates through all the frames starting at the specified frame (which will be the first returned item)'''
        #let's use yield
        while initialFrame is not None:
            yield (initialFrame)
            initialFrame = initialFrame.f_back
            
        raise StopIteration()

def findFrame(thread_id, frame_id):
    """ returns a frame on the thread that has a given frame_id """
    if thread_id != id(threading.currentThread()) : 
        raise VariableError("findFrame: must execute on same thread")

    curFrame = sys._getframe()
    if frame_id == "*": 
        return curFrame # any frame is specified with "*"
    
    frameFound = None
    lookingFor = int(frame_id)
    
    for frame in iterFrames(curFrame):
        if lookingFor == id(frame):
            frameFound = frame
            break
        
    if frameFound == None: 
        msgFrames = ''
        i = 0
        for frame in iterFrames(sys._getframe()):
            i += 1
            msgFrames += str(id(frame))
            if i % 5 == 0:
                msgFrames += '\n'
            else:
                msgFrames += '  -  '
                
        errMsg = '''findFrame: frame not found.
Looking for thread_id:%s, frame_id:%s
Current     thread_id:%s, available frames:
%s
''' % (thread_id, lookingFor, id(threading.currentThread()), msgFrames)

        raise FrameNotFoundError(errMsg)
    
    return frameFound

def resolveCompoundVariable(thread_id, frame_id, scope, attrs):
    """ returns the value of the compound variable as a dictionary"""    
    frame = findFrame(thread_id, frame_id)    
    attrList = attrs.split('\t')
    if scope == "GLOBAL":        
        var = frame.f_globals
        del attrList[0] # globals are special, and they get a single dummy unused attribute
    else:
        var = frame.f_locals

    for k in attrList:
        type, typeName, resolver = getType(var)              
        var = resolver.resolve(var, k)
    
    try:        
        type, typeName, resolver = getType(var)        
        return resolver.getDictionary(var)
    except:
        traceback.print_exc()
    
def evaluateExpression( thread_id, frame_id, expression, doExec ):
    '''returns the result of the evaluated expression
    @param doExec: determines if we should do an exec or an eval
    '''
    frame = findFrame(thread_id, frame_id)
    
    expression = expression.replace('@LINE@', '\n')
    if doExec:
        exec expression in frame.f_globals, frame.f_locals
        return 
    
    else:
        result = None    
        try:
            result = eval( expression, frame.f_globals, frame.f_locals )
        except Exception, e:
            result = str( e )
        return result
