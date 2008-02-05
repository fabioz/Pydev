class A:
    def foo(self, **someDict):
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
    def foo(self, **someDict):
        print "foo"

class B(A):
    try:
        print "foo"
    finally:
        print "done."
    
    anAttribute = "hello"  
    
    def myMethod(self):
        print self.anAttribute

    def foo(self, **someDict):
        return A.foo(self, **someDict)

        
b = B()
b.myMethod()