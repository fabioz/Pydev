class A(object):
    pass

A ## type metaclass

a = A()
a ## type A


B = A
b = B()
b ## type A


if random():
    def A():
        return 1

c = A()
c ## type A|int


class D(object):
    def __init__(self, arg):
        self ## type D
        arg ## type float|int

D(7)
D
D(3.14)