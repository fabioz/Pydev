class A:
    def test(self):
##|        a = 5
        var = a * a##|
        print var
            
    def my_method(self):
        print self.attribute
        
a = A()
a.test()

##r selection is a line begin -> normalize selection!

class A:

    def extracted_method(self):
        a = 5
        var = a * a
        return var

    def test(self):
        var = self.extracted_method()
        print var
            
    def my_method(self):
        print self.attribute
        
a = A()
a.test()