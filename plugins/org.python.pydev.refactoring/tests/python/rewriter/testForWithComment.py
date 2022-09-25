li = ['a', 'b', 'e']
# before
for s in li:
    print(s)

x = (1, 2, 3)
# before
for y in x: # on-line for
    print("foo") # on-line foo
    # after first for-body - disappears
else: # on-line else
    print("bar") # this will be printer at last

# after else body
for (y) in (x): # on-line for 2
    for a in x:
        print("foo") # on-line foo
        # after 1. body - disappears currently
    else: # on-line else
        print("bar") # print "bar"

else:
    print("bla")

# after else body (last line)