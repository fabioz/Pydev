if random():
    a = 1
    a ## type int
elif random():
    a = 3.14
    a ## type float
else:
    a = "test"
    a ## type str

    if random():
        a = 1

    a ## type int|str

a ## type float|int|str


b = 1

def func():
    # Weird, but it's valid Python
    if False:
        global b
    else:
        b = 1.1
    b ## type float|int

b ## type float|int


c = 1

for c in []:
    c ## type object
    c = 3.13
    c ## type float

c ## type float|int|object


d = 2

while random():
    d = 1.1
    d ## type float

d ## type float|int


def test():
    e = 1.1
    e = 1
    if random():
        e ## type int


f = 1
if random():
    f = 1.1
else:
    f ## type int


g = 1
if random():
    g = 3.14
    g = "test"

g ## type int|str


try:
    h = 1
    h = 1.1
except IOError, i:
    h ## type float|int
    h = "str"
    h ## type str
    # not yet done
    i ## type object

h ## type float|int|str

try:
    j = 1.1
finally:
    j = 1

j ## type int