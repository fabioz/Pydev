{}
# before (in module)
{} #on-line
#after (first dic)
{} # on-line
#after second dict
a = {}
b = {'foo':2, 1.24:23, 2:1, """bla""":4}
print(b[1.24])
print(b["""bla"""])
print(b[2])
print(b['foo'])
c = {'bar':{42:1, type(''):2}, 'bla':[]}