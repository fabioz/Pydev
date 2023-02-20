global1 = 123
global2 = 456
def myfunc():
    print("hello")
    local = 5

class Foo:
    def myfoo(self, x, y):
        print("myfoO")
        mylocalfoo = 123
    
    a = 5

global3 = foo()
global3.myfoo()
global3.a = 123
global4 = global3
global4.myfoo()

##r ignore self

# myfunc 2
## print
## local

# myfoo 4
## x
## y
## print
## mylocalfoo

# Foo 1
## a