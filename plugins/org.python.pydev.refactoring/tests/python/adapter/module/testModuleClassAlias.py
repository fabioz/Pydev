import bla as blaAlias
from bla.bar import hullu as foo
from bla import bar as myfoofile
from bla.sub.foo import foohullu as foohulluAlias

class bar(foo):

    hullu = "hullu"
    
    class nested:

        mybar = "123"
    
class foobar(myfoofile.hullu):
    pass
        
##c
'''
<config>
  <resolveNames>
    <string>foo</string>
    <string>blaAlias</string>
    <string>myfoofile.hullu</string>
  </resolveNames>
</config>
'''

##r
# foo -> bla.bar.hullu
# blaAlias -> bla.
# myfoofile.hullu -> bla.bar.hullu
# Imported regular modules (Alias, Realname)
# blaAlias bla
# AliasToIdentifier (Module, Realname, Alias)
# bla.bar hullu foo
# bla bar myfoofile
# bla.sub.foo foohullu foohulluAlias