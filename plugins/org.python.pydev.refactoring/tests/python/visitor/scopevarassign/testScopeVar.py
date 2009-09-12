global1 = 123
global2 = 456
def myfunc():
    print "hello"
    local = 5

class Foo:
    def myfoo(self):
        print "myfoo"
        mylocalfoo = 123
    
    a = 5

global3 = Foo()
global3 = Foo()
global3.myfoo()
global3.a = 123
global4 = global3
global4.myfoo()

##r unique globals (see global3)

# 4
#  global1
#  global2
#  global3
#  global4