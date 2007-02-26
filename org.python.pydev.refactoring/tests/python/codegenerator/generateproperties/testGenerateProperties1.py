class A(object):

    def __init__(self, anAttribute):
        self.anAttribute = anAttribute

    print "Initializing A"
    anAttribute = "hello"  
    
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.myMethod()

##c
'''
<config>
  <classSelection>0</classSelection>
  <attributeSelection>
    <int>0</int>
  </attributeSelection>
  <methodOffsetStrategy>1</methodOffsetStrategy>
  <propertyOffsetStrategy>2</propertyOffsetStrategy>
  <methodSelection>
    <int>0</int>
    <int>3</int>
  </methodSelection>
  <accessModifier>1</accessModifier>
</config>
'''

##r
class A(object):

    anAttribute = property(getAnAttribute, None, None, "AnAttribute's Docstring")

    def __init__(self, anAttribute):
        self.anAttribute = anAttribute

    def getAnAttribute(self):
        return self.__anAttribute


    print "Initializing A"
    anAttribute = "hello"  
    
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.myMethod()