a_global = 3
class Rectangle(object):

    attribute_in_class = 3
    
    class Foo:
        def foo_meth(self):
            class bla:
                bla_attribute = "blattr"
                print "foo"
            foolocal = bla()
            self.fooattribute = "fooattribute"
            print "foo nested class"

    rect_attribute = Foo() 
    rect_attribute.foo_meth()
    
    def __init__(self, width, height):
        Rectangle.another_width = 3
        self.width = width
        self.width = 123 # should be ignored by Attribute visitor
        self.height = height
        self.color = "red"
        no_attribute = "bold"
        
    def get_area(self):
        global attribute_in_class
        print "getArea ", self.attribute_in_class
        no_attribute = 35
        self.first_attr_in_tuple, self.second_attr_in_tuple, Rectangle.third_attr_in_tuple = "foo"
        return self.width * self.height
   
    area = property(get_area, doc='area of the rectangle')
    print "Class scope attribute", attribute_in_class
    a_global = 4
    print "Global ", a_global
    
    
      
rect = Rectangle(10, 15)
print rect.width
print rect.area
print rect.attribute_in_class

# will ignore a_global -> we want attributes only
##r

# 11
# Rectangle attribute_in_class
# bla bla_attribute
# Foo fooattribute
# Rectangle rect_attribute
# Rectangle another_width
# Rectangle width
# Rectangle height
# Rectangle color
# Rectangle first_attr_in_tuple
# Rectangle second_attr_in_tuple
# Rectangle third_attr_in_tuple
# 9
# Rectangle attribute_in_class
# Rectangle rect_attribute
# Rectangle another_width
# Rectangle width
# Rectangle height
# Rectangle color
# Rectangle first_attr_in_tuple
# Rectangle second_attr_in_tuple
# Rectangle third_attr_in_tuple