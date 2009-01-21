foo = [1, 2, 3, 4, 5]
print foo
# before
del (foo[0], foo[1]) # on-line
# after
print foo
del foo[1]
print foo
del foo
x = range(15)
print x
del x[2:4], x[-1:-3]
print x
del x[::1]
print x