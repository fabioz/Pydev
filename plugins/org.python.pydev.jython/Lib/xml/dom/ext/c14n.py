#! /usr/bin/env python
'''XML Canonicalization

This module generates canonical XML, as defined in
    http://www.w3.org/TR/xml-c14n

It is limited in that it can only canonicalize an element and all its
children; general document subsets are not supported.
'''

_copyright = '''Copyright 2001, Zolera Systems Inc.  All Rights Reserved.
Distributed under the terms of the Python 2.0 Copyright or later.'''

from xml.dom import Node
from xml.ns import XMLNS
import re
try:
    import cStringIO
    StringIO = cStringIO
except:
    import StringIO

_attrs = lambda E: E.attributes or []
_children = lambda E: E.childNodes or []

def _sorter(n1, n2):
    '''Sorting predicate for non-NS attributes.'''
    i = cmp(n1.namespaceURI, n2.namespaceURI)
    if i: return i
    return cmp(n1.localName, n2.localName)

def _sorter_ns(n1, n2):
    '''Sorting predicate for NS attributes; "xmlns" always comes first.'''
    if n1.localName == 'xmlns': return -1
    if n2.localName == 'xmlns': return 1
    return cmp(n1.localName, n2.localName)
    
class _implementation:
    '''Implementation class for C14N.'''

    # Handlers for each node, by node type.
    handlers = {}

    # pattern/replacement list for whitespace stripping.
    repats = (
	( re.compile(r'[ \t]+'), ' ' ),
	( re.compile(r'[\r\n]+'), '\n' ),
    )

    def __init__(self, node, write, nsdict={}, stripspace=0, nocomments=1):
	'''Create and run the implementation.'''
	if node.nodeType != Node.ELEMENT_NODE:
	    raise TypeError, 'Non-element node'
	self.write, self.stripspace, self.nocomments = \
		write, stripspace, nocomments

	if nsdict == None or nsdict == {}:
	    nsdict = { 'xml': XMLNS.XML, 'xmlns': XMLNS.BASE }
	self.ns_stack = [ nsdict ]

	# Collect the initial list of xml:XXX attributes.
	xmlattrs = []
	for a in _attrs(node):
	    if a.namespaceURI == XMLNS.XML:
		n = a.localName
		xmlattrs.append(n)

	# Walk up and get all xml:XXX attributes we inherit.
	parent, inherited = node.parentNode, []
	while parent:
	    if parent.nodeType != Node.ELEMENT_NODE: break
	    for a in _attrs(parent):
		if a.namespaceURI != XMLNS.XML: continue
		n = a.localName
		if n not in xmlattrs:
		    xmlattrs.append(n)
		    inherited.append(a)
	    parent = parent.parentNode

	self._do_element(node, inherited)
	self.ns_stack.pop()

    def _do_text(self, node):
	'Process a text node.'
	s = node.data \
		.replace("&", "&amp;") \
		.replace("<", "&lt;") \
		.replace(">", "&gt;") \
		.replace("\015", "&#xD;")
	if self.stripspace:
	    for pat,repl in _implementation.repats: s = re.sub(pat, repl, s)
	if s: self.write(s)
    handlers[Node.TEXT_NODE] =_do_text
    handlers[Node.CDATA_SECTION_NODE] =_do_text

    def _do_pi(self, node):
	'''Process a PI node.  Since we start with an element, we're
	never a child of the root, so we never write leading or trailing
	#xA.
	'''
	W = self.write
	W('<?')
	W(node.nodeName)
	s = node.data
	if s:
	    W(' ')
	    W(s)
	W('?>')
    handlers[Node.PROCESSING_INSTRUCTION_NODE] =_do_pi

    def _do_comment(self, node):
	'''Process a comment node.  Since we start with an element, we're
	never a child of the root, so we never write leading or trailing
	#xA.
	'''
	if self.nocomments: return
	W = self.write
	W('<!--')
	W(node.data)
	W('-->')
    handlers[Node.COMMENT_NODE] =_do_comment

    def _do_attr(self, n, value):
	'Process an attribute.'
	W = self.write
	W(' ')
	W(n)
	W('="')
	s = value \
	    .replace("&", "&amp;") \
	    .replace("<", "&lt;") \
	    .replace('"', '&quot;') \
	    .replace('\011', '&#x9') \
	    .replace('\012', '&#xA') \
	    .replace('\015', '&#xD')
	W(s)
	W('"')

    def _do_element(self, node, initialattrlist = []):
	'Process an element (and its children).'
	name = node.nodeName
	W = self.write
	W('<')
	W(name)

	# Get parent namespace, make a copy for us to inherit.
	parent_ns = self.ns_stack[-1]
	my_ns = parent_ns.copy()

	# Divide attributes into NS definitions and others.
	nsnodes, others = [], initialattrlist[:]
	for a in _attrs(node):
	    if a.namespaceURI == XMLNS.BASE:
		nsnodes.append(a)
	    else:
		others.append(a)

	# Namespace attributes: update dictionary; if not already
	# in parent, output it.
	nsnodes.sort(_sorter_ns)
	for a in nsnodes:
	    # Some DOMs seem to rename "xmlns='xxx'" strangely
	    n = a.nodeName
	    if n == "xmlns:":
		key, n = "", "xmlns"
	    else:
		key = a.localName

	    v = my_ns[key] = a.nodeValue
	    pval = parent_ns.get(key, None)

	    if n == "xmlns" and v in [ '', XMLNS.BASE ] \
	    and pval in [ '', XMLNS.BASE ]:  
		# Default namespace set to default value.
		pass
	    elif v != pval:
		self._do_attr(n, v)

	# Other attributes: sort and output.
	others.sort(_sorter)
	for a in others: self._do_attr(a.nodeName, a.value)

	W('>')

	# Push our namespace dictionary, recurse, pop the dicionary.
	self.ns_stack.append(my_ns)
	for c in _children(node):
	    _implementation.handlers[c.nodeType](self, c)
	    # XXX Ignore unknown node types?
	    #handler = _implementation.handlers.get(c.nodeType, None)
	    #if handler: handler(self, c)
	self.ns_stack.pop()
	W('</%s>' % (name,))
    handlers[Node.ELEMENT_NODE] =_do_element

