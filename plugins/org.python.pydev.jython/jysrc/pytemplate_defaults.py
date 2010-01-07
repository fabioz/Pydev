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
'''

import template_helper

if False:
    #Variables added externally by the runner of this module.
    py_context_type = org.python.pydev.editor.templates.PyContextType

#===================================================================================================
# GetFile
#===================================================================================================
def GetFile(context, editor):
    return str(editor.getEditorFile()).replace('\\', '/')
        
template_helper.AddTemplateVariable(py_context_type, 'file', 'Full path for file', GetFile)    




#===================================================================================================
# GetModuleName
#===================================================================================================
def GetModuleName(context, editor):
    nature = editor.getPythonNature()
    editor_file = editor.getEditorFile()
    return nature.resolveModule(editor_file)

template_helper.AddTemplateVariable(py_context_type, 'module', 'Current module', GetModuleName)    


#===================================================================================================
# _GetCurrentASTPath
#===================================================================================================
def _GetCurrentASTPath(editor, reverse=False):
    '''
    @return: ArrayList(SimpleNode)
    '''
    from org.python.pydev.parser.fastparser import FastParser
    from org.python.pydev.core.docutils import PySelection
    selection = PySelection(editor)
    ret = FastParser.parseToKnowGloballyAccessiblePath(
        editor.getDocument(), selection.getStartLineIndex())
    if reverse:
        from java.util import Collections
        Collections.reverse(ret)
        
    return ret
    

#===================================================================================================
# GetQualifiedNameScope
#===================================================================================================
def GetQualifiedNameScope(context, editor):
    from org.python.pydev.parser.visitors import NodeUtils
    
    ret = ''
    for stmt in _GetCurrentASTPath(editor):
        if ret:
            ret += '.'
        ret += NodeUtils.getRepresentationString(stmt)
    return ret
        

template_helper.AddTemplateVariable(
    py_context_type, 'current_qualified_scope', 'Current qualified scope.', GetQualifiedNameScope)    



#===================================================================================================
# GetCurrentClass
#===================================================================================================
def GetCurrentClass(context, editor):
    from org.python.pydev.parser.visitors import NodeUtils
    from org.python.pydev.parser.jython.ast import ClassDef
    
    for stmt in _GetCurrentASTPath(editor, True):
        if isinstance(stmt, ClassDef):
            return NodeUtils.getRepresentationString(stmt)
    return ''
        

template_helper.AddTemplateVariable(py_context_type, 'current_class', 'Current class', GetCurrentClass)    



#===================================================================================================
# GetCurrentMethod
#===================================================================================================
def GetCurrentMethod(context, editor):
    from org.python.pydev.parser.visitors import NodeUtils
    from org.python.pydev.parser.jython.ast import FunctionDef
    
    for stmt in _GetCurrentASTPath(editor, True):
        if isinstance(stmt, FunctionDef):
            return NodeUtils.getRepresentationString(stmt)
    return ''

        

template_helper.AddTemplateVariable(py_context_type, 'current_method', 'Current method', GetCurrentMethod)    



#===================================================================================================
# _GetPreviousOrNextClassOrMethod
#===================================================================================================
def _GetPreviousOrNextClassOrMethod(editor, searchForward):
    from org.python.pydev.parser.visitors import NodeUtils
    from org.python.pydev.parser.fastparser import FastParser
    from org.python.pydev.core.docutils import PySelection
    doc = editor.getDocument()
    selection = PySelection(editor)
    startLine = selection.getStartLineIndex()
    
    found = FastParser.firstClassOrFunction(doc, startLine, searchForward)
    if found:
        return NodeUtils.getRepresentationString(found)
    return ''
        
    
    
#===================================================================================================
# GetPreviousClassOrMethod
#===================================================================================================
def GetPreviousClassOrMethod(context, editor):
    return _GetPreviousOrNextClassOrMethod(editor, False)

template_helper.AddTemplateVariable(
    py_context_type, 'prev_class_or_method', 'Previous class or method', GetPreviousClassOrMethod)    
    
#===================================================================================================
# GetNextClassOrMethod
#===================================================================================================
def GetNextClassOrMethod(context, editor):
    return _GetPreviousOrNextClassOrMethod(editor, True)

template_helper.AddTemplateVariable(
    py_context_type, 'next_class_or_method', 'Next class or method', GetNextClassOrMethod)    
        