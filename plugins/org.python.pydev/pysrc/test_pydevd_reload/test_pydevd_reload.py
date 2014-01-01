import os
import tempfile
import unittest
import sys
sys.path.insert(0, os.path.realpath(os.path.abspath('..')))

import pydevd_reload

SAMPLE_CODE = """
class C:
    def foo(self):
        return 0

    @classmethod
    def bar(cls):
        return (0, 0)

    @staticmethod
    def stomp():
        return (0, 0, 0)

    def unchanged(self):
        return 'unchanged'
"""

class Test(unittest.TestCase):


    def setUp(self):
        unittest.TestCase.setUp(self)
        self.tempdir = None
        self.save_path = None
        self.tempdir = tempfile.mkdtemp()
        self.save_path = list(sys.path)
        sys.path.append(self.tempdir)
        try:
            del sys.modules['x']
        except:
            pass


    def tearDown(self):
        unittest.TestCase.tearDown(self)
        sys.path = self.save_path
        try:
            del sys.modules['x']
        except:
            pass

    def make_mod(self, name="x", repl=None, subst=None):
        fn = os.path.join(self.tempdir, name + ".py")
        f = open(fn, "w")
        sample = SAMPLE_CODE
        if repl is not None and subst is not None:
            sample = sample.replace(repl, subst)
        try:
            f.write(sample)
        finally:
            f.close()


    def testPydevdReload(self):

        self.make_mod()
        import x

        C = x.C
        COut = C
        Cfoo = C.foo
        Cbar = C.bar
        Cstomp = C.stomp

        def check2(expected):
            C = x.C
            Cfoo = C.foo
            Cbar = C.bar
            Cstomp = C.stomp
            b = C()
            bfoo = b.foo
            self.assertEqual(expected, b.foo())
            self.assertEqual(expected, bfoo())
            self.assertEqual(expected, Cfoo(b))

        def check(expected):
            b = COut()
            bfoo = b.foo
            self.assertEqual(expected, b.foo())
            self.assertEqual(expected, bfoo())
            self.assertEqual(expected, Cfoo(b))
            self.assertEqual((expected, expected), Cbar())
            self.assertEqual((expected, expected, expected), Cstomp())
            check2(expected)

        check(0)

        #modify mod and reload
        count = 0
        while count < 1:
            count += 1
            self.make_mod(repl="0", subst=str(count))
            pydevd_reload.xreload(x)
            check(count)


    def testPydevdReload2(self):

        self.make_mod()
        import x

        c = x.C()
        cfoo = c.foo
        self.assertEqual(0, c.foo())
        self.assertEqual(0, cfoo())

        self.make_mod(repl="0", subst='1')
        pydevd_reload.xreload(x)
        self.assertEqual(1, c.foo())
        self.assertEqual(1, cfoo())

    def testPydevdReload3(self):
        class F:
            def m1(self):
                return 1
        class G:
            def m1(self):
                return 2

        self.assertEqual(F().m1(), 1)
        pydevd_reload._update(F, G)
        self.assertEqual(F().m1(), 2)



    def testIfCodeObjEquals(self):
        class F:
            def m1(self):
                return 1
        class G:
            def m1(self):
                return 1
        class H:
            def m1(self):
                return 2

        self.assertTrue(pydevd_reload.code_objects_equal(F.m1.func_code, G.m1.func_code))
        self.assertFalse(pydevd_reload.code_objects_equal(F.m1.func_code, H.m1.func_code))



    def testMetaclass(self):
        
        class Meta(type):
            def __init__(mcs, name, bases, attrs):
                super(Meta, mcs).__init__(name, bases, attrs)
                
        class F:
            __metaclass__ = Meta
            
            def m1(self):
                return 1
            
        
        class G:
            __metaclass__ = Meta
            
            def m1(self):
                return 2
            
        self.assertEqual(F().m1(), 1)
        pydevd_reload._update(F, G)
        self.assertEqual(F().m1(), 2)

            
if __name__ == "__main__":
    #import sys;sys.argv = ['', 'Test.testPydevdReload']
    unittest.main()