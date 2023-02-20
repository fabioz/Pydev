class A:
    def __init__(self, **opts):
        print("foo")

class B(A):
    try:
        print("foo")
    finally:
        print("done.")
    
    attribute = "hello"
    
    def my_method(self):
        print(self.attribute)
        
b = B()
b.my_method()

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

##r Again any kwargs of a superclass init-method must be called with kwargs (same as for vararg)
class A:
    def __init__(self, **opts):
        print("foo")

class B(A):

    def __init__(self, attribute, **kwargs):
        A.__init__(self, **kwargs)
        self.attribute = attribute

    try:
        print("foo")
    finally:
        print("done.")
    
    attribute = "hello"
    
    def my_method(self):
        print(self.attribute)
        
b = B()
b.my_method()