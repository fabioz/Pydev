import os
import sys
import compiler
from parser import ParserError
from bike.parsing.pathutils import getRootDirectory
from bike.refactor import extractMethod
from bike.refactor.rename import rename
from bike.refactor.extractMethod import coords
from bike.transformer.save import save as saveUpdates
from bike.parsing.utils import fqn_rcar, fqn_rcdr
from bike.parsing import visitor
from bike.transformer.undo import getUndoStack, UndoStackEmptyException
from bike.parsing.fastparserast import getRoot, Class, Function
from bike.query.common import getScopeForLine
from bike.query.getTypeOf import getTypeOfExpr, UnfoundType
from bike.query.findReferences import findReferences
from bike.query.findDefinition import findAllPossibleDefinitionsByCoords
from bike.refactor import inlineVariable, extractVariable, moveToModule
from bike.parsing.load import Cache
from bike import log

def init():
    #context = BRMContext_impl()
    context = BRMContext_wrapper()
    return context

# the context object public interface
class BRMContext(object):


    def save(self):
        """ save the changed files out to disk """

    def setRenameMethodPromptCallback(self, callback):
        """
        sets a callback to ask the user about method refs which brm
        can't deduce the type of. The callback must be callable, and
         take the following parameters:
          - filename
          - linenumber
          - begin column
          - end column
         (begin and end columns enclose the problematic method call)
        """
        
    def renameByCoordinates(self, filename_path, line, col, newname):
        """ an ide friendly method which renames a class/fn/method
        pointed to by the coords and filename"""

    def extract(self, filename_path, 
                begin_line, begin_col,
                end_line, end_col, 
                name):
        """ extracts the region into the named method/function based
        on context"""

    def inlineLocalVariable(self,filename_path, line, col):
        """ Inlines the variable pointed to by
        line:col. (N.B. line:col can also point to a reference to the
        variable as well as the definition) """


    def extractLocalVariable(self,filename_path, begin_line, begin_col,
                             end_line, end_col, variablename):
        """ Extracts the region into a variable """

    def setProgressLogger(self,logger):
        """ Sets the progress logger to an object with a write method
        """
        
    def setWarningLogger(self,logger):
        """ Sets the warning logger to an object with a write method
        """

    def undo(self):
        """ undoes the last refactoring. WARNING: this is dangerous if
        the user has modified files since the last refactoring.
        Raises UndoStackEmptyException"""

    def findReferencesByCoordinates(self, filename_path, line, column):
        """ given the coords of a function, class, method or variable
        returns a generator which finds references to it.
        """

    def findDefinitionByCoordinates(self,filename_path,line,col):        
        """ given the coordates to a reference, tries to find the
        definition of that reference """

    def moveClassToNewModule(self,filename_path, line, 
                             newfilename):
        """ moves the class pointed to by (filename_path, line)
        to a new module """

    def moveFunctionToNewModule(self,filename_path, line, 
                             newfilename):
        """ moves the function pointed to by (filename_path, line)
        to a new module """

        
class NotAPythonModuleOrPackageException: pass
class CouldntLocateASTNodeFromCoordinatesException: pass


# Wrapper to ensure that caches are purged on each request
class BRMContext_wrapper:
    def __init__(self):
        self.brmctx = BRMContext_impl()

    def __getattr__(self,name):
        return BRMContext_callWrapper(self.brmctx,name)


class BRMContext_callWrapper:
    def __init__(self,brmctx,methodname):
        self.name = methodname
        self.brmctx = brmctx

    def __call__(self,*args):
        Cache.instance.reset()
        try:
            return getattr(self.brmctx,self.name)(*args)
        finally:
            Cache.instance.reset()


