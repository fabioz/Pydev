import sys
import os
import re



def find_files(top):
    print top
    for root, dirs, files in os.walk(top):
        for d in ('.svn', '.git', '.metadata'):
            if d in dirs:
                  dirs.remove(d)

        for file in files:
            if file.lower() in ('feature.xml', 'pom.xml', 'manifest.mf'):
                yield os.path.join(root, file)

def update_version(version):
    dirname = os.path.dirname(os.path.abspath(__file__))
    for f in find_files(dirname):
        with open(f, 'r') as stream:
            contents = stream.read()

        new_contents = fix_contents_version(contents, version)
        if contents != new_contents:
            with open(f, 'w') as stream:
                stream.write(new_contents)


def fix_contents_version(contents, version):
    bugfixversion = int(re.sub(r'^\d\.\d\.(\d)', r'\1', version))
    nextversion = re.sub(r'^(\d\.\d\.)\d', r'\1', version) + str(bugfixversion + 1)
    contents = re.sub(r'(bundle-version=")\[\d\.\d\.\d,\d\.\d\.\d\)"', r'\1[%s,%s)"' %(version, nextversion), contents)
    contents = re.sub(r'(version=)\"\d\.\d\.\d(\.qualifier\")', r'\1"%s\2' % (version,), contents)
    contents = re.sub(r'(<version)>\d\.\d\.\d(-SNAPSHOT</version>)', r'\1>%s\2' % (version,), contents)
    contents = re.sub(r'(Bundle-Version:)\s\d\.\d\.\d(\.qualifier)', r'\1 %s\2' % (version,), contents)

    return contents


def test_lines():
    '''
    Things we must match:

        version="3.6.0.qualifier"
         <version>3.6.0-SNAPSHOT</version>
         Bundle-Version: 3.6.0.qualifier
         com.python.pydev.shared_core;bundle-version="[3.6.0,3.6.1)",
    '''

    contents = fix_contents_version('''version="3.6.0.qualifier"
     <version>3.6.0-SNAPSHOT</version>
     Bundle-Version: 3.6.0.qualifier
     com.python.pydev.shared_core;bundle-version="[3.6.0,3.6.1)",''', '3.7.1')

    expected = '''version="3.7.1.qualifier"
     <version>3.7.1-SNAPSHOT</version>
     Bundle-Version: 3.7.1.qualifier
     com.python.pydev.shared_core;bundle-version="[3.7.1,3.7.2)",'''
    assert contents.splitlines() == expected.splitlines(), '%s\n!=\n%s' % (contents, expected)
    print('Tests passed')



if __name__ == '__main__':
    if len(sys.argv) == 2:
        if sys.argv[1] == '--test':
            test_lines()
        else:
            update_version(sys.argv[1])
    else:
        print('This script requires the new version (i.e.: 3.6.0)')
