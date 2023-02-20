def updown(n):
    a = 5
    b = 2
    c = (2, 3)
    d = True
    # is handled by list comprehension
    (a for x in range(9) if 2 == 2)
    for x in range(n, 0, -1):
        yield x

for i in updown(3):
    print(i)