def Canonicalize(node, output=None, **kw):
    '''Canonicalize a DOM element node and everything underneath it.
    Return the text; if output is specified then output.write will
    be called to output the text and None will be returned
    Keyword parameters:
	stripspace -- remove extra (almost all) whitespace from text nodes
	nsdict -- a dictionary of prefix:uri namespace entries assumed
	    to exist in the surrounding context
	comments -- keep comments if non-zero (default is zero)
    '''

    if not output: s = StringIO.StringIO()
    _implementation(node,
	(output and output.write) or s.write,
	nsdict=kw.get('nsdict', {}),
	stripspace=kw.get('stripspace', 0),
	nocomments=kw.get('comments', 0) == 0,
    )
    if not output: return s.getvalue()

if __name__ == '__main__':
    text = '''<SOAP-ENV:Envelope xml:lang='en'
      xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"
      xmlns:SOAP-ENC="http://schemas.xmlsoap.org/soap/encoding/"
      xmlns:xsi="http://www.w3.org/2001/XMLSchemaInstance"
      xmlns:xsd="http://www.w3.org/2001/XMLSchemaZ" xmlns:spare='foo'
      SOAP-ENV:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
	<SOAP-ENV:Body xmlns='test-uri'><?MYPI spenser?>
	    <zzz xsd:foo='xsdfoo' xsi:a='xsi:a'/>
	    <SOAP-ENC:byte>44</SOAP-ENC:byte>        <!-- 1 -->
	    <Name xml:lang='en-GB'>This is the name</Name>Some
content here on two lines.
	    <n2><![CDATA[<greeting>Hello</greeting>]]></n2> <!-- 3 -->
	    <n3 href='z&amp;zz' xsi:type='SOAP-ENC:string'>
	    more content.  indented    </n3>
	    <a2 xmlns:f='z' xmlns:aa='zz'><i xmlns:f='z'>12</i><t>rich salz</t></a2> <!-- 8 -->
	</SOAP-ENV:Body>
      <z xmlns='myns' id='zzz'>The value of n3</z>
      <zz xmlns:spare='foo' xmlns='myns2' id='tri2'><inner>content</inner></zz>
</SOAP-ENV:Envelope>'''

    print _copyright
    from xml.dom.ext.reader import PyExpat
    reader = PyExpat.Reader()
    dom = reader.fromString(text)
    for e in _children(dom):
	if e.nodeType != Node.ELEMENT_NODE: continue
	for ee in _children(e):
	    if ee.nodeType != Node.ELEMENT_NODE: continue
	    print '\n', '=' * 60
	    print Canonicalize(ee, nsdict={'spare':'foo'}, stripspace=1)
	    print '-' * 60
	    print Canonicalize(ee, stripspace=0)
	    print '-' * 60
	    print Canonicalize(ee, comments=1)
	    print '=' * 60
