class A:
    def __init__(self):
        print "foo"

class B(A):
    try:
        print "foo"
    finally:
        print "done."
    
    anAttribute = "hello"  
    
    def myMethod(self):
        print self.anAttribute
        
b = B()
b.myMethod()

##c
'''
<config>
  <classSelection>1</classSelection>
  <attributeSelection>
    <int>0</int>
  </attributeSelection>
  <offsetStrategy>1</offsetStrategy>
</config>
'''

##r no parameters required for base class initialization
class A:
    def __init__(self):
        print "foo"

class B(A):

    def __init__(self, anAttribute):
        self.anAttribute = anAttribute

    try:
        print "foo"
    finally:
        print "done."
    
    anAttribute = "hello"  
    
    def myMethod(self):
        print self.anAttribute
        
b = B()
b.myMethod()