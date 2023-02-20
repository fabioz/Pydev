if 3 == 2:
    print("foo")
elif 3 != 2:
    print("bar")
    if "foo" != "bar":
        print("if in elif")
    elif 1 != 0:
        print("bla")
    else:
        print("bar")
else:
    print("abc") # print abc
    print("nextline")
    print("another")
    if 3 == 4:
        print("if in if")
    elif 4 == 5:
        print("elif in if/else")
    else:
        print("else in if in if")
    # after everything
