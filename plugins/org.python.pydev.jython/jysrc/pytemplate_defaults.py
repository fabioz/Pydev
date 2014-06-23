'''
This module contains template variables (added through the templates engine).

Users should be able to create their own templates by adding an additional path in the scripts
and creating any file that starts with "pytemplate" within that directory.

E.g.: clients could get all the methods in the class and use them to complete the template, make
custom date formatting, etc (only limited by your imagination as any class from Pydev could be
imported and used here).

The only annoyance is that changes aren't automatically applied, so, one must (in a Pydev Editor) use:

Ctrl + 2 + --clear-templates-cache

to clear the cache so that any changed files regarding the templates are (re)evaluated (the only
other way to get the changes applied is restarting eclipse).

The concept is the same as the default scripting engine in pydev. The only difference is that it'll
only get files starting with 'pytemplate', so, it's also worth checking
http://pydev.org/manual_articles_scripting.html

context passed as parameter: org.python.pydev.editor.codecompletion.templates.PyDocumentTemplateContext
'''

import time
import template_helper

if False:
    #Variables added externally by the runner of this module.
    py_context_type = org.python.pydev.editor.templates.PyContextType  # @UndefinedVariable


#===================================================================================================
# _CreateSelection
#===================================================================================================
def _CreateSelection(context):
    '''
    Created method so that it can be mocked on tests.
    '''
    selection = context.createPySelection()
    return selection


#===================================================================================================
# GetFile
#===================================================================================================
def GetFile(context):
    return str(context.getEditorFile()).replace('\\', '/')

template_helper.AddTemplateVariable(py_context_type, 'file', 'Full path for file', GetFile)


#===================================================================================================
# _IsGrammar3
#===================================================================================================
def _IsGrammar3(context):
    if context is None:
        return False  #Default is Python 2
    from org.python.pydev.core import IGrammarVersionProvider
    if context.getGrammarVersion() >= IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0:
        return True
    return False

#===================================================================================================
# GetSpaceIfPy2
#===================================================================================================
def GetSpaceIfPy2(context):
    if _IsGrammar3(context):
        return ''

    #if not 3, it's 2
    return ' '

template_helper.AddTemplateVariable(py_context_type, 'space_if_py2', 'Adds a space if python 2.', GetSpaceIfPy2)


#===================================================================================================
# GetRParenIfPy3
#===================================================================================================
def GetRParenIfPy3(context):
    if _IsGrammar3(context):
        return ')'

    return ''

template_helper.AddTemplateVariable(py_context_type, 'rparen_if_py3', 'Adds a ) if python 3.', GetRParenIfPy3)

#===================================================================================================
# GetLParenIfPy3
#===================================================================================================
def GetLParenIfPy3(context):
    if _IsGrammar3(context):
        return '('

    return ''

template_helper.AddTemplateVariable(py_context_type, 'lparen_if_py3', 'Adds a ( if python 3.', GetLParenIfPy3)


#===============================================================================
# ISO-8601 Dates
#===============================================================================
def GetISODate(context):
    return time.strftime("%Y-%m-%d")

template_helper.AddTemplateVariable(py_context_type, 'isodate', 'ISO-8601 Ymd date', GetISODate)

def GetISODateString1(context):
    return time.strftime("%Y-%m-%d %H:%M")

template_helper.AddTemplateVariable(py_context_type, 'isodatestr', 'ISO-8601 Ymd HM date', GetISODateString1)

def GetISODateString2(context):
    return time.strftime("%Y-%m-%d %H:%M:%S")

template_helper.AddTemplateVariable(py_context_type, 'isodatestr2', 'ISO-8601 Ymd HMS date', GetISODateString2)


#===================================================================================================
# GetModuleName
#===================================================================================================
def GetModuleName(context):
    return context.getModuleName()

template_helper.AddTemplateVariable(py_context_type, 'module', 'Current module', GetModuleName)


#===================================================================================================
# _GetCurrentASTPath
#===================================================================================================
def _GetCurrentASTPath(context, reverse=False):
    '''
    @return: ArrayList(SimpleNode)
    '''
    FastParser = context.getFastParserClass()  # from org.python.pydev.parser.fastparser import FastParser
    selection = _CreateSelection(context)
    ret = FastParser.parseToKnowGloballyAccessiblePath(
        context.getDocument(), selection.getStartLineIndex())
    if reverse:
        from java.util import Collections  # @UnresolvedImport
        Collections.reverse(ret)

    return ret


#===================================================================================================
# GetQualifiedNameScope
#===================================================================================================
def GetQualifiedNameScope(context):
    NodeUtils = context.getNodeUtilsClass()  # from org.python.pydev.parser.visitors import NodeUtils

    ret = ''
    for stmt in _GetCurrentASTPath(context):
        if ret:
            ret += '.'
        ret += NodeUtils.getRepresentationString(stmt)
    return ret


template_helper.AddTemplateVariable(
    py_context_type, 'current_qualified_scope', 'Current qualified scope.', GetQualifiedNameScope)



