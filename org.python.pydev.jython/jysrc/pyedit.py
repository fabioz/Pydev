'''
This is the first example on how to program with Jython within Pydev.

There is a 'protocol' that has to be followed when making scripts and some peculiarities. Let's see how it works...

1. The objects that we can act upon will be set from the outside of the plugin (in the java code). As this is
'arbitrary' for any action, each script should make clear which are its 'required' locals


'''
import org.eclipse
import org.eclipse.swt.SWT
print org.eclipse.swt.SWT

#--------------------------------------------------------------- REQUIRED LOCALS
#interface: String indicating which command will be executed
#As this script will be watching the PyEdit (that is the actual editor in Pydev), and this script
#will be listening to it, this string can indicate any of the methods of org.python.pydev.editor.IPyEditListener
assert cmd is not None 

#interface: PyEdit object: this is the actual editor that we will act upon
assert editor is not None

print cmd
print editor


EXAMPLE_ACTION_ID = "org.python.pydev.core.script.pyedit_example"

class ExampleAction:
    def getAccelerator(self):
        return 
#    public int getAccelerator() {
#        return SWT.CTRL|'\r';
#    }
#
#    public String getText() {
#        return "Evaluate Python Code in Console";
#    }
#    
#    public  void run(){
#        PySelection selection = new PySelection(edit);
#        String code = selection.getTextSelection().getText();
#        getConsoleEnv(edit.getProject(), edit).execute(code);
#    }

#edit.setAction(EXAMPLE_ACTION_ID, ExampleAction()) 
#edit.setActionActivationCode(EXAMPLE_ACTION_ID, 't', -1, SWT.CTRL); #will be activated on Ctrl+t
