#@PydevCodeAnalysisIgnore
'''
Note that this test is run from org.python.pydev.jythontests.JythonTest
(to have the needed eclipse libraries)
'''

import unittest
import sys
IS_JYTHON = sys.platform.find('java') != -1

#===================================================================================================
# PyContextType
#===================================================================================================
class PyContextType:

    def __init__(self):
        self.resolvers = []

    def addResolver(self, resolver):
        self.resolvers.append(resolver)


import __builtin__
py_context_type = PyContextType()
__builtin__.py_context_type = py_context_type


#===================================================================================================
# Context
#===================================================================================================
class Context:

    def __init__(self, doc):
        self.doc = doc
        self.viewer = self

    def getDocument(self):
        return self.doc

    def isCythonFile(self):
        return False

#===================================================================================================
# Test
#===================================================================================================
class Test(unittest.TestCase):

    def setUp(self):
        unittest.TestCase.setUp(self)
        import pytemplate_defaults  #Just importing it will fill the py_context_type
        pytemplate_defaults._CreateSelection = self._CreateSelection

    def _CreateSelection(self, editor):
        return self._selection


    def testResolvers(self):

        types = {}
        for r in py_context_type.resolvers:
            types[r.type] = r

        expected = [
            'current_class',
            'current_method',
            'current_qualified_scope',
            'file',
            'isodate',
            'isodatestr',
            'isodatestr2',
            'lparen_if_py3',
            'module',
            'next_class_or_method',
            'prev_class_or_method',
            'rparen_if_py3',
            'space_if_py2',
            'superclass',
            'pydevd_dir_location',
            'pydevd_file_location',
        ]
        gotten = types.keys()
        gotten.sort()
        expected.sort()
        self.assertEqual(expected, gotten)

# The test below now changed the approach and requires the workbench to work (as it uses PySelection/AST for that now)
#        self.CheckCase1(types)
#        self.CheckCase2(types)
#
#
#    def CheckCase1(self, types):
#        doc = '''class A(object): # line 0
#
#    def m1(self): #line 2
#        pass
#
#    def m2(self): #line 5
#        pass
#        '''
#
#        from org.eclipse.jface.text import Document
#        from org.python.pydev.core.docutils import PySelection
#
#        doc = Document(doc)
#
#        self._selection = PySelection(doc, 1, 0)
#
#        context = Context(doc)
#
#        self.assertEqual(['A'], types['current_class'].resolveAll(context))
#        self.assertEqual([''], types['current_method'].resolveAll(context))
#        self.assertEqual(['A'], types['current_qualified_scope'].resolveAll(context))
#        self.assertEqual(['A'], types['prev_class_or_method'].resolveAll(context))
#        self.assertEqual(['m1'], types['next_class_or_method'].resolveAll(context))
#        self.assertEqual(['object'], types['superclass'].resolveAll(context))
#
#
#    def CheckCase2(self, types):
#        from org.eclipse.jface.text import Document
#        from org.python.pydev.core.docutils import PySelection
#
#        doc = '''class A(object
#
#        '''
#
#        doc = Document(doc)
#
#        self._selection = PySelection(doc, 1, 0)
#
#        context = Context(doc)
#
#        self.assertEqual(['A'], types['current_class'].resolveAll(context))
#        self.assertEqual([''], types['current_method'].resolveAll(context))
#        self.assertEqual(['A'], types['current_qualified_scope'].resolveAll(context))
#        self.assertEqual(['A'], types['prev_class_or_method'].resolveAll(context))
#        self.assertEqual([''], types['next_class_or_method'].resolveAll(context))
#        self.assertEqual([''], types['superclass'].resolveAll(context))
#
#        doc = '''class A(object, obj, foo)
#
#        '''
#
#        doc = Document(doc)
#
#        self._selection = PySelection(doc, 1, 0)
#
#        context = Context(doc)
#
#        self.assertEqual(['A'], types['current_class'].resolveAll(context))
#        self.assertEqual([''], types['current_method'].resolveAll(context))
#        self.assertEqual(['A'], types['current_qualified_scope'].resolveAll(context))
#        self.assertEqual(['A'], types['prev_class_or_method'].resolveAll(context))
#        self.assertEqual([''], types['next_class_or_method'].resolveAll(context))
#        self.assertEqual(['object', 'obj', 'foo'], types['superclass'].resolveAll(context))
#
#        doc = '''class A(object, #comment
#        obj, foo)
#
#        '''
#
#        doc = Document(doc)
#
#        self._selection = PySelection(doc, 1, 0)
#
#        context = Context(doc)
#
#        self.assertEqual(['A'], types['current_class'].resolveAll(context))
#        self.assertEqual([''], types['current_method'].resolveAll(context))
#        self.assertEqual(['A'], types['current_qualified_scope'].resolveAll(context))
#        self.assertEqual(['A'], types['prev_class_or_method'].resolveAll(context))
#        self.assertEqual([''], types['next_class_or_method'].resolveAll(context))
#        self.assertEqual(['object', 'obj', 'foo'], types['superclass'].resolveAll(context))


#===================================================================================================
# main
#===================================================================================================
if __name__ == '__main__':
    if IS_JYTHON:
        suite = unittest.makeSuite(Test)
        unittest.TextTestRunner(verbosity=3).run(suite)
    else:
        sys.stdout.write('Not running jython tests for non-java platform: %s' % sys.platform)
