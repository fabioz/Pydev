from org.python.pydev.editor.templates import PyTemplateVariableResolver

#===================================================================================================
# CallableTemplateVariableResolver
#===================================================================================================
class CallableTemplateVariableResolver(PyTemplateVariableResolver):
    
    
    def __init__(self, variable, description, callable):
        PyTemplateVariableResolver.__init__(self, variable, description)
        self._callable = callable
        
        
    def getEdit(self, context):
        if context.viewer is not None and hasattr(context.viewer, 'getEdit'):
            editor = context.viewer.getEdit()
            return editor
        
        return None
    
        
    def asList(self, v):
        if type(v) != type([]):
            v = [v]
        return v
    
    def resolveAll(self, context):
        ret = 'Unable to evaluate context. Invalid source viewer: '+str(context.viewer)
        
        editor = self.getEdit(context)
        if editor is not None:
            ret = self._callable(context, editor)
        
        #This means we don't have a viewer with a PyEdit.
        return self.asList(ret)


#===================================================================================================
# AddTemplateVariable
#===================================================================================================
def AddTemplateVariable(py_context_type, variable, description, evaluate_callback):
    '''
    @param py_context_type: org.python.pydev.editor.templates.PyContextType
        This is the context type where the variable should be added.
    
    @param variable: str
        The variable we're adding so that when the user uses ${variable} it will resolve it.
    
    @param description: str
        Description for the variable
    
    @param evaluate_callback: callable(context, editor)->str
        Where context is org.python.pydev.editor.codecompletion.templates.PyDocumentTemplateContext,
        editor is org.python.pydev.editor.PyEdit
        and it should return a string that will be put in the document.
    '''
    py_context_type.addResolver(
        CallableTemplateVariableResolver(variable, description, evaluate_callback))