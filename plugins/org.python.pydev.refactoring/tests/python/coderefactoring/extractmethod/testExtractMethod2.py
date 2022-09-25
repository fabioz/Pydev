class A:
    def test(self):
        a = 10
        ##|print(a)##|
        attribute = "hello"
    
    def my_method(self):
        print(self.attribute)
        
a = A()
a.test()

##r

class A:

    def extracted_method(self, a):
        return print(a)

    def test(self):
        a = 10
        self.extracted_method(a)
        attribute = "hello"
    
    def my_method(self):
        print(self.attribute)
        
a = A()
a.test()