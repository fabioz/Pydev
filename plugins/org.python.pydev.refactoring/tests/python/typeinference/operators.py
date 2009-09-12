class Foo(object):
    def __add__(self, other):
        return 1
        
    def __mul__(self, other):
        return 3.14

n = Foo()
a = n + 1
a ## type int

b = n * 1
b ## type float
