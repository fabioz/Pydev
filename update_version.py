import sys
import os
import re

def compare_lines(lines_obtained, lines_expected):
    import textwrap
    if lines_obtained == lines_expected:
        return
    
    msg = 'Lines differ.\n'
    diff_lines = []
    lines_obtained.insert(0, '=== Obtained ===')
    lines_expected.insert(0, '=== Expected ===')
    for i in range(max(len(lines_obtained), len(lines_expected))):
        try:
            l1 = textwrap.wrap(lines_obtained[i])
        except IndexError:
            l1 = []
        try:
            l2 = textwrap.wrap(lines_expected[i])
        except IndexError:
            l2 = []

        for j in range(max(len(l1), len(l2))):
            try:
                line1 = l1[j]
            except:
                line1 = ''
            try:
                line2 = l2[j]
            except:
                line2 = ''
                
            if i == 0:
                sep = '    '
            else:
                sep = (' == ' if line1 == line2 else ' != ')

            # Add the line and the contents of each side.
            diff_lines.append(
                str(i) + '. ' + line1 + (' ' * (81 - len(line1))) + sep + line2)

    msg += '\n'.join(diff_lines)
    raise AssertionError(msg)

def find_files(top):
    print(top)
    for root, dirs, files in os.walk(top):
        for d in ('.svn', '.git', '.metadata', 'target'):
            if d in dirs:
                  dirs.remove(d)

        for file in files:
            if file.lower() in ('feature.xml', 'pom.xml', 'manifest.mf', 'liclipse.product', 'com.brainwy.liclipse.prod.product'):
                yield os.path.join(root, file)

def update_version(version):
    dirname = os.path.dirname(os.path.abspath(__file__))
    for f in find_files(dirname):
        with open(f, 'r') as stream:
            contents = stream.read()

        new_contents = fix_contents_version(contents, version)
        print('PyDev file to update', f)
        if contents != new_contents:
            print('CHANGED', f)
            with open(f, 'w') as stream:
                stream.write(new_contents)


def fix_contents_version(contents, version):
    bugfixversion = int(re.sub(r'^\d+\.\d+\.(\d+)', r'\1', version))
    nextversion = re.sub(r'^(\d+\.\d+\.)\d+', r'\1', version) + str(bugfixversion + 1)
    
    contents = re.sub(r'(bundle-version=")\[\d+\.\d+\.\d+,\d+\.\d+\.\d+\)"', r'\1[%s,%s)"' % (version, nextversion), contents)
    contents = re.sub(r'(version=)\"\d+\.\d+\.\d+(\.qualifier\")', r'\1"%s\2' % (version,), contents)
    contents = re.sub(r'(<version)>\d+\.\d+\.\d+(-SNAPSHOT</version>)', r'\1>%s\2' % (version,), contents)
    contents = re.sub(r'(Bundle-Version:)\s\d+\.\d+\.\d+(\.qualifier)', r'\1 %s\2' % (version,), contents)

    return contents

def fix_liclipse_contents_version(contents, version):
    bugfixversion = int(re.sub(r'^\d+\.\d+\.(\d+)', r'\1', version))
    nextversion = re.sub(r'^(\d+\.\d+\.)\d+', r'\1', version) + str(bugfixversion + 1)
    
    contents = re.sub(r'((com|org)\.python\.pydev(\.\w+)?;)(bundle-version=")\[\d+\.\d+\.\d+,\d+\.\d+\.\d+\)"', r'\1\4[%s,%s)"' % (version, nextversion), contents)
    contents = re.sub(r'(<feature id="org\.python\.pydev\.feature" version=")(\d+\.\d+\.\d+)(\.qualifier"/>)', r'\g<1>%s\3' % (version,), contents)
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
    compare_lines(contents.splitlines(), expected.splitlines())

def check_contents(contents, expected):
    compare_lines(contents.splitlines(), expected.splitlines())

def test_lines2():
    '''
    Things we must match:

        version="3.6.0.qualifier"
         <version>3.6.0-SNAPSHOT</version>
         Bundle-Version: 3.6.0.qualifier
         com.python.pydev.shared_core;bundle-version="[3.6.0,3.6.1)",
    '''

    contents = fix_liclipse_contents_version(''' com.github.eclipsecolortheme,
 com.brainwy.liclipse.theme;bundle-version="[3.5.0,3.5.1)",
 org.eclipse.core.filesystem,
 org.python.pydev.shared_ui;bundle-version="[5.6.0,5.6.1)",
 org.python.pydev.shared_core;bundle-version="[5.6.0,5.6.1)",
 org.python.pydev;bundle-version="[5.6.0,5.6.1)",
 com.python.pydev;bundle-version="[5.6.0,5.6.1)",
 
 <feature id="org.python.pydev.feature" version="5.6.0.qualifier"/>
''', '6.7.1')

    expected = ''' com.github.eclipsecolortheme,
 com.brainwy.liclipse.theme;bundle-version="[3.5.0,3.5.1)",
 org.eclipse.core.filesystem,
 org.python.pydev.shared_ui;bundle-version="[6.7.1,6.7.2)",
 org.python.pydev.shared_core;bundle-version="[6.7.1,6.7.2)",
 org.python.pydev;bundle-version="[6.7.1,6.7.2)",
 com.python.pydev;bundle-version="[6.7.1,6.7.2)",
 
 <feature id="org.python.pydev.feature" version="6.7.1.qualifier"/>
'''
    check_contents(contents, expected)
    print('Tests passed')

def update_version_in_liclipse(version):
    liclipse_dirname = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    liclipse_dir = os.path.join(liclipse_dirname, 'liclipse')
    assert os.path.exists(liclipse_dir)
    for f in find_files(liclipse_dir):
        with open(f, 'r') as stream:
            contents = stream.read()

        new_contents = fix_liclipse_contents_version(contents, version)
        print('LiClipse File to update', f)
        if contents != new_contents:
            print('CHANGED', f)
            with open(f, 'w') as stream:
                stream.write(new_contents)



if __name__ == '__main__':
    if len(sys.argv) == 2:
        if sys.argv[1] == '--test':
            test_lines()
            test_lines2()
        else:
            version = sys.argv[1]
            update_version(version)
            update_version_in_liclipse(version)
    else:
        print('This script requires the new version (i.e.: 3.6.0)')
