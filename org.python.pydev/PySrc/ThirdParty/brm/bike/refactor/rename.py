from bike.transformer.WordRewriter import WordRewriter
from bike.query.findReferences import findReferencesIncludingDefn
from bike.transformer.save import save

def rename(filename,lineno,col,newname,promptcallback=None):
    strrewrite = WordRewriter()
    for match in findReferencesIncludingDefn(filename,lineno,col):
        #print "rename match ",match
        if match.confidence == 100 or promptUser(promptcallback,match):
            strrewrite.rewriteString(match.sourcenode,
                                     match.lineno,match.colno,newname)
    strrewrite.commit()

def promptUser(promptCallback,match):
    if promptCallback is not None and \
       promptCallback(match.filename, match.lineno, match.colno, match.colend):
        return 1
    return 0
    
