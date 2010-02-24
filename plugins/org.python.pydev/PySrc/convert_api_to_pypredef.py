import os, sys
    
#===================================================================================================
# Container
#===================================================================================================
class Container(object):
    
    def __init__(self):
        self.contents = {}
    
    def get(self, s):
        return self.contents.setdefault(s, Class(s))
        
    def indent(self, s):
        return '\n'.join('    '+line for line in s.splitlines())
            

#===================================================================================================
# Class
#===================================================================================================
class Class(Container):
        
    def __init__(self, name):
        self.name = name
        Container.__init__(self)
    
    def __str__(self):
        str_contents = []
        for _key, content in sorted(self.contents.iteritems()):
            str_contents.append(self.indent(str(content)))
        
        if not str_contents:
            str_contents.append(self.indent('pass'))
            
        
        return \
'''
class %s:
%s
''' % (self.name, '\n'.join(str_contents))
    
#===================================================================================================
# Method
#===================================================================================================
class Method(Container):
    
    def __init__(self, name, signature):
        self.name = name
        self.signature = signature
        Container.__init__(self)
        
                
    def __str__(self):
        signature = self.signature
        ret = ''
        has_self = False
        docstring = ''
        
        if signature.startswith('('):
            signature = signature[1:]
            
        if signature.startswith('self, '):
            signature = signature[len('self, '):]
            has_self = True
            
        if '->' in signature:
            signature, ret = signature.split('->')
            signature = signature.strip()
            ret = ret.strip()
            
        
        if ' ' not in signature:
            if '()' in signature:
                signature= 'param='+signature
            elif '.' in signature:
                if '=' not in signature:
                    signature = 'param='+signature
                else:
                    split = signature.split('=')
                    if '.' in split[0]:
                        split[0] = split[0].replace('.', '_')
                        signature = '='.join(split)
                
            signature_rep = '('+signature
        else:
            signature_rep = ''
            splitted = signature.split(', ')
            size = len(splitted)
            for i, s in enumerate(splitted):
                parts = s.split(' ')
                type, s = ' '.join(parts[:-1]), parts[-1]
                
                if not s.strip():
                    s = 'param'
                if '.' in s:
                    if '=' not in s:
                        s = 'param='+s
                    else:
                        split = s.split('=')
                        if '.' in split[0]:
                            split[0] = split[0].replace('.', '_')
                            s = '='.join(split)
                    
                if i == size-1:
                    if not s.endswith(')'):
                        s += ')'
                        if type.endswith(')'):
                            type = type[:-1]
                
                if type:
                    #Add to docstring
                    param = s.strip()
                    if '=' in param:
                        param = param.split('=')[0].strip()
                    if param.endswith(':'):
                        param = param[:-1]
                    if param.endswith(')'):
                        param = param[:-1]
                        
                    docstring+= '\n    @type %s: %s' % (param, type)
                    
                if signature_rep and not signature_rep.strip().endswith(','):
                    signature_rep += ', '
                elif signature_rep.endswith(','):
                    signature_rep += ' '
                signature_rep += s
                
            if has_self:
                signature_rep = '(self, '+signature_rep
            else:
                signature_rep = '('+signature_rep
            
            
        if ret:
            if ',' in ret:
                splitted = ret.split(',')
                new = ''
                for s in splitted:
                    if new:
                        new += ', '
                    new += s.strip().replace(' ', '_')
                ret = new
            elif ' ' in ret.strip():
                ret = ret.strip().replace(' ', '_')
        
        
        string_rep = \
"""
def %s%s:
    '''
    %s
    '''
    """ % (self.name, signature_rep, docstring)
    
        if ret:
            string_rep += 'return '+ret
            
        val = string_rep.replace('from,', 'from_,').\
            replace('from=', 'from_=').\
            replace('exec(', 'exec_(').\
            replace('in)', 'in_)').\
            replace('...', '___').\
            replace('(1)', '(one)')
        return val
    
#===================================================================================================
# Attribute
#===================================================================================================
class Attribute(Container):
    def __init__(self, name, type):
        self.name = name
        self.type = type
        Container.__init__(self)
        
    def __str__(self):
        return '%s = %s' % (self.name, self.type)
    
    
#===================================================================================================
# Module
#===================================================================================================
class Module(Container):
    def __init__(self):
        self.contents = {}
    
    def AddString(self, before, after):
        splitted = before.split('.')
        prev = self
        for part in splitted[:-1]:
            prev = prev.get(part)
            
        if after == '10': #integer constant
            prev.contents.setdefault(splitted[-1], Attribute(splitted[-1], 'int'))
            
        elif after.startswith('4('): #4 means method
            after = after[1:]
            prev.contents.setdefault(splitted[-1], Method(splitted[-1], after))
            
            
        elif after.startswith('1('): #1 means constructor
            after = after[1:]
            if not after.startswith('(self'):
                return #Constructor method (ignore as we only want the __init__)

            #setdefault because we don't want to override it if one declaration is already there
            prev.contents.setdefault(splitted[-1], Method(splitted[-1], after))
            
        elif after.startswith('7'): #1 means constructor
            pass #occurrence: PyQt4.QtCore.QObject.staticMetaObject?7
                
        else:
            raise AssertionError('Not treated: '+before+after)
        
    def __str__(self):
        ret = []
        for _key, content in sorted(self.contents.iteritems()):
            ret.append(str(content))
        return '\n'.join(ret)
            

#===================================================================================================
# Convert
#===================================================================================================
def Convert(api_file, parts_for_module):
    f = open(api_file, 'r')
    try:
        lines = f.readlines()
    finally:
        f.close()
        
    found = set()
    for line in lines:
        contents = line.split('.')
        if len(contents) >= parts_for_module:
            found.add('.'.join(contents[:2]))
    
    for handle_module in sorted(found):
        module = Module()
        
        for line in lines:
            if line.startswith(handle_module+'.'):
                line = line[len(handle_module+'.'):].strip()
                before, after = line.split('?')
                
                module.AddString(before, after)
        
        final_contents = '''"""Automatically generated file from %s (QScintilla API file)

Note that the generated code must be Python 3.0 compatible to be used in a .pypredef file.

Imports should not be used (if a class is used from another module,
it should be completely redeclared in a .pypredef file)

The name of the file should be a direct representation of the module name 
(i.e.: a PyQt4.QtCore.pypredef file represents a PyQt4.QtCore module)
"""
'''
        target = handle_module+'.pypredef'
        print('Writing contents for: %s to: %s' % (handle_module, target))
        final_contents += str(module)
        
        f = open(target, 'w')        
        try:
            f.write(final_contents)
        finally:
            f.close()
            
        
#===================================================================================================
# main
#===================================================================================================
if __name__ == '__main__':
    args = sys.argv[1:]
    if len(args) < 2:
        print(
            'Expected the first parameter to be the QScintilla .api file\n'
            'and the second parameter is the number of strings which \n'
            'define a module\n\n')
        
        print('E.g.: python convert_api_to_pypredef.py PyQt4.api 2')
    else:
        api_file = args[0]
        assert os.path.exists(api_file), 'File: %s does not exist.' % (api_file,)
        parts_for_module = int(args[1])
        assert parts_for_module >= 1, 'At least the 1st part must define a module.'
        Convert(api_file, parts_for_module)
        
            