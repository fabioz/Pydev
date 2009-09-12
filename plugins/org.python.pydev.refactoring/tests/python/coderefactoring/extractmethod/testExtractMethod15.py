class A:
    def test(self, a):
        ##|var = a * a##|


    def my_method(self):
        print self.attribute

b = A()
b.test(55)

##r

class A:

    def extracted_method(self, a):
        var = a * a

    def test(self, a):
        self.extracted_method(a)


    def my_method(self):
        print self.attribute

b = A()
b.test(55)