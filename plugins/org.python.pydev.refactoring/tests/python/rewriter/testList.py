# before
['list', {}] # on-line
# after
# before
alist = ['foobar', [1, 2, 3.323], 333, {}] # on-line
# after
blist = []
clist = [alist, blist]
print(alist)
[ #Comment inside list
    1, #Another one
    2, #Final
] #Finish it