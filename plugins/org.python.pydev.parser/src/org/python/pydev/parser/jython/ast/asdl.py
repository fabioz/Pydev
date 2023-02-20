"""An implementation of the Zephyr Abstract Syntax Definition Language.

See http://asdl.sourceforge.net/ and
http://www.cs.princeton.edu/~danwang/Papers/dsl97/dsl97-abstract.html.

Only supports top level module decl, not view.  I'm guessing that view
is intended to support the browser and I'm not interested in the
browser.
"""

# __metaclass__ = type

import os
import traceback

import spark


class Token:

    # spark seems to dispatch in the parser based on a token's
    # type attribute
    def __init__(self, type, lineno):
        self.type = type
        self.lineno = lineno

    def __str__(self):
        return self.type

    def __repr__(self):
        return str(self)


class Id(Token):

    def __init__(self, value, lineno):
        self.type = 'Id'
        self.value = value
        self.lineno = lineno

    def __str__(self):
        return self.value


class ASDLSyntaxError:

    def __init__(self, lineno, token=None, msg=None):
        self.lineno = lineno
        self.token = token
        self.msg = msg

    def __str__(self):
        if self.msg is None:
            return "Error at '%s', line %d" % (self.token, self.lineno)
        else:
            return "%s, line %d" % (self.msg, self.lineno)


class ASDLScanner(spark.GenericScanner, object):

    def tokenize(self, input):
        self.rv = []
        self.lineno = 1
        super(ASDLScanner, self).tokenize(input)
        return self.rv

    def t_id(self, s):
        r"[\w\.]+"
        # XXX doesn't distinguish upper vs. lower, which is
        # significant for ASDL.
        self.rv.append(Id(s, self.lineno))

    def t_xxx(self, s):  # not sure what this production means
        r"<="
        self.rv.append(Token(s, self.lineno))

    def t_punctuation(self, s):
        r"[\{\}\*\=\|\(\)\,\?\:]"
        self.rv.append(Token(s, self.lineno))

    def t_comment(self, s):
        r"\-\-[^\n]*"
        pass

    def t_newline(self, s):
        r"\n"
        self.lineno += 1

    def t_whitespace(self, s):
        r"[ \t]+"
        pass

    def t_default(self, s):
        r" . +"
        raise ValueError("unmatched input: %s" % s)


class ASDLParser(spark.GenericParser, object):

    def __init__(self):
        super(ASDLParser, self).__init__("module")

    def typestring(self, tok):
        return tok.type

    def error(self, tok):
        raise ASDLSyntaxError(tok.lineno, tok)

    def p_module_0(self, tup):
        " module ::= Id Id { } "
        module, name, _0, _1 = tup
        if module.value != "module":
            raise ASDLSyntaxError(module.lineno,
                                  msg="expected 'module', found %s" % module)
        return Module(name, None)

    def p_module(self, tup):
        " module ::= Id Id { definitions } "
        module, name, _0, definitions, _1 = tup
        if module.value != "module":
            raise ASDLSyntaxError(module.lineno,
                                  msg="expected 'module', found %s" % module)
        return Module(name, definitions)

    def p_definition_0(self, tup):
        " definitions ::= definition "
        definition = tup[0]
        return definition

    def p_definition_1(self, tup):
        " definitions ::= definition definitions "
        definitions, definition = tup
        return definitions + definition

    def p_definition(self, tup):
        " definition ::= Id = type "
        id, _, type = tup
        return [Type(id, type)]

    def p_type_0(self, tup):
        " type ::= product "
        product = tup[0]
        return product

    def p_type_1(self, tup):
        " type ::= sum "
        sum = tup[0]
        return Sum(sum)

    def p_type_2(self, tup):
        " type ::= sum Id ( fields ) "
        sum, id, _0, attributes, _1 = tup
        if id.value != "attributes":
            raise ASDLSyntaxError(id.lineno,
                                  msg="expected attributes, found %s" % id)
        return Sum(sum, attributes)

    def p_product(self, tup):
        " product ::= ( fields ) "
        # XXX can't I just construct things in the right order?
        _0, fields, _1 = tup
        fields.reverse()
        return Product(fields)

    def p_sum_0(self, tup):
        " sum ::= constructor """
        constructor = tup[0]
        return [constructor]

    def p_sum_1(self, tup):
        " sum ::= constructor | sum "
        constructor, _, sum = tup
        return [constructor] + sum

    def p_sum_2(self, tup):
        " sum ::= constructor | sum "
        constructor, _, sum = tup
        return [constructor] + sum

    def p_constructor_0(self, tup):
        " constructor ::= Id "
        id = tup[0]
        return Constructor(id)

    def p_constructor_1(self, tup):
        " constructor ::= Id ( fields ) "
        id, _0, fields, _1 = tup
        # XXX can't I just construct things in the right order?
        fields.reverse()
        return Constructor(id, fields)

    def p_fields_0(self, tup):
        " fields ::= field "
        field = tup[0]
        return [field]

    def p_fields_1(self, tup):
        " fields ::= field , fields "
        field, _, fields = tup
        return fields + [field]

    def p_field_0(self, tup):
        " field ::= Id "
        type = tup[0]
        return Field(type)

    def p_field_1(self, tup):
        " field ::= Id Id "
        type, name = tup
        return Field(type, name)

    def p_field_2(self, tup):
        " field ::= Id * Id "
        type, _, name = tup
        return Field(type, name, seq=1)

    def p_field_3(self, tup):
        " field ::= Id ? Id "
        type, _, name = tup
        return Field(type, name, opt=1)

    def p_field_4(self, tup):
        " field ::= Id * "
        type, _ = tup
        return Field(type, seq=1)

    def p_field_5(self, tup):
        " field ::= Id ? "
        type, _ = tup
        return Field(type, opt=1)


