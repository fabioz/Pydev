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

def getInitialFile():
    s = \
'''
class C:
    def a(self):
        a = 2
        b = 3
        c = a+b #this should be refactored.
        return c
c = C()
'''
    return s


def getRenameRefactored():
    s = \
'''
class G:
    def a(self):
        a = 2
        b = 3
        c = a+b #this should be refactored.
        return c
c = G()
'''
    return s
    
class Test(unittest.TestCase):

        
    def getRefactoredFile(self):
        s = \
'''
class C:
    def a(self):
        a = 2
        b = 3
        c = self.plusMet(a, b) #this should be refactored.
        return c

    def plusMet(self, a, b):
        return a+b
c = C()
'''
        return s

    def setUp(self):
        unittest.TestCase.setUp(self)
        createFile(FILE, getInitialFile())

    def tearDown(self):
        unittest.TestCase.tearDown(self)
        delete(FILE)
    
    def testExtractMethod(self):
        r = refactoring.Refactoring()
        s = r.extractMethod(FILE, 5+1, 12, 5+1, 12+3, 'plusMet')

        f = file(FILE, 'r')
        contents = f.read()
        f.close()

        self.assertEquals(self.getRefactoredFile(), contents)

    def testRename(self):
        r = refactoring.Refactoring()
        s = r.renameByCoordinates(FILE, 1+1, 6, 'G')

        f = file(FILE, 'r')
        contents = f.read()
        f.close()

        self.assertEquals(getRenameRefactored(), contents)

        
if __name__ == '__main__':
    unittest.main()

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    