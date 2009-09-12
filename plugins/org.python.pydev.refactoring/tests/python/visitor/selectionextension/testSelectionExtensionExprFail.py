class A:
    def test(self):
        print "I##|nitializing A", "test"##|
        attribute = "hello"  
    
    def my_method(self):
        print self.attribute
        
a = A()
a.test()

##r Should expand to Full String "Initializing A"

# Invalid selection:
# nitializing A", "test"