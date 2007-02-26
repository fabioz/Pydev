class A:
    def __init__(self, *someVarArg):
        print "foo"

class B(A):
    try:
        print "foo"
    finally:
        print "done."
    
    anAttribute = "hello"  
    
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.myMethod()

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

##r VarArg must be after arguments/keywords
class A:
    def __init__(self, *someVarArg):
        print "foo"

class B(A):

    def __init__(self, anAttribute, *varArg):
        A.__init__(self, varArg)
        self.anAttribute = anAttribute

    try:
        print "foo"
    finally:
        print "done."
    
    anAttribute = "hello"  
    
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.myMethod()