'''
This is the first example on how to program with Jython within Pydev.

There is a 'protocol' that has to be followed when making scripts and some peculiarities. Let's see how it works...

1. The objects that we can act upon will be set from the outside of the plugin (in the java code). As this is
'arbitrary' for any action, each script should make clear which are its 'required' locals

2. We need to 'fit' the framework that Eclipse gives us. In this case, we have to make sure the action is an IAction
(so we subclass the Action)

3. Register it to the Eclipse framework. In this case, we will bind Ctrl+. to some action we define.

NOTE: between one call and the other to this script, all the globals we set before will remain. Only the passed
arguments will be changed before calling it again

NOTE2: This code is 'compiled' at runtime, and the file timestamp will be cached with the code, so, if it changes,
it will be automatically reloaded (no need to restart eclipse for that).

Java-Side inner workings:
- Refer to: org.python.pydev.editor.scripting.PyEditScripting (this is the class that binds any script that starts with pyedit to the PyEdit editor)
- and   to: org.python.pydev.jython.JythonPlugin (this is the class that makes the actual call to the jython code).

Some IMPORTANT implementation notes:
- The same interpreter will be used for all the scripts within a PyEdit, so, be careful on what is left in the namespace...

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

#--------------------------------------------------------------- ACTION DEFINITION
if cmd == 'onCreateActions':
    from org.eclipse.swt import SWT #@UnresolvedImport
    from org.eclipse.jface.action import Action #@UnresolvedImport
    from org.python.pydev.core.docutils import PySelection #@UnresolvedImport
    from org.eclipse.core.resources import IMarker #@UnresolvedImport
    from org.eclipse.core.resources import IResource #@UnresolvedImport

    FIND_NEXT_PROBLEM_ACTION_ID = "org.python.pydev.core.script.pyedit_find_next_problem"
    
    def cmpMarkers(a,b):
        '''Helper function to compare markers through its location (starting character)
        '''
        
        return cmp(a.getAttribute(IMarker.CHAR_START), b.getAttribute(IMarker.CHAR_START))
        
    class FindNextProblemAction(Action):
        '''This class defines an Action that goes to the next problem
        '''
        
        def getAccelerator(self):
            return SWT.CTRL|'.'
        
        def getStartAndEnd(self, marker):
            '''Helper to get the char start and end of the marker
            '''
            
            charStart = marker.getAttribute(IMarker.CHAR_START)
            charEnd = marker.getAttribute(IMarker.CHAR_END)
            return charStart, charEnd
        
        def run(self):
            '''Makes the run when activated. In this case, we get all the markers linked to the resource
            and check which one is the 'next', based on the current cursor position
            '''
            
            selection = PySelection(editor)
            absoluteCursorOffset = selection.getAbsoluteCursorOffset()
            resource = editor.getIFile()
            if not resource:
                return
            
            markers = resource.findMarkers(IMarker.PROBLEM, True, IResource.DEPTH_ZERO)
            markers = [m for m in markers] #we have to make the array a list so that we can easily sort it
            markers.sort(cmpMarkers)
    
            #find the next marker to reveal
            for marker in markers:
                charStart, charEnd = self.getStartAndEnd(marker)
                if charStart is not None and charStart > absoluteCursorOffset:
                    editor.selectAndReveal(charStart, charEnd - charStart)
                    return
                
            #ok, if it got here, it didn't find any that was greater (so, we have to 'wrap' around)
            #and go to the first one found
            for marker in markers:
                charStart, charEnd = self.getStartAndEnd(marker)
                if charStart is not None: #same thing, but return on first not None
                    editor.selectAndReveal(charStart, charEnd - charStart)
                    return
    
    #bind the action to some internal definition
    editor.setAction(FIND_NEXT_PROBLEM_ACTION_ID, FindNextProblemAction()) 
    #bind the action to some code
    
    #NOTE: this is not the standard way to do it... The correct way would be creating a command in the plugin.xml
    #then make a keybinding for that command, and associate the command to the action
    
    #with this implementation, the user is not able to change this keybinding.
    editor.setActionActivationCode(FIND_NEXT_PROBLEM_ACTION_ID, '.', -1, SWT.CTRL); #will be activated on Ctrl+.
    
    
    
    
    
    
    
    
    
    
    
    
    
    