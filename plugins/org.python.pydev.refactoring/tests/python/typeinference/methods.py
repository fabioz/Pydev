class A(object):
    def method(self):
        self ## type A
        return 1
        
    def test(self, arg):
        self ## type A
        arg ## type bool|float|int

a = A()
a.method() ## type int


test("*confuse*")
a.test(1.1)
nonexisting.test("*disctract*")
a.test(1)
o = object()
o.test("*perplex*")

A.test(a, True)