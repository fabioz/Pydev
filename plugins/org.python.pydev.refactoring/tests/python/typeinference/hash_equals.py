class Foo(object):
    def set1(self, arg):
        self.attr = arg

    def set2(self, arg):
        self.attr = arg

x = Foo()
Foo.set1(x, 1)
Foo.set2(x, 3.14)
x.attr ## type float|int


class Bar(object):
    def __init__(self):
        self.attr = 2
        
    def get2(self):
        return self.attr
        
    def get1(self):
        return self.attr


if random():
    y = Bar().get1()
else:
    y = Bar().get2()

y ## type int                       


        

class Baz(object):
    def __init__(self):
        self.attr = 2
        
    def get2(self):
        return self.method
        
    def get1(self):
        return self.method
        
    def method(self):
        return 5


if random():
    y = Baz().get1()
else:
    y = Baz().get2()

y() ## type int                       

a = Baz
b = Baz


if random():
    c = a
else:
    c = b
    
c() ## type Baz




