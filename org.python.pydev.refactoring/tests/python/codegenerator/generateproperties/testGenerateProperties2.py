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
  <methodOffsetStrategy>2</methodOffsetStrategy>
  <propertyOffsetStrategy>4</propertyOffsetStrategy>
  <methodSelection>
    <int>1</int>
    <int>2</int>
  </methodSelection>
  <accessModifier>1</accessModifier>
</config>
'''

##r
class A(object):

    def setAnAttribute(self, value):
        self.__anAttribute = value


    def delAnAttribute(self):
        del self.__anAttribute


    def __init__(self, anAttribute):
        self.anAttribute = anAttribute

    print "Initializing A"
    anAttribute = "hello"  
    
    def myMethod(self):
        print self.anAttribute

    anAttribute = property(None, setAnAttribute, delAnAttribute, None)
        
a = A()
a.myMethod()