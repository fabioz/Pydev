if False: 
    from org.python.pydev.editor import PyEdit #@UnresolvedImport
    cmd = 'command string'
    editor = PyEdit

#--------------------------------------------------------------- REQUIRED LOCALS
#interface: String indicating which command will be executed
#As this script will be watching the PyEdit (that is the actual editor in Pydev), and this script
#will be listening to it, this string can indicate any of the methods of org.python.pydev.editor.IPyEditListener
assert cmd is not None 

#interface: PyEdit object: this is the actual editor that we will act upon
assert editor is not None
import traceback
import StringIO

if cmd == 'onCreateActions':
    from org.eclipse.jface.action import Action #@UnresolvedImport
    from org.eclipse.jface.dialogs import MessageDialog #@UnresolvedImport
    from org.eclipse.ui.texteditor import IEditorStatusLine #@UnresolvedImport
    
    class SetWrap(Action):
        def run(self):
                    
            try:
                text_widget = editor.getPySourceViewer().getTextWidget()
                if text_widget:
                    setting = text_widget.getWordWrap() == False
        
                    text_widget.setWordWrap(setting);
                    
                    statusLine = editor.getAdapter(IEditorStatusLine)
                    if setting:
                        msg = "Word wrap is on"
                    else:
                        msg = "Word wrap is off"
                        
                    statusLine.setMessage(False, msg, None)


            except:
                s = StringIO.StringIO()
                traceback.print_exc(file=s)
                MessageDialog.openInformation(editor.getSite().getShell(), "Error setting the editor wrap", s.getvalue());
            
    editor.addOfflineActionListener("setwrap", SetWrap(), 'Makes editor show contents wrapped.', True) 
                    
