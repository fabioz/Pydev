class A(object):

    def __init__(self, attribute):
        self.attribute = attribute

    print("Initializing A")
    attribute = "hello"
    
    def my_method(self):
        print(self.attribute)
        
a = A()
a.my_method()

##c

<config>
  <classSelection>0</classSelection>
  <attributeSelection>
    <int>0</int>
  </attributeSelection>
  <methodOffsetStrategy>2</methodOffsetStrategy>
  <propertyOffsetStrategy>4</propertyOffsetStrategy>
  <methodSelection>
    <int>1</int>
    <int>2</int>
  </methodSelection>
  <accessModifier>1</accessModifier>
</config>

##r

class A(object):

    def set_attribute(self, value):
        self.__attribute = value


    def del_attribute(self):
        del self.__attribute


    def __init__(self, attribute):
        self.attribute = attribute

    print("Initializing A")
    attribute = "hello"
    
    def my_method(self):
        print(self.attribute)
    attribute = property(None, set_attribute, del_attribute, None)
        
a = A()
a.my_method()