#===================================================================================================
# _GetCurrentClassStmt
#===================================================================================================
def _GetCurrentClassStmt(context):
    NodeUtils = context.getNodeUtilsClass()  #from org.python.pydev.parser.visitors import NodeUtils
    ClassDef = context.getClassDefClass()  # from org.python.pydev.parser.jython.ast import ClassDef

    for stmt in _GetCurrentASTPath(context, True):
        if isinstance(stmt, ClassDef):
            return stmt
    return None


#===================================================================================================
# GetCurrentClass
#===================================================================================================
def GetCurrentClass(context):
    NodeUtils = context.getNodeUtilsClass()  #from org.python.pydev.parser.visitors import NodeUtils
    ClassDef = context.getClassDefClass()  # from org.python.pydev.parser.jython.ast import ClassDef

    stmt = _GetCurrentClassStmt(context)
    if stmt is not None:
        return NodeUtils.getRepresentationString(stmt)

    return ''


template_helper.AddTemplateVariable(py_context_type, 'current_class', 'Current class', GetCurrentClass)


#===================================================================================================
# GetPydevdFileLocation
#===================================================================================================
def GetPydevdFileLocation(context):
    from org.python.pydev.debug.ui.launching import PythonRunnerConfig  # @UnresolvedImport
    return PythonRunnerConfig.getDebugScript()

template_helper.AddTemplateVariable(
    py_context_type, 'pydevd_file_location', 'pydevd.py File Location', GetPydevdFileLocation)

#===================================================================================================
# GetPydevdDirLocation
#===================================================================================================
def GetPydevdDirLocation(context):
    from org.python.pydev.debug.ui.launching import PythonRunnerConfig  # @UnresolvedImport
    import os
    return os.path.split(PythonRunnerConfig.getDebugScript())[0]

template_helper.AddTemplateVariable(
    py_context_type, 'pydevd_dir_location', 'pydevd.py Directory Location', GetPydevdDirLocation)



#===================================================================================================
# GetCurrentMethod
#===================================================================================================
def GetCurrentMethod(context):
    NodeUtils = context.getNodeUtilsClass()  #from org.python.pydev.parser.visitors import NodeUtils
    FunctionDef = context.getFunctionDefClass()  # from org.python.pydev.parser.jython.ast import FunctionDef

    for stmt in _GetCurrentASTPath(context, True):
        if isinstance(stmt, FunctionDef):
            return NodeUtils.getRepresentationString(stmt)
    return ''



template_helper.AddTemplateVariable(py_context_type, 'current_method', 'Current method', GetCurrentMethod)


#===================================================================================================
# _GetPreviousOrNextClassOrMethod
#===================================================================================================
def _GetPreviousOrNextClassOrMethod(context, searchForward):
    NodeUtils = context.getNodeUtilsClass()  #from org.python.pydev.parser.visitors import NodeUtils
    FastParser = context.getFastParserClass()  #from org.python.pydev.parser.fastparser import FastParser
    doc = context.getDocument()
    selection = _CreateSelection(context)
    startLine = selection.getStartLineIndex()

    found = FastParser.firstClassOrFunction(doc, startLine, searchForward, context.isCythonFile())
    if found:
        return NodeUtils.getRepresentationString(found)
    return ''



#===================================================================================================
# GetPreviousClassOrMethod
#===================================================================================================
def GetPreviousClassOrMethod(context):
    return _GetPreviousOrNextClassOrMethod(context, False)

template_helper.AddTemplateVariable(
    py_context_type, 'prev_class_or_method', 'Previous class or method', GetPreviousClassOrMethod)

#===================================================================================================
# GetNextClassOrMethod
#===================================================================================================
def GetNextClassOrMethod(context):
    return _GetPreviousOrNextClassOrMethod(context, True)

template_helper.AddTemplateVariable(
    py_context_type, 'next_class_or_method', 'Next class or method', GetNextClassOrMethod)



#===================================================================================================
# GetSuperclass
#===================================================================================================
def GetSuperclass(context):
    selection = _CreateSelection(context)
    stmt = _GetCurrentClassStmt(context)
    BadLocationException = context.getBadLocationExceptionClass()  # from org.eclipse.jface.text import BadLocationException
    if stmt is not None:
        doc = context.getDocument()
        name = stmt.name
        nameStartOffset = selection.getAbsoluteCursorOffset(name.beginLine - 1, name.beginColumn - 1)
        nameStartOffset += len(name.id)

        found_start = False
        i = 0
        contents = ''
        while True:
            try:
                c = doc.get(nameStartOffset + i, 1)
                i += 1

                if c == '(':
                    found_start = True

                elif c in (')', ':'):
                    break

                elif c in ('\r', '\n', ' ', '\t'):
                    pass

                elif c == '#':  #skip comments
                    while c not in ('\r', '\n'):
                        c = doc.get(nameStartOffset + i, 1)
                        i += 1


                else:
                    if found_start:
                        contents += c

            except BadLocationException:
                return ''  #Seems the class declaration is not properly finished as we're now out of bounds in the doc.

        if ',' in contents:
            ret = []
            for c in contents.split(','):
                ret.append(c.strip())
            return ret

        return contents.strip()

    return ''

template_helper.AddTemplateVariable(
    py_context_type, 'superclass', 'Superclass of the current class', GetSuperclass)
