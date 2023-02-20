class A:
    def test(self):
        a = 1
        try:
            ##|print(a)
            print("foo")##|
        except:
            print(b)
            print("bar")
        
        var = a * a
        print(var)
            
    def my_method(self):
        print(self.attribute)
        
a = A()
a.test()

##r selection starts at the first char, but we have to normalize indentation!

print(a)
print("foo")
