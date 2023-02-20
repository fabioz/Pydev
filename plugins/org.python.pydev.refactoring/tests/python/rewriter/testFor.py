li = ['a', 'b', 'e']
for s in li:
    print(s)

x = (1, 2, 3)
for y in x:
    print("foo") # on-line foo
else:
    print("bar")

for (y) in (x):
    for a in x:
        print("foo")
    else:
        print("bar")

else:
    print("bla")