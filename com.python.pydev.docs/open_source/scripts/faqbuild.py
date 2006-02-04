TITLE = '--------------------------------------------------------------------------------------------------------------Title'

ANSWER = '--------------------------------------------------------------------------------------------------------------Answer'


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
        out += '<h4><a href="#ref_%s">%s</a></h4>\n\n' % (i,title)
    
    out += '<br/><hr/>'
        
    #now the titles and answers
    for i in range(len(content)):
        title, answer = content[i]
        out += '''<a name="ref_%s"></a>
<h2>%s</h2><br>%s
<a href="#top"><p align="right">Return to top&nbsp;&nbsp;</p></a>
<hr>
''' % (i, title, answer)       
    
    file( outF, 'w' ).write( out ) 
        
    

    