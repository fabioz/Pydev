# before
(0, ) # not a simple expression!
# after
(1, 2, 3) # on-line
# after
1, 2, 3
a = ()
()
# tuple in call
print(((1), 2))
print(((1, 3), 2))
print(1, (3, 2))
print(1, 2)
print((1), 2)
print(1, 3, 2)
print(1, (3, 2))
print((1,2,(3)))

##r

# before
(0, ) # not a simple expression!
# after
(1, 2, 3) # on-line
# after
(1, 2, 3)
a = ()
()
# tuple in call
print(((1), 2))
print(((1, 3), 2))
print(1, (3, 2))
print(1, 2)
print((1), 2)
print(1, 3, 2)
print(1, (3, 2))
print((1, 2, (3)))