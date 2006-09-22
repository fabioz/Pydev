import logging
logger = logging.getLogger('myapp') 

initialNone = None

class Foo(object):
    def m1(self):
        pass

initialSet = Foo()

class WithGetAttr(object):
    
    def __getattribute__(self, attr):
        return attr
    
getWithAttr = WithGetAttr()