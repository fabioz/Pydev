'''
Refactoring tests.
'''

from coilib import unittest
import refactoring
import os

#===============================================================================
# delete
#===============================================================================
def delete(filename):
    '''Removes filename, or does nothing if the file doesn't exist.
    '''
    try:
        os.remove(filename)
    except OSError:
        pass

#================================================================================
# createFile       
#================================================================================
def createFile(filename, contents='', flag='w'):
    '''Creates the given filename with the given contents.
    '''
    f = file(filename, flag)
    f.write(contents)
    f.close()

FILE = 'temporary_file.py'
    
class Test(unittest.TestCase):

    def getInitialFile(self):
        s = \
'''
class C:
    def a(self):
        a = 2
        b = 3
        c = a+b #this should be refactored.
        return c
'''
        return s
        
    def getRefactoredFile(self):
        s = \
'''
class C:
    def a(self):
        a = 2
        b = 3
        c = self.plusMet(a, b)
        return c

    def plusMet(self, a, b):
        c = a+b #this should be refactored.
        return c
'''
        return s

    def setUp(self):
        unittest.TestCase.setUp(self)
        createFile(FILE, self.getInitialFile())

    def tearDown(self):
        unittest.TestCase.tearDown(self)
        delete(FILE)
    
    def testIt(self):
        r = refactoring.Refactoring()
        s = r.extractMethod(FILE, 5+1, 0, 5+1, 44, 'plusMet')

        f = file(FILE, 'r')
        contents = f.read()
        f.close()

        self.assertEquals(contents, self.getRefactoredFile())
        
        
if __name__ == '__main__':
    unittest.main()

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    