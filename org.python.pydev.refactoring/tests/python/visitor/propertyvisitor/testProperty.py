class Rectangle(object):

    class Foo:
        def foo(self):
            class bla:
                print "foo"
            b = bla()
            print "foo nested class"

    a = Foo() 
    a.foo()          
    def __init__(self, width, height):
        self.width = width
        self.height = height
        self.color = "red"
        localBorder = "bold"
    def getArea(self):
        return self.width * self.height
    
    def setArea(self, value):
        self.width = sqrt(value)
        self.height = sqrt(value)
    
    def delArea(self):
        del self.area
        
    def getFoo(self):
        return 1
    
    def setFoo(self, value):
        return
    
    def delFoo(self):
        return
   
    empty = property()
    area = property(getArea, doc='area of the rectangle')
    completeproperty = property(getArea, setArea, delArea, doc='area of the rectangle')
    invalidproperty1 = property(None, None, None, docfoo=None)
    invalidproperty2 = property(None, None, None, docfoo="test")
    invalidproperty3 = Property(getFoo, setFoo, delFoo, "test")
    fakeproperty = property(None, None, None, None)
    fakeproperty2 = property(None, None, None, "docstring")
    fakeproperty3 = property(None)
    fakeproperty4 = property(None, None)
    fakeproperty5 = property(None, None, None)
    fakeProperty6 = property(None, None, None, None)
    docfoo = "foodoc"
    fakeProperty7 = property(None, None, None, docfoo)
    fakeproperty8 = property(None, None, None, doc="docfoo")
    
    simpleproperty0 = property(getFoo, None, None, None)
    simpleproperty1 = property(getFoo, None, None, "docstring")
    simpleproperty2 = property(None, setFoo, None, "docstring")
    simpleproperty3 = property(None, setFoo, delFoo, "docstring")
    simpleproperty4 = property(None, None, delFoo, "docstring")
    simpleproperty5 = property(getFoo, setFoo, delFoo)
    simpleproperty6 = property(getFoo, setFoo)
    simpleproperty7 = property(getFoo)
    simpleProperty8 = property(getFoo, None, delFoo)
    simpleProperty9 = property(getFoo, None, delFoo, "docstring")
    
    simpleProperty10 = property(getFoo, fset=setFoo, doc="test")
    simpleProperty11 = property(fget=getFoo, fset=setFoo, fdel=delFoo, doc="test")
    
    
    
myRectangle = Rectangle(10, 15)
print myRectangle.width
print myRectangle.area

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