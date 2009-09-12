x = 10
x ## type int

y = x
y ## type int

def foo():
    a = 10
    a ## type int

def bar():
    a = "test"
    a ## type str

def baz():
    x ## type int|str
    y ## type int

def qux():
    global g
    g = 1.0
    g ## type float|int

g = 1
g ## type float|int

x = "test"
x ## type str

qux ## type function


def setglobal():
    global b
    b = 1

def setglobalagain():
    global b
    b = 3.14

setglobal()
setglobalagain()
b ## type float|int


c = 1
c = 3.14
c ## type float

def accesglobal():
    c ## type float|int
