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
'''

##r
class A(object):

    def getAnAttribute(self):
        return self.__anAttribute


    def setAnAttribute(self, value):
        self.__anAttribute = value


    def delAnAttribute(self):
        del self.__anAttribute


    _anAttribute = property(getAnAttribute, setAnAttribute, delAnAttribute, "AnAttribute's Docstring")

    def __init__(self, anAttribute):
        self.anAttribute = anAttribute

    print "Initializing A"
    anAttribute = "hello"  
    
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.myMethod()