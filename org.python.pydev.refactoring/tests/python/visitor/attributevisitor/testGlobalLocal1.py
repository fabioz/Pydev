aGlobal = 3
class Rectangle(object):

    attributeInClass = 3
    
    class Foo:
        def fooMeth(self):
            class bla:
                blaAttribute = "blattr"
                print "foo"
            foolocal = bla()
            self.fooattribute = "fooattribute"
            print "foo nested class"

    rectAttribute = Foo() 
    rectAttribute.fooMeth()          
    def __init__(self, width, height):
        Rectangle.anotherWith = 3
        self.width = width
        self.width = 123 # should be ignored by Attribute visitor
        self.height = height
        self.color = "red"
        noAttribute = "bold"
    def getArea(self):
        global attributeInClass
        print "getArea ", self.attributeInClass
        noAttribute = 35
        self.FirstAttrInTuple, self.SecondInTuple, Rectangle.ThirdInTuple = "foo"
        return self.width * self.height
   
    area = property(getArea, doc='area of the rectangle')
    print "Class scope attribute", attributeInClass
    aGlobal = 4
    print "Global ", aGlobal
    
    
      
myRectangle = Rectangle(10, 15)
print myRectangle.width
print myRectangle.area
print myRectangle.attributeInClass

# will ignore aGlobal -> we want attributes only
##r
# 11
# Rectangle attributeInClass
# bla blaAttribute
# Foo fooattribute
# Rectangle rectAttribute
# Rectangle anotherWith
# Rectangle width
# Rectangle height
# Rectangle color
# Rectangle FirstAttrInTuple
# Rectangle SecondInTuple
# Rectangle ThirdInTuple
# 9
# Rectangle attributeInClass
# Rectangle rectAttribute
# Rectangle anotherWith
# Rectangle width
# Rectangle height
# Rectangle color
# Rectangle FirstAttrInTuple
# Rectangle SecondInTuple
# Rectangle ThirdInTuple