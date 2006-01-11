LAST_VERSION_TAG='0.9.8.6'


def template( template, contents, title ):

    contents_file = '_%s.contents.html' % p_name
    target_file   = 'final/%s.html' % p_name


def main():
    template('template.html', 'index', 'Pydev Extensions')

if __name__ == '__main__':
    main()