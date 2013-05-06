from __future__ import nested_scopes # for Jython 2.1 compatibility

'''
In this example we bind a simple action, that when run will open a dialog to the user.

The action is binded to the 'OfflineActions' in the PyEdit... The 'offline actions' are those that are
started with Ctrl+2 (yeah, I know the name is nonsense) and after that the user will type what he wants.

In this case, the user will have to press 'Ctrl+2' then 'ex2' then <ENTER> to activate it. It should be clear
in the example below on how to bind any other action (subclass of the IAction interface) to any other text 
entered after Ctrl+2.
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
    Action = editor.getActionClass()

    class ExampleCommand2(Action):
        def run(self):
            editor.showInformationDialog("Example2", "Activated!!");


    editor.addOfflineActionListener("ex2", ExampleCommand2(), 'Example on how to bind script action', True) #the user can activate this action with: Ctrl+2  ex2<ENTER>

