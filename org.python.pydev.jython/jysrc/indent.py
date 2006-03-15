'''
This is the first example on how to program with Jython within Pydev.

There is a 'protocol' that has to be followed when making scripts and some peculiarities. Let's see how it works...

1. The objects that we can act upon will be set from the outside of the plugin (in the java code). As this is
'arbitrary' for any action, each script should make clear which are its 'required' localsp


'''

#--------------------------------------------------------------- REQUIRED LOCALS
assert doc is not None #interface: IDocument
assert cmd is not None #interface: DocumentCommand
assert prefs is not None #interface: IIndentPrefs
assert strategy is not None #interface: PyAutoIndentStrategy

