class A:
    def test(self):
        a = 1
        ##|while (a < 2):
            a += 1##|
        var = a * a
        print var
            
    def my_method(self):
        print self.attribute
        
a = A()
a.test()

##r selection starts at the first char, but we have to normalize indentation!

while (a < 2):
    a += 1
