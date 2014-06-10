
if False:
    from org.python.pydev.editor import PyEdit #@UnresolvedImport
    cmd = 'command string'
    editor = PyEdit
    systemGlobals = {}

#--------------------------------------------------------------- REQUIRED LOCALS
#interface: String indicating which command will be executed
#As this script will be watching the PyEdit (that is the actual editor in Pydev), and this script
#will be listening to it, this string can indicate any of the methods of org.python.pydev.editor.IPyEditListener
assert cmd is not None

#interface: PyEdit object: this is the actual editor that we will act upon
assert editor is not None

if cmd == 'onCreateActions':
    
    SetWrap = systemGlobals.get('SetWrap')
    if SetWrap is None:
        Action = editor.getActionClass() #from org.eclipse.jface.action import Action #@UnresolvedImport
    
        class SetWrap(Action):
            
            def __init__(self, editor):
                self.editor = editor
            
            def run(self):
                editor = self.editor
                try:
                    text_widget = editor.getPySourceViewer().getTextWidget()
                    if text_widget:
                        setting = text_widget.getWordWrap() == False
    
                        text_widget.setWordWrap(setting);
    
                        if setting:
                            msg = "Word wrap is on"
                        else:
                            msg = "Word wrap is off"
    
                        editor.setMessage(False, msg)
                except:
                    import traceback
                    import StringIO
                    s = StringIO.StringIO()
                    traceback.print_exc(file=s)
                    editor.showInformationDialog("Error setting the editor wrap", s.getvalue());
                    
            systemGlobals['SetWrap'] = SetWrap

    editor.addOfflineActionListener("setwrap", SetWrap(editor), 'Makes editor show contents wrapped.', True)

