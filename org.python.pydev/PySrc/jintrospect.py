"""Extend introspect.py for Java based Jython classes."""

from introspect import *
import string
from debug import *

__author__ = "Don Coleman <dcoleman@chariotsolutions.com>"
__cvsid__ = "$Id: jintrospect.py,v 1.1 2004-05-25 21:36:56 dana_virtual Exp $"

def getAutoCompleteList(command='', locals=None, includeMagic=1, 
                        includeSingle=1, includeDouble=1):
    """Return list of auto-completion options for command.
    
    The list of options will be based on the locals namespace."""
    attributes = []
    # Get the proper chunk of code from the command.
    root = getRoot(command, terminator='.')
    debug( "JgetAutoCompleteList: root ", root)  
    try:
        if locals is not None:
            object = eval(root, locals)
        else:
            object = eval(root)
    except:
        return attributes
    debug( "JgetAutoCompleteList:  object ", object)
    if ispython(object):
        # use existing code
        attributes = getAttributeNames(object, includeMagic, includeSingle, includeDouble)
    else:
        methods = methodsOf(object.__class__)
        attributes = [eachMethod.__name__ for eachMethod in methods]
        
    return attributes

def methodsOf(clazz):
    """return a list of all the methods in a class"""
    classMembers = vars(clazz).values()
    methods = [eachMember for eachMember in classMembers if callable(eachMember)]
    for eachBase in clazz.__bases__:
        methods.extend(methodsOf(eachBase))
    return methods

def getCallTipJava(command='', locals=None):
    """For a command, return a tuple of object name, argspec, tip text.

    The call tip information will be based on the locals namespace."""

    calltip = ('', '', '')  # object name, argspec, tip text.

    # Get the proper chunk of code from the command.
    root = getRoot(command, terminator='(')

    try:
        if locals is not None:
            object = eval(root, locals)
        else:
            object = eval(root)
    except:
        return calltip

    if ispython(object):
        # Patrick's code handles python code
        # TODO fix in future because getCallTip runs eval() again
        return getCallTip(command, locals)

    name = ''
    try:
        name = object.__name__
    except AttributeError:
        pass
    
    tipList = []
    argspec = '' # not using argspec for Java
    
    if inspect.isbuiltin(object):
        # inspect.isbuiltin() fails for Jython
        # Can we get the argspec for Jython builtins?  We can't in Python.
        pass
    elif inspect.isclass(object):
        # get the constructor(s)
        # TODO consider getting modifiers since jython can access private methods
        constructors = object.getConstructors()
        for constructor in constructors:
            paramList = []
            paramTypes = constructor.getParameterTypes()
            # paramTypes is an array of classes, we need Strings
            # TODO consider list comprehension
            for param in paramTypes:
                # TODO translate [B to byte[], [C to char[] etc
                paramList.append(param.__name__)
            paramString = string.join(paramList,', ')
            tip = "%s(%s)" % (constructor.name, paramString)
            tipList.append(tip)
             
    elif inspect.ismethod(object):
        method = object
        object = method.im_class

        # java allows overloading so we may have more than one method
        methodArray = object.getMethods()

        for eachMethod in methodArray:
            if eachMethod.name == method.__name__:
                paramList = []
                for eachParam in eachMethod.parameterTypes:
                    paramList.append(eachParam.__name__)
                 
                paramString = string.join(paramList,', ')

                # create a python style string a la PyCrust
                # we're showing the parameter type rather than the parameter name, since that's all I can get
                # we need to show multiple methods for overloading
                # TODO improve message format
                # do we want to show the method visibility
                # how about exceptions?
                # note: name, return type and exceptions same for EVERY overload method

                tip = "%s(%s) -> %s" % (eachMethod.name, paramString, eachMethod.returnType)
                tipList.append(tip)
            
#    else:
#        print "Not a java class :("

    calltip = (name, argspec, string.join(tipList,"\n"))
    return calltip
                                      
def ispython(object):
    """
    Figure out if this is Python code or Java Code

    """
    pyclass = 0
    pycode = 0
    pyinstance = 0
    
    if inspect.isclass(object):
        try:
            object.__doc__
            pyclass = 1
        except AttributeError:
            pyclass = 0

    elif inspect.ismethod(object):
        try:
            object.__dict__
            pycode = 1
        except AttributeError:
            pycode = 0
    else: # I guess an instance of an object falls here
        try:
            object.__dict__
            pyinstance = 1
        except AttributeError:
            pyinstance = 0

#    print "object", object, "pyclass", pyclass, "pycode", pycode, "returning", pyclass | pycode
    
    return pyclass | pycode | pyinstance

