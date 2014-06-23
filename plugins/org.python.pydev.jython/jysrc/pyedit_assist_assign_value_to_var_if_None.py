"""Quick Assistant: Assign empty dictionary to variable if None.

Effect
======
Generates code that tests if a variable is None, and if so, assigns an 
empty dictionary to it.

Valid when
==========
When the current line contains exactly one alphanumeric word. This script 
does not check if the word is defined or valid in any other way. 

Installation
============
Place this file in your pydev jython script dir, along with 
assist_proposal.py and assist_regex_based_proposal.py, open a new editor,
and you are ready to go. See the pydev docs if you don't know where your 
dir is.

Use case
========
It's generally a bad idea to use mutable objects as default values to 
methods and functions. The common way around it is to use None as the 
default value, check the arg in the fuction body and then assign 
the desired mutable to it. This proposal does the check/assignment for you.
You only need to type the arg name where you want the check, and then 
activate the Quick Assistant.

Example
=======
Before:
----------------------------------------------
def func(arg = None):
    arg # <- place cursor here and hit Ctrl-1!
    ...
----------------------------------------------

After:
----------------------------------------------
def func(arg = None):
    if arg is None:
        arg = dict()
    ...
----------------------------------------------

"""
__author__ = """Joel Hedlund <joel.hedlund at gmail.com>"""

__version__ = "1.0.0"

__copyright__ = '''Available under the same conditions as PyDev.

See PyDev license for details.
http://pydev.sourceforge.net
'''

# This is a magic trick that tells the PyDev Extensions editor about the 
# namespace provided for pydev scripts:
if False:
    from org.python.pydev.editor import PyEdit #@UnresolvedImport
    cmd = 'command string'
    editor = PyEdit
    systemGlobals = {}
    

# Set to True to do inefficient stuff that is only useful for debugging 
# and development purposes. Should always be False if not debugging.
DEBUG = False

if DEBUG and cmd == 'onSave':
    from org.python.pydev.jython import JythonPlugin #@UnresolvedImport
    # We want a fresh interpreter if we're debugging this script!
    editor.pyEditScripting.interpreter = JythonPlugin.newPythonInterpreter()
    cmd = 'onCreateActions' # Force it to recreate things

    
# We don't need to add the same assist proposal more than once.
if cmd == 'onCreateActions':
    #
    # Interesting stuff starts here!
    #
    assist_proposal = systemGlobals.get('assist_proposal')
    if assist_proposal is None:
        import assist_proposal
        systemGlobals['assist_proposal'] = assist_proposal
        
    assist_regex_based_proposal = systemGlobals.get('assist_regex_based_proposal')
    if assist_regex_based_proposal is None:
        import assist_regex_based_proposal
        systemGlobals['assist_regex_based_proposal'] = assist_regex_based_proposal
        
    if DEBUG and cmd == 'onSave':
        reload(assist_regex_based_proposal)
    
    o = assist_regex_based_proposal.AssignValueToVarIfNone()
    assist_proposal.register_proposal(o, DEBUG)
