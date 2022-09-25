
import import_package.bar
import import_package.foo
import import_package.baz as baz_alias
from import_package.qux import Qux
from import_package.quux import Quux as Xuuq
from import_package import sub_package

baz_alias ## type baz
import_package ## type import_package
sub_package ## type sub_package
import_package.foo ## type foo
import_package.bar ## type bar

x1 = baz_alias
x2 = import_package.bar

x1 ## type baz
x2 ## type bar

x = baz_alias.Baz()
x ## type Baz
print(x.path())


q = Qux()
q ## type Qux
aq = Xuuq()
aq ## type Quux

