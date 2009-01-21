class A:
    def foo(self, *someVarArg):
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
  <classSelection>0</classSelection>
  <methodSelection>
    <string>foo</string>
  </methodSelection>
  <offsetStrategy>4</offsetStrategy>
  <editClass>1</editClass>
</config>
'''

##r
class A:
    def foo(self, *someVarArg):
        print "foo"

class B(A):
    try:
        print "foo"
    finally:
        print "done."
    
    anAttribute = "hello"  
    
    def myMethod(self):
        print self.anAttribute

    def foo(self, *someVarArg):
        return A.foo(self, *someVarArg)

        
a = A()
a.myMethod()