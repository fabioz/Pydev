# Copyright (c) 2002-2004 LOGILAB S.A. (Paris, FRANCE).
# http://www.logilab.fr/ -- mailto:contact@logilab.fr
#
# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation; either version 2 of the License, or (at your option) any later
# version.
#
# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details
#
# You should have received a copy of the GNU General Public License along with
# this program; if not, write to the Free Software Foundation, Inc.,
# 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
"""Universal reports objects

A Universal report is a tree of layout and content objects
"""

__revision__ = "$Id: nodes.py,v 1.1 2004-10-26 12:52:29 fabioz Exp $"

from logilab.common.tree import VNode

class BaseComponent(object, VNode):
    """base report component

    attributes
    * id : the component's optional id
    * klass : the component's optional klass
    """
    def __init__(self, id=None, klass=None):
        VNode.__init__(self, id)
        self.klass = klass

class BaseLayout(BaseComponent):
    """base container node

    attributes
    * BaseComponent attributes
    * children : components in this table (i.e. the table's cells)
    """
    def __init__(self, children=(), id=None, klass=None):
        super(BaseLayout, self).__init__(id, klass)
        for child in children:
            if isinstance(child, BaseComponent):
                self.append(child)
            else:
                self.add_text(child)

    def append(self, child):
        """overriden to detect problems easily"""
        assert child not in self.parents()
        VNode.append(self, child)
        
    def parents(self):
        """return the ancestor nodes"""
        assert self.parent is not self
        if self.parent is None:
            return []
        return [self.parent] + self.parent.parents()
    
    def add_text(self, text):
        """shortcut to add text data"""
        self.children.append(Text(text))


# non container nodes #########################################################

class Text(BaseComponent):
    """a text portion

    attributes :
    * BaseComponent attributes
    * data : the text value as an encoded string (REQUIRED)
    """
    def __init__(self, data, id=None, klass=None):
        super(Text, self).__init__(id, klass)
        if isinstance(data, unicode):
            data = data.encode('ascii')
        assert isinstance(data, str), data.__class__
        self.data = data

class VerbatimText(Text):
    """a verbatim text, display the raw data

    attributes :
    * BaseComponent attributes
    * data : the text value as an encoded string (REQUIRED)
    """
        
class Link(BaseComponent):
    """a labelled link

    attributes :
    * BaseComponent attributes
    * url : the link's target (REQUIRED)
    * label : the link's label as an encoded string (use the url by default)
    """
    def __init__(self, url, label=None, id=None, klass=None):
        super(Link, self).__init__(id, klass)
        assert url
        self.url = url
        self.label = label or url
        
class Image(BaseComponent):
    """an embeded or a single image

    attributes :
    * BaseComponent attributes
    * filename : the image's filename (REQUIRED)
    * stream : the stream object containing the image data (REQUIRED)
    * title : the image's optional title
    """
    def __init__(self, filename, stream, title=None, id=None, klass=None):
        super(Link, self).__init__(id, klass)
        assert filename
        assert stream
        self.filename = filename
        self.stream = stream
        self.title = title

        
# container nodes #############################################################
        
class Section(BaseLayout):
    """a section

    attributes :
    * BaseLayout attributes
    
    a title may also be given to the constructor, it'll be added
    as a first element
    a description may also be given to the constructor, it'll be added
    as a first paragraph
    """
    def __init__(self, title=None, description=None, klass=None,
                 id=None, children=()):
        super(Section, self).__init__(children, id, klass)
        if description:
            self.insert(0, Paragraph([Text(description)]))
        if title:
            self.insert(0, Title(children=(title,)))
        
class Title(BaseLayout):
    """a title
    
    attributes :
    * BaseLayout attributes

    A title must not contains a section nor a paragraph!
    """
    
class Span(BaseLayout):
    """a title
    
    attributes :
    * BaseLayout attributes

    A span should only contains Text and Link nodes
    """
    
class Paragraph(BaseLayout):
    """a simple text paragraph
    
    attributes :
    * BaseLayout attributes

    A paragraph must not contains a section !
    """
    
class Table(BaseLayout):
    """some tabular data

    attributes :
    * BaseLayout attributes
    * cols : the number of columns of the table (REQUIRED)
    * rheaders : the first row's elements are table's header
    * cheaders : the first col's elements are table's header
    * title : the table's optional title
    """    
    def __init__(self, cols, title=None, klass=None,
                 rheaders=0, cheaders=0, rrheaders=0, rcheaders=0, 
                 id=None, children=()):
        super(Table, self).__init__(children, id, klass)
        assert isinstance(cols, int)
        self.cols = cols
        self.title = title
        self.rheaders = rheaders
        self.cheaders = cheaders
        self.rrheaders = rrheaders
        self.rcheaders = rcheaders
        
class List(BaseLayout):
    """some list data

    attributes :
    * BaseLayout attributes
    """    
