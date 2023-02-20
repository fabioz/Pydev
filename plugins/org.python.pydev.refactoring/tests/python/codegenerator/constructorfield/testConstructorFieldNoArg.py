class A:
    def __init__(self):
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
  <offsetStrategy>1</offsetStrategy>
</config>
'''

##r no parameters required for base class initialization
class A:
    def __init__(self):
        print("foo")

class B(A):

    def __init__(self, attribute):
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