import httplib as foo
from httplib import HTTP as bar
from httplib import HTTP_PORT as port

class a(bar):
    pass

class b(foo.HTTPConnection):
    pass

print(port)
       
##c

<config>
  <resolveNames>
    <string>bar</string>
    <string>foo.HTTPConnection</string>
    <string>port</string>
  </resolveNames>
</config>

       
##r
# bar -> httplib.HTTP
# foo.HTTPConnection -> httplib.HTTPConnection
# port -> httplib.HTTP_PORT
# Imported regular modules (Alias, Realname)
# foo httplib
# AliasToIdentifier (Module, Realname, Alias)
# httplib HTTP bar
# httplib HTTP_PORT port