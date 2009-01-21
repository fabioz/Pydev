'''
Test for regular expressions in java (result of an interactive session)
'''

from java.util.regex import Pattern
from java.lang import String
p = Pattern.compile("coding[:=]+[\\s]*[\\w[\\-]]+[\\s]*")

assert p.matcher(String('coding:foo')).find()
assert p.matcher(String('coding: foo ')).find()
assert p.matcher(String('coding:foo_1')).find()
assert p.matcher(String('coding:foo-1')).find()
assert p.matcher(String('coding:foo_1')).find()
assert not p.matcher(String('coding foo')).find()
assert not p.matcher(String('encoding foo')).find()
assert not p.matcher(String('coding')).find()
assert not p.matcher(String('coding')).find()
