import io
import re
import string
import sys
import time


PY2 = sys.version_info[0] == 2
if not PY2:
  text_type = str
  string_types = (str,)
  unichr = chr
else:
  text_type = unicode
  string_types = (str, unicode)
  unichr = unichr


class _CommentSentinel(object):
  __slots__ = ()
  def __repr__(self):
    return 'jprops.COMMENT'
  def __copy__(self):
    return self
  def __deepcopy__(self, memo):
    return self
  def __reduce__(self):
    return 'COMMENT'


COMMENT = _CommentSentinel()


def load_properties(fh, mapping=dict):
  """
    Reads properties from a Java .properties file.

    Returns a dict (or provided mapping) of properties.

    :param fh: a readable file-like object
    :param mapping: mapping type to load properties into
  """
  return mapping(iter_properties(fh))


def store_properties(fh, props, comment=None, timestamp=True):
  """
    Writes properties to the file in Java properties format.

    :param fh: a writable file-like object
    :param props: a mapping (dict) or iterable of key/value pairs
    :param comment: comment to write to the beginning of the file
    :param timestamp: boolean indicating whether to write a timestamp comment
  """
  w = _property_writer(fh)

  if comment is not None:
    w.write_comment(comment)

  if timestamp:
    w.write_comment(time.strftime('%a %b %d %H:%M:%S %Z %Y'))

  if hasattr(props, 'keys'):
    for key in props:
      w.write_property(key, props[key])
  else:
    for key, value in props:
      w.write_property(key, value)


def write_comment(fh, comment):
  """
    Writes a comment to the file in Java properties format.

    Newlines in the comment text are automatically turned into a continuation
    of the comment by adding a "#" to the beginning of each line.

    :param fh: a writable file-like object
    :param comment: comment string to write
  """
  _property_writer(fh).write_comment(comment)


def write_property(fh, key, value):
  """
    Write a single property to the file in Java properties format.

    :param fh: a writable file-like object
    :param key: the key to write
    :param value: the value to write
  """
  _property_writer(fh).write_property(key, value)


def iter_properties(fh, comments=False):
  """
    Incrementally read properties from a Java .properties file.

    Yields tuples of key/value pairs.

    If ``comments`` is `True`, comments will be included with ``jprops.COMMENT``
    in place of the key.

    :param fh: a readable file-like object
    :param comments: should include comments (default: False)
  """
  for line in _property_lines(fh):
    key, value = _split_key_value(line)
    if key is not COMMENT:
      key = _unescape(key)
    elif not comments:
      continue
    yield key, _unescape(value)


################################################################################
# Helpers for property parsing/writing
################################################################################


_COMMENT_CHARS = u'#!'
_LINE_PATTERN = re.compile(r'^\s*(?P<body>.*?)(?P<backslashes>\\*)$')
_KEY_TERMINATORS_EXPLICIT = u'=:'
_KEY_TERMINATORS = _KEY_TERMINATORS_EXPLICIT + string.whitespace
_COMMENT_UNICODE_ESCAPE = re.compile(u'[\u0100-\uffff]')
_PROPERTY_UNICODE_ESCAPE = re.compile(u'[\u0000-\u0019\u007f-\uffff]')


_escapes = {
  't': '\t',
  'n': '\n',
  'f': '\f',
  'r': '\r',
}
_escapes_rev = dict((v, '\\' + k) for k,v in _escapes.items())
for c in '\\' + _COMMENT_CHARS + _KEY_TERMINATORS_EXPLICIT:
  _escapes_rev.setdefault(c, '\\' + c)


def _unescape(value):
  def unirepl(m):
    backslashes = m.group(1)
    charcode = m.group(2)

    # if preceded by even number of backslashes, the \u is escaped
    if len(backslashes) % 2 == 0:
      return m.group(0)

    c = unichr(int(charcode, 16))
    # if unicode decodes to '\', re-escape it to unescape in the second step
    if c == '\\':
      c = u'\\\\'

    return backslashes + c

  value = re.sub(r'(\\+)u([0-9a-fA-F]{4})', unirepl, value)

  def bslashrepl(m):
    code = m.group(1)
    return _escapes.get(code, code)

  value = re.sub(r'\\(.)', bslashrepl, value)

  # if not native string (e.g. PY2) try converting it back
  if not isinstance(value, str):
    try:
      value = value.encode('ascii')
    except UnicodeEncodeError:
      # cannot be represented in ASCII so leave it as unicode type
      pass

  return value


def _escape_comment(comment):
  comment = comment.replace('\r\n', '\n').replace('\r', '\n')
  comment = re.sub(r'\n(?![#!])', '\n#', comment)
  return u'#' + comment


