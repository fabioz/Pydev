TITLE = '--------------------------------------------------------------------------------------------------------------Title'

ANSWER = '--------------------------------------------------------------------------------------------------------------Answer'

existing = set()

#gotten from curses.ascii -- that we can't import in windows.
def _ctoi(c):
    if type(c) == type(""):
        return ord(c)
    else:
        return c
    
def isalpha(c): return isupper(c) or islower(c)
def isupper(c): return _ctoi(c) >= 65 and _ctoi(c) <= 90
def islower(c): return _ctoi(c) >= 97 and _ctoi(c) <= 122

def MakeTitle(title):
    title = title[:50]
    
    temp = ''
    for c in title:
        if not isalpha(c):
            c = '_'
            
        temp += c.lower()
        
    title = temp
    
    while '__' in title:
        title = title.replace('__', '_')
        
    if title.endswith('_'):
        title = title[:-1]
        
    if title in existing:
        raise RuntimeError('Duplicated: %s' % title)
    
    existing.add(title)
    
    return title
    
def Generate(inF, outF):
    lines = file( inF, 'r' ).readlines() #read the faq file
    
    inTitle = False

    bufferTitle = ''
    bufferAnswer = ''

    content = dict()
    
    i = -1
    for l in lines:
        if l.startswith(TITLE):
            if i >= 0:
                content[i] = (bufferTitle, bufferAnswer)

            i += 1
            inTitle = True
            bufferTitle = ''
            bufferAnswer = ''

        elif l.startswith(ANSWER):
            inTitle = False

        else:
            if inTitle:
                bufferTitle += l
            else:
                bufferAnswer += l

    #last one
    content[i] = (bufferTitle, bufferAnswer)
        
    #ok, now we have all the content in a dict, so, let's write things out in html
    out = '<a name="top">'
    
    #first only titles
    for i in range(len(content)):
        title, answer = content[i]
        i = MakeTitle(title)
        out += '<h4><a href="#%s">%s</a></h4>\n\n' % (i,title)
    
    out += '<br/><hr/>'

    existing.clear()
    
    #now the titles and answers
    for i in range(len(content)):
        title, answer = content[i]
        i = MakeTitle(title)
        out += '''<a name="%s"></a>
<h2>%s</h2><br>%s
<a href="#top"><p align="right">Return to top&nbsp;&nbsp;</p></a>
<hr>
''' % (i, title, answer)       
    
    file( outF, 'w' ).write( out ) 
        
    

    