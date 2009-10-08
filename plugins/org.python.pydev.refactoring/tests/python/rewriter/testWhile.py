# before
i = 0
while (i < 2): # while test comment on-line
    a = 10
    print "under 5"
    if (i < 3):
        i += 1
    # this comment is printed twice...
else: # else on-line
    print "bigger" # print on-line

# after the second body (but actually in the module node)!
while (i < 2): # while test comment on-line
    print "under 5"
    i += 1
    # .. and this one disappears (strange hm?)
else: # else on-line
    print "bigger" # print on-line

# after the second body (but actually in the module node)!
print "foo"
# test
while (i < 2):
    print i
    i += 1

print a, "is global"