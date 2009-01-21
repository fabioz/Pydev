# before
from __future__ import with_statement # on-line
# before
from __future__ import division as foovision # on-line
# after
from __future__ import with_statement as foostatement, generators as barstatement
# before
from cmath import * # on-line
import os, codecs
# comment after
import os, codecs
# before
import httplib as httpfoo, array as arraybar # on-line
# after
import samplepackage.fibo # on-line
# after
samplepackage.fib(100)


