class A:
    def test(self):
        a = True
        ##|a = True##|
    
a = A()
a.test()


##r
class A:

    def extracted_method(self):
        a = True

    def test(self):
        a = True
        self.extracted_method()
    
a = A()
a.test()