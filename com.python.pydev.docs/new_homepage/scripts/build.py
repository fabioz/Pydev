import datetime
LAST_VERSION_TAG='0.9.8.6'


def template( template, contents, title ):

    contents_file = '../%s.contents.html' % contents
    target_file   = '../final/%s.html' % contents

    newFile = file( contents_file, 'r' ).read()

    contents = file( template, 'r' ).read()
    contents = contents.replace('%(contents)s', newFile)
    contents = contents.replace('%(title)s', title)
    contents = contents.replace('%(date)s', datetime.datetime.now().strftime('%d %B %Y'))
    contents = contents.replace('LAST_VERSION_TAG', LAST_VERSION_TAG)
    file( target_file, 'w' ).write( contents ) 

def main():
    template('../template1.html', 'index', 'Pydev Extensions')

if __name__ == '__main__':
    main()
    print 'built'