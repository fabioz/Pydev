class A:
    def test(self, a):
        ##|var = a * a##|
        print var
            
    def my_method(self):
        print self.attribute
        
b = A()
b.test(55)

##r selection starts at the first char

class A:

    def extracted_method(self, a):
        var = a * a
        return var

    def test(self, a):
        var = self.extracted_method(a)
        print var
            
    def my_method(self):
        print self.attribute
        
b = A()
b.test(55)