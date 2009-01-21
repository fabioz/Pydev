varname = "foo"
exec varname + '=23'
print foo
globaldict = {}
localdict = {}
exec 'FOOCONST=24' in globaldict
exec varname + '=24' in globaldict, localdict
print "globals: ", globaldict
print "locals: ", localdict