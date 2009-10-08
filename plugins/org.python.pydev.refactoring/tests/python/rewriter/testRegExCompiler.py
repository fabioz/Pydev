import re
# These statements don't require a AST regular expression node
m = re.search('(?<=-)\w+', 'spam-egg')
print m.group(0)
re.compile("a").match("ba", 1)
re.split('(\W+)', 'Words, words, words.')
re.sub(r'def\s+([a-zA-Z_][a-zA-Z_0-9]*)\s*\(\s*\):', r'static PyObject*\npy_\1(void)\n{', 'def myfunc():')
pattern = '^M?M?M?$'
re.search(pattern, 'M')
def first_match(s, regexList):
    for match in (regex.search(s) for regex in regexList):
        if match:
            return match
        # some comment after first_match

##r

import re
# These statements don't require a AST regular expression node
m = re.search('(?<=-)\w+', 'spam-egg')
print m.group(0)
re.compile("a").match("ba", 1)
re.split('(\W+)', 'Words, words, words.')
re.sub(r'def\s+([a-zA-Z_][a-zA-Z_0-9]*)\s*\(\s*\):', r'static PyObject*\npy_\1(void)\n{', 'def myfunc():')
pattern = '^M?M?M?$'
re.search(pattern, 'M')
def first_match(s, regexList):
    for match in (regex.search(s) for regex in regexList):
        if match:
            return match
        # some comment after first_match
    

   