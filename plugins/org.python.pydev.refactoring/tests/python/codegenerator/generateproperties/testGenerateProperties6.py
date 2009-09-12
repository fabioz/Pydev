class A(object):

    def __init__(self, attribute):
        self.attribute = attribute

    print "Initializing A"
    attribute = "hello"
    
    def my_method(self):
        print self.attribute
        
a = A()
a.my_method()

##c

<config>
  <classSelection>0</classSelection>
  <attributeSelection>
    <int>0</int>
  </attributeSelection>
  <methodOffsetStrategy>3</methodOffsetStrategy>
  <propertyOffsetStrategy>2</propertyOffsetStrategy>
  <methodSelection>
    <int>0</int>
    <int>1</int>
    <int>2</int>
    <int>3</int>
  </methodSelection>
  <accessModifier>2</accessModifier>
</config>

##r

class A(object):

    def get_attribute(self):
        return self.__attribute


    def set_attribute(self, value):
        self.__attribute = value


    def del_attribute(self):
        del self.__attribute

    _attribute = property(get_attribute, set_attribute, del_attribute, "_attribute's docstring")

    def __init__(self, attribute):
        self.attribute = attribute

    print "Initializing A"
    attribute = "hello"
    
    def my_method(self):
        print self.attribute
        
a = A()
a.my_method()