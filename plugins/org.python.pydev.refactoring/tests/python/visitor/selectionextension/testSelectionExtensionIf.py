class A:
    def test(self):
        ##|if (2 == 3):
            print("foo")
        els##|e:
            print("bar")
        attribute = "hello"  
    
    def my_method(self):
        print(self.attribute)
        
a = A()
a.test()

##r

if (2 == 3):
    print("foo")
else:
    print("bar")