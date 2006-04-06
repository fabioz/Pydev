'''
This script lists all the bindings linked to Ctrl+2
'''

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

if cmd == 'onCreateActions':
    from org.eclipse.jface.action import Action #@UnresolvedImport
    from org.eclipse.jface.dialogs import MessageDialog #@UnresolvedImport
    from java.lang import String
    from jarray import array
    import traceback
    from org.eclipse.swt.graphics import FontData #@UnresolvedImport
    
    def format(s, size):
        curr = len(s)
        new = []
        for _i in range(size-curr):
            new.append(' ')
        return s + ''.join(new)
    
    class CustomDialog(MessageDialog):
        def createMessageArea(self, composite):
            r = self.super__createMessageArea(composite)
            fontData = FontData("courier", 6, 0)
            self.messageLabel.setFont(editor.getFont(fontData))
            self.messageLabel.getLayoutData().widthHint = 800
            return r
        
    class ListCommand(Action):
        def run(self):
            try:
                offlineActionDescriptions = editor.getOfflineActionDescriptions()
                lines = []
                line = ' - '.join([format('Binding', 8), format('Description', 40), 'Automatically activated?'])
                lines.append(line)
                for actDesc in offlineActionDescriptions:
                    if actDesc.needsEnter:
                        auto = 'No'
                    else:
                        auto = 'Yes'
                    line = ' - '.join([format(actDesc.binding, 8), format(actDesc.description, 40), auto])
                    lines.append(line)
    #            MessageDialog.openInformation(editor.getSite().getShell(), "Keys available", '\n'.join(lines));
                d = CustomDialog(editor.getSite().getShell(), "Keys available", None, '\n'.join(lines), 2, array([String('Ok')], String), 0);
                d.open()
            except:
                traceback.print_exc()
                raise
            
            
    editor.addOfflineActionListener("?", ListCommand(), 'Lists the available commands in Ctrl+2', False) #activate automatically in ?
    editor.addOfflineActionListener("help", ListCommand(), 'Lists the available commands in Ctrl+2', False) #activate automatically in ?
            
