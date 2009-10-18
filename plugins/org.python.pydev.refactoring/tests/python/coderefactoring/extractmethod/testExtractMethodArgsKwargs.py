global_var = {}

class A:
    def test(self, a):
        ##|
        b = self.my_method(*a, **global_var)
        ##|
        print b


    def my_method(self, a, **kwargs):
        print a
        return a + [1] + kwargs

b = A()
b.test(55)

##r
global_var = {}

class A:

    def extracted_method(self, a):
        b = self.my_method(*a, **global_var)
        return b

    def test(self, a):
        
        b = self.extracted_method(a)
        
        print b


    def my_method(self, a, **kwargs):
        print a
        return a + [1] + kwargs

b = A()
b.test(55)