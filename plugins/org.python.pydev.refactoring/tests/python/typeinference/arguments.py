def add(a, b):
    a ## type float|int|str
    b ## type float|str
    
    b = "test"
    b ## type str

def double(x):
    add(x, x)

pi = 3.14
if random():
    a = 7
    add(a, pi)
else:
    add(1.2, pi)

double("x")


def special(a, b, c, d=True):
    a ## type int
    b ## type str
    c ## type float
    d ## type bool

special(1, c=1.1, b="test")