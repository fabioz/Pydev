print"foo"
print "foo", 
print "foo"
print("foo")
print ("foo", test("bla"))
fileobj = open('log', 'a')
print >> fileobj, "foo"
print >> fileobj
fileobj.close()
remove('log')

##r

print "foo"
print "foo"
print "foo"
print ("foo")
print ("foo", test("bla"))
fileobj = open('log', 'a')
print >> fileobj, "foo"
print >> fileobj
fileobj.close()
remove('log')
