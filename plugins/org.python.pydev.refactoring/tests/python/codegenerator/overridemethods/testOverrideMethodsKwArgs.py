class A:
    def foo(self, **kwargs):
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
  <classSelection>0</classSelection>
  <methodSelection>
    <int>0</int>
  </methodSelection>
  <offsetStrategy>4</offsetStrategy>
  <editClass>1</editClass>
</config>
'''
##r

class A:
    def foo(self, **kwargs):
        print("foo")

class B(A):
    try:
        print("foo")
    finally:
        print("done.")
    
    attribute = "hello"
    
    def my_method(self):
        print(self.attribute)

    def foo(self, **kwargs):
        super().foo(**kwargs)

        
b = B()
b.my_method()