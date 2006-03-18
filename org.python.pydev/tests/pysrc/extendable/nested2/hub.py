from testlib.unittest.anothertest import AnotherTest
from testlib.unittest import anothertest

class SomeA(object):
    def fun(self):
        pass

class C1(object):
    a = SomeA()
    b = AnotherTest
    c = anothertest.AnotherTest
    d = anothertest

c1 = C1()