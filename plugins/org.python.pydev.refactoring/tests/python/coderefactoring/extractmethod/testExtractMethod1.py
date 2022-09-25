class A:
    def test(self):
        ##|print("Initializing A")##|
        attribute = "hello"
    
    def my_method(self):
        print(self.attribute)
        
a = A()
a.test()

##r

class A:

    def extracted_method(self):
        return print("Initializing A")

    def test(self):
        self.extracted_method()
        attribute = "hello"
    
    def my_method(self):
        print(self.attribute)
        
a = A()
a.test()