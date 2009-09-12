class A:
    def test(self):
        a = 5
        ##|var = a * a##|
        print var
            
    def my_method(self):
        print self.attribute
        
a = A()
a.test()

##r

class A:

    def extracted_method(self, a):
        var = a * a
        return var

    def test(self):
        a = 5
        var = self.extracted_method(a)
        print var
            
    def my_method(self):
        print self.attribute
        
a = A()
a.test()