def _escape_key(key):
  return _escape(key, _KEY_TERMINATORS)


def _escape_value(value):
  tail = value.lstrip()
  if len(tail) == len(value):
    return _escape(value)

  if tail:
    head = value[:-len(tail)]
  else:
    head = value

  # escape any leading whitespace, but leave other spaces intact
  return _escape(head, string.whitespace) + _escape(tail)


def _escape(value, chars=''):
  escape_chars = set(_escapes_rev)
  escape_chars.update(chars)
  escape_pattern = '[%s]' % re.escape(''.join(escape_chars))

  def esc(m):
    c = m.group(0)
    return _escapes_rev.get(c) or '\\' + c
  value = re.sub(escape_pattern, esc, value)

  return value


def _unicode_replace(m):
  c = m.group(0)
  return r'\u%.4x' % ord(c)


def _split_key_value(line):
  if line[0] in _COMMENT_CHARS:
    return COMMENT, line[1:]

  escaped = False
  key_buf = io.StringIO()

  for idx, c in enumerate(line):
    if not escaped and c in _KEY_TERMINATORS:
      key_terminated_fully = c in _KEY_TERMINATORS_EXPLICIT
      break

    key_buf.write(c)
    escaped = c == u'\\'

  else:
    # no key terminator, key is full line & value is blank
    return line, u''

  value = line[idx+1:].lstrip()
  if not key_terminated_fully and value[:1] in _KEY_TERMINATORS_EXPLICIT:
    value = value[1:].lstrip()

  return key_buf.getvalue(), value


def _is_text_file(fp):
  return (
    isinstance(fp, io.TextIOBase)
    or getattr(fp, 'encoding', None) is not None
  )


def _read_lines(fp):
  lines = iter(fp)
  if not _is_text_file(fp):
    lines = (line.decode('latin-1') for line in lines)

  # if file was not opened with universal newline support convert the newlines
  if 'U' not in getattr(fp, 'mode', ''):
    lines = _universal_newlines(lines)

  return lines


def _universal_newlines(lines):
  for line in lines:
    line = line.replace('\r\n', '\n').replace('\r', '\n')
    for piece in line.split('\n'):
      yield piece


def _property_lines(fp):
  buf = io.StringIO()
  for line in _read_lines(fp):
    m = _LINE_PATTERN.match(line)

    body = m.group('body')
    backslashes = m.group('backslashes')

    if len(backslashes) % 2 == 0:
      body += backslashes
      continuation = False
    else:
      body += backslashes[:-1]
      continuation = True

    if not body:
      continue

    buf.write(body)

    if not continuation:
      yield buf.getvalue()
      buf = io.StringIO()


def _property_writer(fh):
  if _is_text_file(fh):
    return _TextPropertyWriter(fh)
  else:
    return _BytesPropertyWriter(fh)


def _require_string(value, name):
  if isinstance(value, text_type):
    return value

  if isinstance(value, string_types):
    # allow Python 2 native strings
    return value.decode('latin-1')

  valid_types = ' or '.join(cls.__name__ for cls in string_types)
  raise TypeError('%s must be %s, but got: %s %r'
                  % (name, valid_types, type(value), value))


class _TextPropertyWriter(object):
  _escape_comment = staticmethod(_escape_comment)
  _escape_key = staticmethod(_escape_key)
  _escape_value = staticmethod(_escape_value)

  def __init__(self, fp):
    self.fp = fp

  def write_property(self, key, value):
    if key is COMMENT:
      self.write_comment(value)
      return

    key = _require_string(key, 'keys')
    value = _require_string(value, 'values')

    key = self._escape_key(key)
    value = self._escape_value(value)

    self._write(key)
    self._write(u'=')
    self._write(value)
    self._write(u'\n')

  def write_comment(self, comment):
    comment = _require_string(comment, 'comments')
    comment = self._escape_comment(comment)
    self._write(comment)
    self._write(u'\n')

  def _write(self, data):
    self.fp.write(data)


class _BytesPropertyWriter(_TextPropertyWriter):
  def _write(self, data):
    self.fp.write(data.encode('latin-1'))

  def _escape_comment(self, comment):
    comment = _TextPropertyWriter._escape_comment(comment)
    return _COMMENT_UNICODE_ESCAPE.sub(_unicode_replace, comment)

  def _escape_key(self, key):
    key = _TextPropertyWriter._escape_key(key)
    return _PROPERTY_UNICODE_ESCAPE.sub(_unicode_replace, key)

  def _escape_value(self, value):
    value = _TextPropertyWriter._escape_value(value)
    return _PROPERTY_UNICODE_ESCAPE.sub(_unicode_replace, value)
