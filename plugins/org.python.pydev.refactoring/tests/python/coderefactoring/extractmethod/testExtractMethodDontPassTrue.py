class A:
    def test(self):
        a = True
        print ##|a == True##|
    
a = A()
a.test()

##r

class A:

    def extracted_method(self, a):
        return a == True

    def test(self):
        a = True
        print self.extracted_method(a)
    
a = A()
a.test()