"""test access to undefined variables"""

__revision__ = '$Id:'

defined = 1

if defined != 1:
    if defined in (unknown, defined):
        defined += 1


def in_method(var):
    """method doc"""
    var = nomoreknown
    assert var

defined = {defined:__revision__}
defined[__revision__] = other = 'move this is astng test'

other += '$'

def bad_default(var, default=unknown2):
    """function with defaut arg's value set to an unexistant name"""
    print var, default
