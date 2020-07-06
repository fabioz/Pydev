class A:
    def test(self):
        ##|try:
            a = 10
        except:
            e: ValueError##|

a = A()
a.test()

##r

class A:

    def extracted_method(self):
        try:
            a = 10
        except:
            e: ValueError

    def test(self):
        self.extracted_method(attribute)

a = A()
a.test()