class A:
    def __init__(self, **someDict):
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
  <offsetStrategy>2</offsetStrategy>
</config>
'''

##r Again any kwArg's of a superclass init-method must be called kwArg (same as for varArg)
class A:
    def __init__(self, **someDict):
        print "foo"

class B(A):

    def __init__(self, anAttribute, **kwArg):
        A.__init__(self, kwArg)
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