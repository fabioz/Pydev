# before
if 3 == 2: # on-line 3==2
    # after first if
    print("foo") # on-line
# after first body
elif 3 != 2: # on-line first elif
    print("bar") # on-line elif print
    if "foo" != "bar":
        print("if in elif")
    elif 1 != 0:
        print("bla")
    else:
        print("bar")
        # after second body (elif)
else: # on-line else
    print("abc") # print abc
    print("nextline")
    print("another")
    if 3 == 4:
        print("if in if")
    elif 4 == 5:
        print("elif in if/else")
    else:
        print("else in if in if")
    # some test
# after everything

##r

# before
if 3 == 2: # on-line 3==2
    # after first if
    print("foo") # on-line
# after first body
elif 3 != 2: # on-line first elif
    print("bar") # on-line elif print
    if "foo" != "bar":
        print("if in elif")
    elif 1 != 0:
        print("bla")
    else:
        print("bar")
        # after second body (elif)
else: # on-line else
    print("abc") # print abc
    print("nextline")
    print("another")
    if 3 == 4:
        print("if in if")
    elif 4 == 5:
        print("elif in if/else")
    else:
        print("else in if in if")
    # some test
# after everything