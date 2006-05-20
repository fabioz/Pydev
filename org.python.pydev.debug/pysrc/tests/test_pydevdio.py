import sys
import os

#make it as if we were executing from the directory above this one (so that we can use jycompletionserver
#without the need for it being in the pythonpath)
sys.argv[0] = os.path.dirname(sys.argv[0]) 
#twice the dirname to get the previous level from this file.
sys.path.insert(1, os.path.join(  os.path.dirname( sys.argv[0] )) )

import unittest

class Test(unittest.TestCase):
    
    def testIt(self):
        import pydevd_io
        original = sys.stdout
        
        try:
            sys.stdout = pydevd_io.IOBuf()
            print 'foo'
            print 'bar'
            
            self.assertEquals('foo\nbar\n', sys.stdout.getvalue()) #@UndefinedVariable
            
            print 'ww'
            print 'xx'
            self.assertEquals('ww\nxx\n', sys.stdout.getvalue()) #@UndefinedVariable
        finally:
            sys.stdout = original
        
if __name__ == '__main__':
    unittest.main()
