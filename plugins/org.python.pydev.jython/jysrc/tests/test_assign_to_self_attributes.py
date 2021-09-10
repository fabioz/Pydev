#Note that this test must be run from org.python.pydev.jythontests.JythonTest
#as it depends on PyDev classes (i.e.: PySelection)
import sys
IS_JYTHON = sys.platform.find('java') != -1

try:
    import unittest
except:
    sys.stderr.write('--- PYTHONPATH FOUND:\n')
    sys.stderr.write('\n'.join(sys.path))
    sys.stderr.write('\n--- END PYTHONPATH\n')
    raise

#=======================================================================================================================
# TestIndentPrefs
#=======================================================================================================================
class TestIndentPrefs:
    
    def getIndentationString(self):
        return '    '

#=======================================================================================================================
# TestEditor
#=======================================================================================================================
class TestEditor:

    def __init__(self, document, ps):
        self.doc = document
        self.ps = ps
    
    def createPySelection(self):
        return self.ps
    
    def getIndentPrefs(self):
        return TestIndentPrefs()
    
    def setSelection(self, offset, len):
        pass

#=======================================================================================================================
# Test
#=======================================================================================================================
class Test(unittest.TestCase):
    
    def setUp(self):
        unittest.TestCase.setUp(self)
    
    def tearDown(self):
        unittest.TestCase.tearDown(self)
        
    def testAssignToSelfAttributes(self):
        initial_doc = '''class A:
    def m1(self, a, b):
        pass
'''
        
        final_doc = '''class A:
    def m1(self, a, b):
        self.a = a
        self.b = b
'''
        self.check(initial_doc, final_doc)
        
    def testAssignToSelfAttributes1(self):
        initial_doc = '''class A:
    def m1(self, a, b):
        pass'''
        
        final_doc = '''class A:
    def m1(self, a, b):
        self.a = a
        self.b = b
'''
        self.check(initial_doc, final_doc)
        
    def testAssignToSelfAttributes2(self):
        initial_doc = '''class A:
    def m1(self, a=(1,2)):
        pass'''
        
        final_doc = '''class A:
    def m1(self, a=(1,2)):
        self.a = a
'''
        self.check(initial_doc, final_doc)
        
    def testAssignToSelfAttributes3(self):
        initial_doc = '''class A:
    def m1(self, a, b):
        self.a = a'''
        
        final_doc = '''class A:
    def m1(self, a, b):
        self.a = a
        self.b = b
'''
        self.check(initial_doc, final_doc)
    
    def testAssignToSelfAttributes4(self):
        initial_doc = '''class RobotInfoEnv(object):
    def __init__(self, env: Dict[str, str]):'''
        
        final_doc = '''class RobotInfoEnv(object):
    def __init__(self, env: Dict[str, str]):
        self.env = env
'''
        self.check(initial_doc, final_doc)
        
    def testAssignToSelfAttributes5(self):
        initial_doc = '''class Foo(object):
    def __init__(self, complex_var: Dict[str, Node[str]()]):'''
        
        final_doc = '''class Foo(object):
    def __init__(self, complex_var: Dict[str, Node[str]()]):
        self.complex_var = complex_var
'''
        self.check(initial_doc, final_doc)
    
    def testAssignToSelfAttributes6(self):
        initial_doc = '''class Foo(object):
    def __init__(self, complex_var: Dict[str, Node('')]):'''
        
        final_doc = '''class Foo(object):
    def __init__(self, complex_var: Dict[str, Node('')]):
        self.complex_var = complex_var
'''
        self.check(initial_doc, final_doc)

    def testAssignToSelfAttributes7(self):
        initial_doc = '''class Foo(object):
    def __init__(self, complex_var: Dict[str, Node(0)]):'''
        
        final_doc = '''class Foo(object):
    def __init__(self, complex_var: Dict[str, Node(0)]):
        self.complex_var = complex_var
'''
        self.check(initial_doc, final_doc)

    def testAssignToSelfAttributes8(self):
        initial_doc = '''class Foo(object):
    def __init__(self, complex_var: DefaultDict[int, bytes]()):'''
        
        final_doc = '''class Foo(object):
    def __init__(self, complex_var: DefaultDict[int, bytes]()):
        self.complex_var = complex_var
'''
        self.check(initial_doc, final_doc)

    def testAssignToSelfAttributes9(self):
        initial_doc = '''class Foo(object):
    def __init__(self, complex_var: collections.defaultdict()):'''
        
        final_doc = '''class Foo(object):
    def __init__(self, complex_var: collections.defaultdict()):
        self.complex_var = complex_var
'''
        self.check(initial_doc, final_doc)

    def testAssignToSelfAttributes10(self):
        initial_doc = '''class Foo(object):
    def __init__(self, complex_var: str):'''
        
        final_doc = '''class Foo(object):
    def __init__(self, complex_var: str):
        self.complex_var = complex_var
'''
        self.check(initial_doc, final_doc)

    def testAssignToSelfAttributes11(self):
        initial_doc = '''class Tree:
    def __init__(self, left: 'Tree', right: 'Tree'):'''
        
        final_doc = '''class Tree:
    def __init__(self, left: 'Tree', right: 'Tree'):
        self.left = left
        self.right = right
'''
        self.check(initial_doc, final_doc)

    def testAssignToSelfAttributes12(self):
        initial_doc = '''class Foo:
    def __init__(self, x: Union[int, None, Empty]):'''
        
        final_doc = '''class Foo:
    def __init__(self, x: Union[int, None, Empty]):
        self.x = x
'''
        self.check(initial_doc, final_doc)

    def testAssignToSelfAttributes13(self):
        initial_doc = '''class Foo:
    def __init__(self, x: str): -> int'''
        
        final_doc = '''class Foo:
    def __init__(self, x: str): -> int
        self.x = x
'''
        self.check(initial_doc, final_doc)

    def testAssignToSelfAttributes14(self):
        initial_doc = '''class Foo:
    def __init__(self, x: Union[int, None, Empty]): -> Union[str, None, int]'''
        
        final_doc = '''class Foo:
    def __init__(self, x: Union[int, None, Empty]): -> Union[str, None, int]
        self.x = x
'''
        self.check(initial_doc, final_doc)

    def testAssignToSelfAttributes15(self):
        initial_doc = '''class Foo:
    def __init__(self, x: Union[Dict[str, str], None, Empty]):'''
        
        final_doc = '''class Foo:
    def __init__(self, x: Union[Dict[str, str], None, Empty]):
        self.x = x
'''
        self.check(initial_doc, final_doc)
        
    def check(self, initial_doc, final_doc):
        from org.eclipse.jface.text import Document #@UnresolvedImport
        from org.python.pydev.core.docutils import PySelection
        from assign_params_to_attributes_action import AssignToAttribsOfSelf
        doc = Document(initial_doc)
        editor = TestEditor(doc, PySelection(doc, 1, 2))
        assign_to_attribs_of_self = AssignToAttribsOfSelf(editor)
        assign_to_attribs_of_self.run()
        self.assertEqual(final_doc, doc.get())

#=======================================================================================================================
# main
#=======================================================================================================================
if __name__ == '__main__':
    if IS_JYTHON:
        suite = unittest.makeSuite(Test)
        unittest.TextTestRunner(verbosity=3).run(suite)
    else:
        sys.stdout.write('Not running jython tests for non-java platform: %s' % sys.platform)

