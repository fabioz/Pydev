class Rectangle(object):

    class Foo:
        def foo(self):
            class bla:
                print("foo")
            b = bla()
            print("foo nested class")

    a = Foo()
    a.foo()
    def __init__(self, width, height):
        self.width = width
        self.height = height
        self.color = "red"
        local_border = "bold"
    def get_area(self):
        return self.width * self.height
    
    def set_area(self, value):
        self.width = sqrt(value)
        self.height = sqrt(value)
    
    def del_area(self):
        del self.area
        
    def get_foo(self):
        return 1
    
    def set_foo(self, value):
        return
    
    def del_foo(self):
        return
   
    empty = property()
    area = property(get_area, doc='area of the rectangle')
    completeproperty = property(get_area, set_area, del_area, doc='area of the rectangle')
    invalidproperty1 = property(None, None, None, docfoo=None)
    invalidproperty2 = property(None, None, None, docfoo="test")
    invalidproperty3 = Property(get_foo, set_foo, del_foo, "test")
    fakeproperty = property(None, None, None, None)
    fakeproperty2 = property(None, None, None, "docstring")
    fakeproperty3 = property(None)
    fakeproperty4 = property(None, None)
    fakeproperty5 = property(None, None, None)
    fakeProperty6 = property(None, None, None, None)
    docfoo = "foodoc"
    fakeProperty7 = property(None, None, None, docfoo)
    fakeproperty8 = property(None, None, None, doc="docfoo")
    
    simpleproperty0 = property(get_foo, None, None, None)
    simpleproperty1 = property(get_foo, None, None, "docstring")
    simpleproperty2 = property(None, set_foo, None, "docstring")
    simpleproperty3 = property(None, set_foo, del_foo, "docstring")
    simpleproperty4 = property(None, None, del_foo, "docstring")
    simpleproperty5 = property(get_foo, set_foo, del_foo)
    simpleproperty6 = property(get_foo, set_foo)
    simpleproperty7 = property(get_foo)
    simpleProperty8 = property(get_foo, None, del_foo)
    simpleProperty9 = property(get_foo, None, del_foo, "docstring")
    
    simpleProperty10 = property(get_foo, fset=set_foo, doc="test")
    simpleProperty11 = property(fget=get_foo, fset=set_foo, fdel=del_foo, doc="test")
    
    
    
rect = Rectangle(10, 15)
print(rect.width)
print(rect.area)

##r

# 23
# Rectangle empty false false false false false
# Rectangle area false true false false true
# Rectangle completeproperty true true true true true
# Rectangle fakeproperty false false false false false
# Rectangle fakeproperty2 false false false false true
# Rectangle fakeproperty3 false false false false false
# Rectangle fakeproperty4 false false false false false
# Rectangle fakeproperty5 false false false false false
# Rectangle fakeProperty6 false false false false false
# Rectangle fakeProperty7 false false false false true
# Rectangle fakeproperty8 false false false false true
# Rectangle simpleproperty0 false true false false false
# Rectangle simpleproperty1 false true false false true
# Rectangle simpleproperty2 false false true false true
# Rectangle simpleproperty3 false false true true true
# Rectangle simpleproperty4 false false false true true
# Rectangle simpleproperty5 false true true true false
# Rectangle simpleproperty6 false true true false false
# Rectangle simpleproperty7 false true false false false
# Rectangle simpleProperty8 false true false true false
# Rectangle simpleProperty9 false true false true true
# Rectangle simpleProperty10 false true true false true
# Rectangle simpleProperty11 true true true true true