"""A DOM implementation that offers traversal and ranges on top of
minidom, using the 4DOM traversal implementation."""

import minidom, string

class DOMImplementation(minidom.DOMImplementation):
    def hasFeature(self, feature, version):
        if version not in ("1.0", "2.0"):
            return 0
        feature = string.lower(feature)
        if feature in ['traversal','range']:
            return 1
        return minidom.DOMImplementation.hasFeature(self, feature, version)

    def _createDocument(self):
        return Document()

class Document(minidom.Document):
    implementation = DOMImplementation()
    def createNodeIterator(self, root, whatToShow, filter, entityReferenceExpansion):
        from xml.dom import NodeIterator
        nodi = NodeIterator.NodeIterator(root, whatToShow, filter, entityReferenceExpansion)
        return nodi

    def createTreeWalker(self, root, whatToShow, filter, entityReferenceExpansion):
        from TreeWalker import TreeWalker
        return TreeWalker(root, whatToShow, filter, entityReferenceExpansion)

    def createRange(self):
        import Range
        return Range.Range(self)

def getDOMImplementation():
    return Document.implementation
