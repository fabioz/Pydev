from data.module import YO, YOUPI


class Specialization(YOUPI, YO): pass

class Metaclass(type): pass

class Interface: pass

class MyIFace(Interface): pass

class AnotherIFace(Interface): pass

class MyException(Exception): pass
class MyError(MyException): pass

class AbstractClass(object):

    def to_override(self, whatever):
        raise NotImplementedError()

    def return_something(self, param):
        if param:
            return 'toto'
        return
    
class Concrete0:
    __implements__ = MyIFace
class Concrete1:
    __implements__ = MyIFace, AnotherIFace
class Concrete2:
    __implements__ = (MyIFace,
                      AnotherIFace)
class Concrete23(Concrete1): pass


del YO
[SYN1, SYN2] = Concrete0, Concrete1
assert `1`
b = 1 | 2 & 3 ^ 8
exec 'c = 3'

def raise_string():
    raise 'pas glop'
