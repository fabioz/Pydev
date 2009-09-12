class A:
    def test(self, a):
       ##|var = a * a
        
       u = "foo"
       print u##|

       v = "bar"
       w = "baz"
        
       print v        
       print var

    def my_method(self):
        print self.attribute

b = A()
b.test(55)

##r

class A:

    def extracted_method(self, a):
        var = a * a
        u = "foo"
        print u
        return var

    def test(self, a):
       var = self.extracted_method(a)

       v = "bar"
       w = "baz"
        
       print v        
       print var

    def my_method(self):
        print self.attribute

b = A()
b.test(55)