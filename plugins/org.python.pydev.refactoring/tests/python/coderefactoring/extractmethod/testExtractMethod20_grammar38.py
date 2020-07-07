class A:
    def test(self):
        ##|attribute: str = "something"
        attribute = "hello"
        print(attribute)##|

a = A()
a.test()

##r

class A:

    def extracted_method(self):
        attribute: str = "something"
        attribute = "hello"
        print(attribute)

    def test(self):
        self.extracted_method()

a = A()
a.test()