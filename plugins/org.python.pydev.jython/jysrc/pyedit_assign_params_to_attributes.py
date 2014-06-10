"""Assign Params to Attributes by Joel Hedlund <joel.hedlund at gmail.com>.

PyDev script for generating python code that assigns method parameter 
values to attributes of self with the same name. Activates with 'a' by 
default. Edit global constants ACTIVATION_STRING and WAIT_FOR_ENTER if this
does not suit your needs. See docs on the class AssignToAttribsOfSelf for 
more details.

Contact the author for bug reports/feature requests.

Changed:Fabio Zadrozny (binded to Ctrl+1 too)
"""

__version__ = "1.0.1"

__copyright__ = """Available under the same conditions as PyDev.

See PyDev license for details.
http://pydev.sourceforge.net

"""

# Change this if the default does not suit your needs
ACTIVATION_STRING = 'a'
WAIT_FOR_ENTER = False


# Set to True to force Jython script interpreter restart on save events.
# Useful for Jython PyDev script development, not useful otherwise.
DEBUG = False

# This is a magic trick that tells the PyDev Extensions editor about the
# namespace provided for pydev scripts:
if False:
    from org.python.pydev.editor import PyEdit  #@UnresolvedImport
    cmd = 'command string'
    editor = PyEdit
    systemGlobals = {}


if DEBUG and cmd == 'onSave':
    from org.python.pydev.jython import JythonPlugin  #@UnresolvedImport
    editor.pyEditScripting.interpreter = JythonPlugin.newPythonInterpreter()
    cmd = 'onCreateActions'  #Force it to recreate stuff.


#=======================================================================================================================
# Actually bind the actions
#=======================================================================================================================
if cmd == 'onCreateActions':
    
    created_class = False
    
    assign_params_to_attributes_action = systemGlobals.get('assign_params_to_attributes_action')
    if assign_params_to_attributes_action is None:
        import assign_params_to_attributes_action
        systemGlobals['assign_params_to_attributes_action'] = assign_params_to_attributes_action
        
    AssignToAttribsOfSelfAction = systemGlobals.get('AssignToAttribsOfSelfAction')
    if AssignToAttribsOfSelfAction is None:
        created_class = True
        Action = editor.getActionClass()  #from org.eclipse.jface.action import Action #@UnresolvedImport
        
        #=======================================================================================================================
        # AssignToAttribsOfSelfAction
        #=======================================================================================================================
        class AssignToAttribsOfSelfAction(Action):
        
            def __init__(self, assign_to_attribs_helper):
                Action.__init__(self)
                self.assign_to_attribs_helper = assign_to_attribs_helper
        
            def run(self):
                self.assign_to_attribs_helper.run()
        systemGlobals['AssignToAttribsOfSelfAction'] = AssignToAttribsOfSelfAction
        
        
    #---------------------------------------------------------------------------------------------- Bind it to Ctrl+2, a
    sDescription = 'Assign method params to attribs of self'
    assign_to_attribs_helper = assign_params_to_attributes_action.AssignToAttribsOfSelf(editor)
    editor.addOfflineActionListener(
        ACTIVATION_STRING, AssignToAttribsOfSelfAction(assign_to_attribs_helper), sDescription, WAIT_FOR_ENTER)

    #------------------------------------------------------------------------------------------------- Bind it to Ctrl+1
    if created_class:
        #This has to be done only once when the class is created.
        PythonCorrectionProcessor = editor.getPythonCorrectionProcessorClass()
        ASSIGN_PARAMS_TO_ATTRIBUTES_ASSIST = 'ASSIGN_PARAMS_TO_ATTRIBUTES_ASSIST'
        if not PythonCorrectionProcessor.hasAdditionalAssist(ASSIGN_PARAMS_TO_ATTRIBUTES_ASSIST):
            import assign_params_to_attributes_assist
            assist = assign_params_to_attributes_assist.AssistAssignParamsToAttributes()
            PythonCorrectionProcessor.addAdditionalAssist(ASSIGN_PARAMS_TO_ATTRIBUTES_ASSIST, assist)



