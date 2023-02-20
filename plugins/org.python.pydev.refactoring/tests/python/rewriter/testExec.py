varname = "foo"
exec(varname + '=23')
print(foo)
globaldict = {}
localdict = {}
exec('FOOCONST=24', globaldict)
exec(varname + '=24', globaldict, localdict)
print("globals: ", globaldict)
print("locals: ", localdict)