class BRMContext_impl(BRMContext):
    
    def __init__(self):
        self.ast = getRoot()

        # Used because some refactorings delegate back to the user.
        # this flag ensures that code isnt imported during those times
        self.readyToLoadNewCode = 1 
        self.paths = []
        getUndoStack(1)  # force new undo stack
        if not getRoot().unittestmode:
            log.warning = sys.stderr            
        self.promptUserClientCallback = None

    def _getAST(self):
        return self.ast

    # returns a list of saved filenames
    def save(self):
        savedfiles = saveUpdates()
        return savedfiles

    def setRenameMethodPromptCallback(self, callback):
        self.promptUserClientCallback = callback


    def normalizeFilename(self,filename):
        filename = os.path.expanduser(filename)
        filename = os.path.normpath(os.path.abspath(filename))
        return filename

    def extractMethod(self, filename_path, 
                        begin_line, begin_column, 
                        end_line, end_column, 
                        methodname):
        self.extract(filename_path, begin_line, begin_column,
                     end_line, end_column,methodname)

    def extractFunction(self, filename_path, 
                        begin_line, begin_column, 
                        end_line, end_column, 
                        methodname):
        self.extract(filename_path, begin_line, begin_column,
                     end_line, end_column,methodname)

    # does it based on context
    def extract(self, filename_path, 
                begin_line, begin_col,
                end_line, end_col, 
                name):
        filename_path = self.normalizeFilename(filename_path)
        extractMethod.extractMethod(filename_path,
                                    coords(begin_line, begin_col), 
                                    coords(end_line, end_col), name)

    def inlineLocalVariable(self,filename_path, line, col):
        filename_path = self.normalizeFilename(filename_path)
        inlineVariable.inlineLocalVariable(filename_path,line,col)

    def extractLocalVariable(self,filename_path, begin_line, begin_col,
                             end_line, end_col, variablename):
        filename_path = self.normalizeFilename(filename_path)
        extractVariable.extractLocalVariable(filename_path,
                                             coords(begin_line, begin_col),
                                             coords(end_line, end_col),
                                             variablename)

    def moveClassToNewModule(self,filename_path, line, 
                             newfilename):
        filename_path = self.normalizeFilename(filename_path)
        newfilename = self.normalizeFilename(newfilename)
        moveToModule.moveClassToNewModule(filename_path, line, 
                                       newfilename)

    def moveFunctionToNewModule(self,filename_path, line, 
                             newfilename):
        filename_path = self.normalizeFilename(filename_path)
        newfilename = self.normalizeFilename(newfilename)
        moveToModule.moveFunctionToNewModule(filename_path, line, 
                                       newfilename)

    def undo(self):
        getUndoStack().undo()

    def _promptUser(self, filename, lineno, colbegin, colend):
        return self.promptUserClientCallback(filename, lineno, colbegin, colend)


    # must be an object with a write method
    def setProgressLogger(self,logger):
        log.progress = logger

    # must be an object with a write method
    def setWarningLogger(self,logger):
        log.warning = logger


    # filename_path must be absolute
    def renameByCoordinates(self, filename_path, line, col, newname):
        filename_path = self.normalizeFilename(filename_path)
        Cache.instance.reset()
        try:
            self._setNonLibPythonPath(filename_path)
            rename(filename_path,line,col,newname,
                   self.promptUserClientCallback)
        finally:
            Cache.instance.reset()

    def _reverseCoordsIfWrongWayRound(self, colbegin, colend):
        if(colbegin > colend):
            colbegin,colend = colend,colbegin
        return colbegin,colend


    def findDefinitionByCoordinates(self,filename_path,line,col):
        filename_path = self.normalizeFilename(filename_path)
        self._setCompletePythonPath(filename_path)
        return findAllPossibleDefinitionsByCoords(filename_path,line,col)

        
    # filename_path must be absolute
    def findReferencesByCoordinates(self, filename_path, line, column):
        filename_path = self.normalizeFilename(filename_path)
        self._setNonLibPythonPath(filename_path)
        return findReferences(filename_path,line,column)
        
    def refreshASTFromFileSystem(self):
        for path in self.paths:
            self.ast = loadast(path, self.ast)

    def _setCompletePythonPath(self,filename):
        pythonpath = [] + sys.path  # make a copy
        self.ast.pythonpath = pythonpath
        
    def _setNonLibPythonPath(self,filename):
        if getRoot().unittestmode:
            return
        pythonpath = self._removeLibdirsFromPath(sys.path)
        pythonpath = [os.path.abspath(p) for p in pythonpath]
        self.ast.pythonpath = pythonpath

    def _getCurrentSearchPath(self):
        return self.ast.pythonpath
    
    def _removeLibdirsFromPath(self, pythonpath):
        libdir = os.path.join(sys.prefix,"lib").lower()
        pythonpath = [p for p in pythonpath
                      if not p.lower().startswith(libdir)]
        return pythonpath

        
def _deducePackageOfFile(filename):
    package = ""
    dot = ""
    dir = os.path.dirname(filename)
    while dir != ""and \
          os.path.exists(os.path.join(dir, "__init__.py")):
        dir, dirname = os.path.split(dir)
        package = dirname+dot+package
        dot = "."
    return package
