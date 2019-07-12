from typing import List, Tuple, AnyStr

class A(object):

    def foo(self)->None:
        ...

class MyMessage(object):

    def __init__(self, msg: A):
        ...
