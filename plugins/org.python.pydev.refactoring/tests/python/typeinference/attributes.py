class A(object):
    def get(self):
        # float from attributes_user
        self.attribute ## type float|int
        print self.attribute
        return self.attribute

    def set(self, value):
        self.attribute = value

a = A()
a.set(1)
a.get() ## type float|int
