class A:
    def foo(self, *args):
        print("foo")

class B(A):
    try:
        print("foo")
    finally:
        print("done.")
    
    attribute = "hello"
    
    def my_method(self):
        print(self.attribute)
        
a = A()
a.my_method()

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
    def foo(self, *args):
        print("foo")

class B(A):
    try:
        print("foo")
    finally:
        print("done.")
    
    attribute = "hello"
    
    def my_method(self):
        print(self.attribute)

    def foo(self, *args):
        super().foo(*args)

        
a = A()
a.my_method()