#! /usr/bin/env python
import sys
import traceback
import inspect
import os
import parser
import symbol
import token
import types
import string

from types import ListType, TupleType

def get_docs( source , basename ):
    """Retrieve information from the parse tree of a source file.

    source
        source code to parse.
    """
    ast = parser.suite(source)
    return ModuleInfo(ast.totuple(1), basename)


class SuiteInfoBase:
    _docstring = ''
    _name = ''

    def __init__(self, tree = None):
        self._class_info = {}
        self._function_info = {}
        if tree:
            self._extract_info(tree)

    def _extract_info(self, tree):
        # extract docstring
        if len(tree) == 2:
            found, vars = match(DOCSTRING_STMT_PATTERN[1], tree[1])
        else:
            found, vars = match(DOCSTRING_STMT_PATTERN, tree[3])
        if found:
            self._docstring = eval(vars['docstring'])
        # discover inner definitions
        for node in tree[1:]:
            found, vars = match(COMPOUND_STMT_PATTERN, node)
            if found:
                cstmt = vars['compound']
                if cstmt[0] == symbol.funcdef:
                    name = cstmt[2][1]
                    self._function_info[name] = FunctionInfo(cstmt)
                elif cstmt[0] == symbol.classdef:
                    name = cstmt[2][1]
                    self._class_info[name] = ClassInfo(cstmt)

    def get_docstring(self):
        return self._docstring

    def get_name(self):
        return self._name

    def get_class_names(self):
        return self._class_info.keys()

    def get_class_info(self, name):
        return self._class_info[name]

    def __getitem__(self, name):
        try:
            return self._class_info[name]
        except KeyError:
            return self._function_info[name]


class SuiteFuncInfo:
    #  Mixin class providing access to function names and info.

    def get_function_names(self):
        return self._function_info.keys()

    def get_function_info(self, name):
        return self._function_info[name]


class FunctionInfo(SuiteInfoBase, SuiteFuncInfo):
    def __init__(self, tree = None):
        self._name = tree[2][1]
        self._lineNo = tree[2][2]
        self._args = []
        self.argListInfos( tree[3] )
        SuiteInfoBase.__init__(self, tree and tree[-1] or None)

    def get_DeclareLineNo( self ):
        return self._lineNo
        
    def argListInfos( self , tree ):
      argType = tree[0]
      if argType == symbol.parameters:
        self.argListInfos(tree[2])
      elif argType == symbol.varargslist:
        for arg in tree[1:]:
          self.argListInfos(arg)
      elif argType == symbol.fpdef:
        self._args.append(tree[1])

class ClassInfo(SuiteInfoBase):
    def __init__(self, tree = None):
        self._name = tree[2][1]
        self._lineNo = tree[2][2]
        SuiteInfoBase.__init__(self, tree and tree[-1] or None)

    def get_method_names(self):
        return self._function_info.keys()

    def get_method_info(self, name):
        return self._function_info[name]

    def get_DeclareLineNo():
        return self._lineNo 

class ModuleInfo(SuiteInfoBase, SuiteFuncInfo):
    def __init__(self, tree = None, name = "<string>"):
        self._name = name
        SuiteInfoBase.__init__(self, tree)
        if tree:
            found, vars = match(DOCSTRING_STMT_PATTERN, tree[1])
            if found:
                self._docstring = vars["docstring"]


def match(pattern, data, vars=None):
    """Match `data' to `pattern', with variable extraction.

    pattern
        Pattern to match against, possibly containing variables.

    data
        Data to be checked and against which variables are extracted.

    vars
        Dictionary of variables which have already been found.  If not
        provided, an empty dictionary is created.

    The `pattern' value may contain variables of the form ['varname'] which
    are allowed to match anything.  The value that is matched is returned as
    part of a dictionary which maps 'varname' to the matched value.  'varname'
    is not required to be a string object, but using strings makes patterns
    and the code which uses them more readable.

    This function returns two values: a boolean indicating whether a match
    was found and a dictionary mapping variable names to their associated
    values.
    """
    if vars is None:
        vars = {}
    if type(pattern) is ListType:       # 'variables' are ['varname']
        vars[pattern[0]] = data
        return 1, vars
    if type(pattern) is not TupleType:
        return (pattern == data), vars
    if len(data) != len(pattern):
        return 0, vars
    for pattern, data in map(None, pattern, data):
        same, vars = match(pattern, data, vars)
        if not same:
            break
    return same, vars


#  This pattern identifies compound statements, allowing them to be readily
#  differentiated from simple statements.
#
COMPOUND_STMT_PATTERN = (
    symbol.stmt,
    (symbol.compound_stmt, ['compound'])
    )


#  This pattern will match a 'stmt' node which *might* represent a docstring;
#  docstrings require that the statement which provides the docstring be the
#  first statement in the class or function, which this pattern does not check.
#
DOCSTRING_STMT_PATTERN = (
    symbol.stmt,
    (symbol.simple_stmt,
     (symbol.small_stmt,
      (symbol.expr_stmt,
       (symbol.testlist,
        (symbol.test,
         (symbol.and_test,
          (symbol.not_test,
           (symbol.comparison,
            (symbol.expr,
             (symbol.xor_expr,
              (symbol.and_expr,
               (symbol.shift_expr,
                (symbol.arith_expr,
                 (symbol.term,
                  (symbol.factor,
                   (symbol.power,
                    (symbol.atom,
                     (token.STRING, ['docstring'])
                     )))))))))))))))),
     (token.NEWLINE, '')
     ))

