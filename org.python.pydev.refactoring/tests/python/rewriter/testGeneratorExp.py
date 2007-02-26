def updown(N):
    a = 5
    b = 2
    c = (2, 3)
    d = True
    # is handled by listComp
    (a for x in range(9) if 2 == 2)
    for x in xrange(N, 0, -1):
        yield x
    

for i in updown(3):
    print i