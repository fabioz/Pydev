class A:
    def test(self):
        ##|pr##|int "Initializing A"
        attribute = "hello"  
    
    def my_method(self):
        print self.attribute
        
a = A()
a.test()

##r

print "Initializing A"