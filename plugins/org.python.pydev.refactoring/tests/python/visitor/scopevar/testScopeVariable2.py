class A:
    def test(self):
        a = 10
        print a
        attribute = "hello"
    
    def my_method(self):
        print self.attribute
        
a = A()
a.test()

##r a is used twice!

# test 3
## a
## a
## attribute

# my_method 0

# A 0