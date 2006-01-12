import datetime
LAST_VERSION_TAG='0.9.8.6'



def template( template, contents, title ):

    contents_file = '../%s.contents.html' % contents
    target_file   = '../final/%s.html' % contents

    contents_file = file( contents_file, 'r' ).read()
    
    contents = file( template, 'r' ).read()
    contents = contents.replace('%(contents_area)s', getContents(contents_file, 'contents_area'))
    contents = contents.replace('%(right_area)s', getContents(contents_file, 'right_area'))
    contents = contents.replace('%(image_area)s', getContents(contents_file, 'image_area'))
    contents = contents.replace('%(quote_area)s', getContents(contents_file, 'quote_area'))
    contents = contents.replace('%(title)s', title)
    contents = contents.replace('%(date)s', datetime.datetime.now().strftime('%d %B %Y'))
    contents = contents.replace('LAST_VERSION_TAG', LAST_VERSION_TAG)
    file( target_file, 'w' ).write( contents ) 

def getContents(contents_file, tag):
    try:
        istart = contents_file.index('<%s>'%tag)+2+len(tag)
        iend = contents_file.index('</%s>'%tag)
        contents_area = contents_file[istart: iend]
    except ValueError:
        return ""
    return contents_area
    

def main():
    template('../template1.html', 'index', 'Pydev Extensions')
    template('../template1.html', 'terms', 'Terms and Conditions')
    template('../template1.html', 'download', 'Download')
    template('../template1.html', 'buy', 'Buy')

if __name__ == '__main__':
    main()
    print 'built'