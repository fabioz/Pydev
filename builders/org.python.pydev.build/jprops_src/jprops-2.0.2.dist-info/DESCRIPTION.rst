======
jprops
======

Parser for `Java .properties files <https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html>`_.

Supports Python 2.6, 2.7, and 3.3+

Installation
============

Use pip to install jprops from PyPI::

  pip install jprops

Usage
=====

Reading properties
------------------

Use ``jprops.load_properties`` to read a properties file and return a normal
Python ``dict``::

  import jprops
  with open('mine.properties') as fp:
    properties = jprops.load_properties(fp)

You can provide a custom "mapping" to load the properties into a different data
strucuture. For example, if you would like to keep the properties in the same
order they originally appeared, you can use an "ordered dict", such as
``collections.OrderedDict``. The "mapping" can be any type or function that
accepts an iterable of (key, value) pairs::

  import collections
  with open('mine.properties') as fp:
    properties = jprops.load_properties(fp, collections.OrderedDict)

``load_properties`` is just a wrapper to ``iter_properties``, which you can use
directly if you want to process the properties lazily without loading them all
into a data structure::

  with open('mine.properties') as fp:
    for key, value in jprops.iter_properties(fp):
      if key.startswith('foo'):
        print key, value

Writing properties
------------------

Use ``jprops.store_properties`` to write a ``dict``, dict-like object, or any
iterable of (key, value) pairs to a file::

  x = {'y': '1', 'z': '2'}
  with open('out.properties', 'w') as fp:
    jprops.store_properties(fp, x)

By default jprops follows the Java convention of writing a timestamp comment to
the beginning of the file, like::

  #Thu Oct 06 19:08:50 EDT 2011
  y=1
  z=2

You can suppress writing the timestamp comment by passing ``timestamp=False``.

You can provide a custom header comment that appears before the timestamp.
Multi-line comments are handled appropriately, continuing the comment across
lines::

  jprops.store_properties(fp, {'x': '1'}, comment='Hello\nworld!')

::

  #Hello
  #world!
  #Thu Oct 06 19:17:21 EDT 2011
  x=1

You can also use ``write_comment`` and ``write_property`` for finer-grained
control over writing a properties file::

  with open('out.properties', 'w') as fp:
    jprops.write_comment(fp, 'the hostname:')
    jprops.write_property(fp, 'host', 'localhost')
    jprops.write_comment(fp, 'the port number:')
    jprops.write_property(fp, 'port', '443')

::

  #the hostname:
  host=localhost
  #the port number:
  port=443

Comments
--------

By default, comments in the input will be ignored, but they can be included by
``iter_properties`` by passing ``comments=True``. The comments will be included
with ``jprops.COMMENT`` as a sentinal value in place of the key::

  with open('in.properties') as fp:
    props = list(jprops.iter_properties(fp, comments=True))
  for k, v in props:
    if k is jprops.COMMENT:
      print 'comment:', v

``jprops`` doesn't include any special data structures for preserving comments,
but you can manipulate the properties before writing them back out. For example
this is one simple pattern for altering properties while writing the ouput::

  updates = {'one': '1', 'two': '2', 'to_remove': None}

  with open('out.properties', 'w') as fp:
    for key, value in props:
      # updates.pop will return and remove the value for the key, or return
      # the original `value` if it doesn't exist
      value = updates.pop(key, value)
      # skip keys set to `None` in `updates`
      if value is not None:
        # write_property handles jprops.COMMENT as the key so you don't have to
        # check whether to use write_comment
        jprops.write_property(fp, key, value)
    # since the existing keys have already been popped, use store_properties
    # to write the remaining updates
    jprops.store_properties(fp, updates, timestamp=False)

File encodings and Unicode
--------------------------

Files opened in binary mode such as ``open(filename, 'rb')`` or
``open(filename, 'wb')`` will use the ``latin-1`` encoding and escape unicode
characters in the format ``\uffff`` for compatibility with the Java
``Properties`` byte stream encoding.

Starting with version 2.0, files opened with other text encodings are also
supported::

  with io.open('sample.properties', encoding='utf-8') as fp:
    props = jprops.load_properties(fp)

This works with the built-in ``open`` function, ``codecs.open``, or ``io.open``.
Other file-like objects that extend ``io.TextIOBase`` or have a non-empty
``encoding`` property will be read or written as unicode text values, otherwise
they will be considered binary and read or written as ``latin-1`` encoded bytes.

Authors
=======

Matt Good (matt@matt-good.net)


Changes
=======

2.0.2 (2017-04-21)
------------------

* Bump version in setup.py before release

2.0.1 (unreleased)
----------------

* Fix over-escaping in values

2.0 (2017-04-08)
----------------

* Support files opened with text encodings
* Nice repr for ``jprops.COMMENT``

1.0 (2013-06-12)
----------------

* Python 3.3 support
* More informative error when trying to write a non-string value

0.2 (2012-05-02)
----------------

* Handle Windows or Mac line endings


0.1 (2011-10-07)
----------------

Initial release.


