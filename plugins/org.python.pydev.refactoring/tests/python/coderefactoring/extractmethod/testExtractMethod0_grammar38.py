class A:
    def test(self):
        ##|attribute: str
        attribute = "hello"
        print(attribute)##|

a = A()
a.test()

##r

class A:

    def extracted_method(self, attribute):
        attribute: str
        attribute = "hello"
        print(attribute)

    def test(self):
        self.extracted_method(attribute)

a = A()
a.test()