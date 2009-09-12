import bla as bla_alias
from bla.bar import hullu as foo
from bla import bar as myfoofile
from bla.sub.foo import foohullu as foohullu_alias

class bar(foo):

    hullu = "hullu"
    
    class nested:

        mybar = "123"
    
class foobar(myfoofile.hullu):
    pass
        
##c

<config>
  <resolveNames>
    <string>foo</string>
    <string>bla_alias</string>
    <string>myfoofile.hullu</string>
  </resolveNames>
</config>


##r
# foo -> bla.bar.hullu
# bla_alias -> bla.
# myfoofile.hullu -> bla.bar.hullu
# Imported regular modules (Alias, Realname)
# bla_alias bla
# AliasToIdentifier (Module, Realname, Alias)
# bla.bar hullu foo
# bla bar myfoofile
# bla.sub.foo foohullu foohullu_alias