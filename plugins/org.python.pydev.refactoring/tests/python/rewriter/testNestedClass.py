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
    
    area = property(getArea, doc='area of the rectangle')

myRectangle = Rectangle(10, 15)
print myRectangle.width
print myRectangle.area