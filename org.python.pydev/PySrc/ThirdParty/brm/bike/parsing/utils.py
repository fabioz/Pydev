# get the first element of a fully qualified python path
#(e.g. _car('a.b.c.d') = 'a')
def fqn_car(fqn):
    try:
        return fqn[:fqn.index(".")]
    except ValueError:   # i.e. no dots in fqn
        return fqn

# get the other elements of a fully qualified python path
#(e.g. _cdr('a.b.c.d') = 'b.c.d')
def fqn_cdr(fqn):
    try:
        return fqn[fqn.index(".")+1:]
    except ValueError:   # i.e. no dots in fqn
        return ""

# reverse of above _rcar("a.b.c.d") = "d"
def fqn_rcar(fqn):
    try:
        return fqn[fqn.rindex(".")+1:]
    except ValueError:   # i.e. no dots in fqn
        return fqn


# reverse of above _rcdr("a.b.c.d") = "a.b.c"
def fqn_rcdr(fqn):
    try:
        return fqn[:fqn.rindex(".")]
    except ValueError:   # i.e. no dots in fqn
        return ""
