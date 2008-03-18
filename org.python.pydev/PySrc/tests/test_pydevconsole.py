import unittest
import sys
import os

sys.argv[0] = os.path.dirname(sys.argv[0]) 
sys.path.insert(1, os.path.join(  os.path.dirname( sys.argv[0] )) )

import pydevconsole

#=======================================================================================================================
# Test
#=======================================================================================================================
class Test(unittest.TestCase):
    
    def testConsoleRequests(self):
        interpreter = pydevconsole.InterpreterInterface()
        interpreter.addExec('class Foo:')
        interpreter.addExec('   CONSTANT=1')
        interpreter.addExec('')
        interpreter.addExec('foo=Foo()')
        interpreter.addExec('foo.__doc__=None')
        interpreter.addExec('val = raw_input()')
        interpreter.addExec('50')
        
        comps = interpreter.getCompletions('foo.')
        self.assert_(('CONSTANT', '', '', '3') in comps or ('CONSTANT', '', '', '4') in comps)
        
        comps = interpreter.getCompletions('"".')
        self.assert_(('__add__', 'x.__add__(y) <==> x+y', '', '3') in comps or ('__add__', '', '', '4') in comps)
        
        self.assert_(('AssertionError', '', '', '1') in interpreter.getCompletions(''))
        self.assert_(('RuntimeError', '', '', '1') not in interpreter.getCompletions('Assert'))
        
        self.assert_(('__doc__', None, '', '3') not in interpreter.getCompletions('foo.CO'))
        
        comps = interpreter.getCompletions('va')
        self.assert_(('val', '', '', '3') in comps or ('val', '', '', '4') in comps)
        
        
        
#=======================================================================================================================
# main        
#=======================================================================================================================
if __name__ == '__main__':
    unittest.main()

