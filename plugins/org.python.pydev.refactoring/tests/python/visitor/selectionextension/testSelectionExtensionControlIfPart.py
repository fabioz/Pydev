class A:
    def test(self):
        ##|if##| (1 == 2):
            print "foo"
        else:
            print "bar"
        attribute = "hello"  
    
    def my_method(self):
        print self.attribute
        
a = A()
a.test()

##r

if (1 == 2):
    print "foo"
else:
    print "bar"