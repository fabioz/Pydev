i = 0
while True:
    print("foo")
    if i % 3 == 1:
        break
    else:
        print("else")
        i += 1
        # after i += 1
        print("foo before continue")
        # before continue
        continue # on-line continue
        # after continue
        print("foo after continue")
        print("foo after continue")

    # after while body
print("bar")