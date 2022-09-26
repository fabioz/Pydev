class MyMeta(type):
    def __str__(cls):
        return "Beautiful class '%s'" % cls.__name__


class MyClass(metaclass=MyMeta):
    pass


foox = MyClass()
print(type(foox))
# after print type
class A(object): # on-line
    # foo test
    def met(self):
        print('A')


    # after class A comment
class B(A):
    def met(self):
        print('B')
        A.met(self)


class C(A):
    def met(self):
        print('C')
        A.met(self)


class D(B, C):
    def met(self):
        print('D')
        B.met(self)
        C.met(self) # C met comment


    # after C.met
    # also after C.met
    # and this is after class D
d = D()
d.met()