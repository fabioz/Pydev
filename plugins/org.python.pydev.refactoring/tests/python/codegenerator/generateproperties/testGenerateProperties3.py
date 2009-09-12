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
  <methodOffsetStrategy>1</methodOffsetStrategy>
  <propertyOffsetStrategy>4</propertyOffsetStrategy>
  <methodSelection>
    <int>0</int>
    <int>2</int>
  </methodSelection>
  <accessModifier>1</accessModifier>
</config>

##r

class A(object):

    def __init__(self, attribute):
        self.attribute = attribute

    def get_attribute(self):
        return self.__attribute


    def del_attribute(self):
        del self.__attribute


    print "Initializing A"
    attribute = "hello"
    
    def my_method(self):
        print self.attribute
    attribute = property(get_attribute, None, del_attribute, None)
        
a = A()
a.my_method()