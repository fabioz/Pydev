def simple():
    return 1

simple() ## type int


def multiple():
    if random():
        return 1
    else:
        return 3.14

multiple() ## type float|int


def returns_argument(arg):
    arg ## type float|int
    return arg

# It's context-sensitive
returns_argument(1) ## type int
returns_argument(3.14) ## type float


def call(func):
    result = func()
    result ## type float|int
    return result

call(multiple) ## type float|int


def foo():
    return "x"

def oof():
    if "a" == "b":
        return 5.0
    else:
        return 5
    
    
if  foo == oof:
    z = oof
else:
    z = foo

# engine has to realize that the function might be foo or oof 
x = z()
x ## type float|int|str


def a(x):
    return b(x)

def b(x):
    return x

a(1) ## type int
b(3.14)


class Foo(object):
    def method(self, arg):
        return arg
    
    def another_method(self, x):
        return x
    
    def different_method(self, x):
        if random():
            return self.another_method(x)
        else:
            return self.method(x)
            
    def simpler_method(self, x):
        if random():
            return x
        else:
            return x
            
    def recursive_horror(self, x):
        if random():
            return self.recursive_horror(x)
        else:
            return self.different_method(x)
    
x = Foo()
z = x.method(5.5)
z ## type float
z = x.another_method(10)
y = x.another_method("x")
z ## type int
y ## type str
v = x.different_method(x)
v ## type Foo
w = x.simpler_method(5.5)
w ## type float
u = x.recursive_horror(9)
u ## type int


