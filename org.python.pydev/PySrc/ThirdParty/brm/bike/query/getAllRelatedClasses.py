

"""

def getAllRelatedClasses(root,classfqn):
    classobj = getTypeOf(root,classfqn)
    rootClasses = _getRootClasses(classobj)
    #print rootClasses
    relatedClasses = [] + rootClasses
    for rootClass in rootClasses:
        relatedClasses += _getAllSubClasses(rootClass,root)
    return relatedClasses

def _getRootClasses(klass):
    if klass is None:  # i.e. dont have base class in our ast
        return None
    if klass.getBaseClassNames() == []:  # i.e. is a root class
        return[klass]
    else:
        rootclasses = []
        for base in klass.getBaseClassNames():
            baseclass = getTypeOf(klass,base)
            rootclass = _getRootClasses(baseclass)
            if rootclass is None:  # base class not in our ast
                rootclass = [klass]
            rootclasses+=rootclass
        return rootclasses


def _getAllSubClasses(baseclass, root, subclasses = []):
    class ClassVisitor:
        def visitSource(self,node):
            self.visit(node.fastparseroot)
            
        def visitClass(self, node):
            for basename in node.getBaseClassNames():
                if basename.find(baseclass.name) != -1 and \
                       getTypeOf(node,basename) == baseclass:
                    subclasses.append(node)
                    _getAllSubClasses(node,root,subclasses)
            for child in node.getChildNodes():
                self.visit(child)
                
    walk(root, ClassVisitor())
    return subclasses

"""
