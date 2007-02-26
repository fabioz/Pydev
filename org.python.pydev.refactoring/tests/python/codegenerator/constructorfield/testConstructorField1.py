class A:
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
  <offsetStrategy>1</offsetStrategy>
</config>
'''

##r
class A:

    def __init__(self, anAttribute):
        self.anAttribute = anAttribute

    print "Initializing A"
    anAttribute = "hello"  
    
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.myMethod()