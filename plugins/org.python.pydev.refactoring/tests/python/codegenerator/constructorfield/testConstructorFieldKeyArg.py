class A:
    def __init__(self, arg=123):
        print "foo"

class B(A):
    try:
        print "foo"
    finally:
        print "done."
    
    attribute = "hello"
    
    def my_method(self):
        print self.attribute
        
a = A()
a.my_method()

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

##r
class A:
    def __init__(self, arg=123):
        print "foo"

class B(A):

    def __init__(self, arg, attribute):
        A.__init__(self, arg)
        self.attribute = attribute

    try:
        print "foo"
    finally:
        print "done."
    
    attribute = "hello"
    
    def my_method(self):
        print self.attribute
        
a = A()
a.my_method()