class jpyutils :
        
  def parsedReturned( self , 
                      command = 'COMMAND' , 
                      argument = None , 
                      message = None , 
                      details = None ):
    parsedCommand = []
    parsedCommand.append(command)
    parsedCommand.append(argument)
    parsedCommand.append(message)
    parsedCommand.append(details)
    return parsedCommand

  def populateCMDException( self , arg , oldstd ):
    "global utility exception reporter for all pydbg classes"
    sys.stdout=oldstd
    tb , exctype , value = sys.exc_info()
    excTrace = traceback.format_exception( tb , exctype , value )
    tb = None # release
    return self.parsedReturned( argument = arg ,
                                message = "Error on CMD" ,
                                details = excTrace
                              )  
  def removeForXml( self , strElem , keepLinefeed = 0 ):
    "replace unsuported xml encoding characters"
    if (not  keepLinefeed ):       
      strElem = string.replace(strElem,'\n','')
    strElem = string.replace(strElem,'"',"&quot;")
    strElem = string.replace(strElem,'<','&lt;')
    strElem = string.replace(strElem,'>','&gt;')
    return strElem
    

class xmlizer:
  "xmlize a provided syntax error or parsed module infos"
  
  def __init__(self , infos , baseName , fName , destFName , error = None ):
    self.Infos = infos
    self.Error = error
    self.Utils = jpyutils()
    self.FileName = fName 
    self.DestFName = destFName
    self.BaseName = baseName

  def populate_error( self ):
    LINESTR = 'line '
    reason=self.Error[len(self.Error)-1]
    lower =self.Error[2].find(LINESTR)+len(LINESTR)
    higher=self.Error[2].find(',',lower)
    lineNo=self.Error[2][lower:]
    self.Dest.write('<error  fileid="'+ \
                     self.Utils.removeForXml(self.FileName)+ \
                     '" reason="' + \
                     reason + \
                     '" lineno="'+lineNo+'" />' )
    
  def populate_class_infos( self , className ):
    classInfo = self.Infos.get_class_info(className)
    self.Dest.write('<class  name="'+ \
                     self.Utils.removeForXml(className)+ \
                     '" doc="' + \
                     self.Utils.removeForXml(classInfo.get_docstring()) + \
                     '" lineno="'+ str(classInfo._lineNo) +'" >\n' )
    # gather infos about class methods
    methods = classInfo.get_method_names()
    for methodName in methods:
      method = classInfo.get_method_info(methodName)
      self.populate_method_infos(method,methodName)
    self.Dest.write('</class>\n' )
  
  def populate_function_arguments_infos( self , args ):
    for arg in args:
      self.Dest.write('  <argument name="' + \
                      arg[1] + '" lineno="' +\
                      str(arg[2]) + '"/>\n')
                      
  def populate_method_infos( self , method , methodName):
    self.Dest.write('<function  name="'+ \
                     self.Utils.removeForXml(methodName)+ \
                     '" doc="' + \
                     self.Utils.removeForXml(method.get_docstring()) + \
                     '" lineno="'+ str(method._lineNo) +'" >\n' )
    if (len(method._args) != 0):
      self.populate_function_arguments_infos(method._args)
    self.Dest.write("</function>\n")                  
    
  def populate_function_infos( self , functionName ):
    functionInfo = self.Infos.get_function_info(functionName)
    self.populate_method_infos(functionInfo,functionName)
    
  def populate_tree( self ):
    self.Dest.write('<module name="'+self.BaseName+'">\n')
    classes = self.Infos.get_class_names()
    for curClass in classes:
      self.populate_class_infos(curClass)
    functions = self.Infos.get_function_names()
    for curFunction in functions:
      self.populate_function_infos(curFunction)
    self.Dest.write("</module>\n")                  
    
  def populate( self ):
    self.Dest = file( self.DestFName , 'w+' )
    self.Dest.write( '<?xml version="1.0"?>\n')
    if self.Error != None:
      self.populate_error()
    else:
      self.populate_tree()
    self.Dest.close()
    
class commander :
  "python source file inspector/introspector"

  def __init__(self , source , dest ):
    "constructor providing python source to inspect"
    self.SourceFile = source
    self.DestFile = dest
    self.Infos = None
    self.BaseName = None
    self.Errors = None
  
  def check(self):    
    "make a syntaxic control of provided source "
    # execute requested dynamic command on this side
    fp = open(self.SourceFile,"r")
    sourceStr = fp.read() + "\n"
    try:
      # extract python module name
      self.BaseName = os.path.basename(os.path.splitext(self.SourceFile)[0])
      self.Code = compile( sourceStr , self.SourceFile , "exec" )
      self.Infos = get_docs( sourceStr , self.BaseName )
    except:
      tb , exctype , value = sys.exc_info()
      self.Errors = traceback.format_exception(tb,exctype,value)
      pass
    xml = xmlizer(self.Infos , self.BaseName , self.SourceFile , self.DestFile , self.Errors )
    xml.populate()
    
# 
# inspector launcher entry point
#
if __name__ == "__main__":
  # inspect jpydaemon itself      
  print "args = " , sys.argv
  
  if ( len(sys.argv) > 1 ): source = sys.argv[1]
  if ( len(sys.argv) > 2 ): dest   = sys.argv[2]
  if ( len(sys.argv) < 3 ): sys.exit(12) # missing arguments 
  instance = commander( source , dest)
  instance.check()
  pass

