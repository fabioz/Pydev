class A:
    def test(self):
        a = 10
        print a
        anAttribute = "hello"  
    
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.test()

##r a is used twice!
# test 3
## a
## a
## anAttribute

# myMethod 0

# A 0