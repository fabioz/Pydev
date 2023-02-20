class A:
    def test(self):
        ##|a = 10
        b = 20
        ##|
        print(a, b)


    def my_method(self):
        print(self.attribute)

##r

class A:

    def extracted_method(self):
        a = 10
        b = 20
        return a, b

    def test(self):
        a, b = self.extracted_method()
        
        print(a, b)


    def my_method(self):
        print(self.attribute)
