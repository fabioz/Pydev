'''
This is the first example on how to program with Jython within Pydev.

There is a 'protocol' that has to be followed when making scripts and some peculiarities. Let's see how it works...

1. The objects that we can act upon will be set from the outside of the plugin (in the java code). As this is
'arbitrary' for any action, each script should make clear which are its 'required' locals
'''

if False:
    text

def indent(*args, **kwargs):
    global jythonResult
    jythonResult = 1
    
indent(text=text)