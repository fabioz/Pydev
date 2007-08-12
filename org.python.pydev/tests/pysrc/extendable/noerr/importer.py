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

class ChildWithGetAttr(WithGetAttr):
	pass

childGetWithAttr = ChildWithGetAttr()

class Struct:
    '''@DynamicAttrs
    '''
    def __init__(self, **entries): 
        self.__dict__.update(entries)
    
globals_struct = Struct(answer=42, linelen = 80, font='courier')
