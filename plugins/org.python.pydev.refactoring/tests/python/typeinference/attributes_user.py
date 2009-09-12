import attributes

class Foo(object):
    def get(self):
        return ""
    
    def set(self, value):
        pass

if random():
    a = attributes.A()
else:
    a = Foo()

a.set(3.14)
a.get() ## type float|int|str
