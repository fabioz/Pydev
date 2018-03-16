from testlib.unittest.anothertest import AnotherTest
from testlib.unittest import anothertest
import testlib.unittest
import os.path

class SomeA(object):
    def fun(self):
        pass

class C1(object):
    a = SomeA()
    b = AnotherTest
    b2 = AnotherTest()
    x, b3 = '', AnotherTest()
    b4, y = AnotherTest(), ''
    c = anothertest.AnotherTest
    d = anothertest
    e = testlib.unittest.TestCase
    f = os.path

c1 = C1()