a, b = 1, 3.14

a ## type int
b ## type float

c = d, (e, f) = a, ("test", True)

c ## type tuple
d ## type int
e ## type str
f ## type bool


g = (1, 3.1)
h, i = g

g ## type tuple
h ## type int
i ## type float


def func():
    return 1.1, "str"

j = func()
k, l = func()
j ## type tuple
k ## type float
l ## type str