builtin_types = ("identifier", "string", "int", "bool", "object", "ISpecialStr")

# below is a collection of classes to capture the AST of an AST :-)
# not sure if any of the methods are useful yet, but I'm adding them
# piecemeal as they seem helpful


class AST:
    pass  # a marker class


class Module(AST):

    def __init__(self, name, dfns):
        self.name = name
        self.dfns = dfns
        self.types = {}  # maps type name to value (from dfns)
        for type in dfns:
            self.types[type.name.value] = type.value

    def __repr__(self):
        return "Module(%s, %s)" % (self.name, self.dfns)


class Type(AST):

    def __init__(self, name, value):
        self.name = name
        self.value = value

    def __repr__(self):
        return "Type(%s, %s)" % (self.name, self.value)


class Constructor(AST):

    def __init__(self, name, fields=None):
        self.name = name
        self.fields = fields or []

    def __repr__(self):
        return "Constructor(%s, %s)" % (self.name, self.fields)


class Field(AST):

    def __init__(self, type, name=None, seq=0, opt=0):
        self.type = type
        self.name = name
        self.seq = seq
        self.opt = opt

    def __repr__(self):
        if self.seq:
            extra = ", seq=1"
        elif self.opt:
            extra = ", opt=1"
        else:
            extra = ""
        if self.name is None:
            return "Field(%s%s)" % (self.type, extra)
        else:
            return "Field(%s, %s,%s)" % (self.type, self.name, extra)


class Sum(AST):

    def __init__(self, types, attributes=None):
        self.types = types
        self.attributes = attributes or []

    def __repr__(self):
        if self.attributes is None:
            return "Sum(%s)" % self.types
        else:
            return "Sum(%s, %s)" % (self.types, self.attributes)


class Product(AST):

    def __init__(self, fields):
        self.fields = fields

    def __repr__(self):
        return "Product(%s)" % self.fields


class VisitorBase(object):

    def __init__(self, skip=0):
        self.cache = {}
        self.skip = skip

    def visit(self, object, *args):
        meth = self._dispatch(object)
        if meth is None:
            return
        try:
            meth(object, *args)
        except Exception as err:
            print("Error visiting", repr(object))
            print(err)
            traceback.print_exc()
            # XXX hack
            if hasattr(self, 'file'):
                self.file.flush()
            os._exit(1)

    def _dispatch(self, object):
        if not isinstance(object, AST):
            raise AssertionError('Expected AST. Found: %s (%s)' % (type(object), object))
        klass = object.__class__
        meth = self.cache.get(klass)
        if meth is None:
            methname = "visit" + klass.__name__
            if self.skip:
                meth = getattr(self, methname, None)
            else:
                meth = getattr(self, methname)
            self.cache[klass] = meth
        return meth


class Check(VisitorBase):

    def __init__(self):
        super(Check, self).__init__(skip=1)
        self.cons = {}
        self.errors = 0
        self.types = {}

    def visitModule(self, mod):
        for dfn in mod.dfns:
            self.visit(dfn)

    def visitType(self, type):
        self.visit(type.value, str(type.name))

    def visitSum(self, sum, name):
        for t in sum.types:
            self.visit(t, name)

    def visitConstructor(self, cons, name):
        key = str(cons.name)
        conflict = self.cons.get(key)
        if conflict is None:
            self.cons[key] = name
        else:
            print("Redefinition of constructor %s" % key)
            print("Defined in %s and %s" % (conflict, name))
            self.errors += 1
        for f in cons.fields:
            self.visit(f, key)

    def visitField(self, field, name):
        key = str(field.type)
        l = self.types.setdefault(key, [])
        l.append(name)

    def visitProduct(self, prod, name):
        for f in prod.fields:
            self.visit(f, name)


def check(mod):
    v = Check()
    v.visit(mod)

    for t in v.types:
        if t not in mod.types and not t in builtin_types:
            v.errors += 1
            uses = ", ".join(v.types[t])
            print("Undefined type %s, used in %s" % (t, uses))

    return not v.errors


def parse(file):
    scanner = ASDLScanner()
    parser = ASDLParser()

    buf = open(file).read()
    tokens = scanner.tokenize(buf)
    try:
        return parser.parse(tokens)
    except ASDLSyntaxError as err:
        print(err)
        lines = buf.split("\n")
        print(lines[err.lineno - 1])  # lines starts at 0, files at 1


if __name__ == "__main__":
    import glob
    import sys

    if len(sys.argv) > 1:
        files = sys.argv[1:]
    else:
        testdir = "tests"
        files = glob.glob(testdir + "/*.asdl")

    for file in files:
        print(file)
        mod = parse(file)
        print("module", mod.name)
        print(len(mod.dfns), "definitions")
        if not check(mod):
            print("Check failed")
        else:
            for dfn in mod.dfns:
                print(dfn.type)
