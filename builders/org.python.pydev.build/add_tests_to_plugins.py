'''
This script will change the plugins so that test folders are added to the plugin sources.

We do that so that we can test the code using maven-tycho.

Get jprops with:

pip install jprops --target jprops_src
'''
import os
import sys
from pathlib import Path
from xml.etree import ElementTree

assert sys.version_info[:2] >= (3, 7), 'Python 3.7 or newer is required to run this script.'

curdir = os.path.dirname(__file__)
sys.path.append(os.path.join(curdir, 'jprops_src'))


def main():
    import jprops

    pydev_dir = (Path(curdir) / '..' / '..')

    plugins_dir = pydev_dir / 'plugins'
    for plugin_dir in plugins_dir.iterdir():
        if plugin_dir.is_dir():
            build_properties = plugin_dir / 'build.properties'
            if not build_properties.exists():
                continue

            classpath = (plugin_dir / '.classpath')
            if not classpath.exists():
                continue

            with build_properties.open('r') as fp:
                properties = jprops.load_properties(fp)

            # Now, we need to add the test* folders to the source.xxx.jar.
            # The folders can be seen from the .classpath by reading all the
            # "classpathentry" where the kind="src".
            # and then adding the to the related `source.xxx.jar`.
            source_jar_key = None
            for k, _v in list(properties.items()):
                if k.startswith('source.') and k.endswith('.jar'):
                    source_jar_key = k
                    break

            if source_jar_key:
                tree = ElementTree.parse(classpath.open('r'))
                root = tree.getroot()
                source_jars = []
                for child in root:
                    # i.e.: ignore the previous value and add all the entries
                    # from the PYTHONPATH into the plugin sources.
                    if child.tag == 'classpathentry':
                        attrib = child.attrib
                        if attrib.get('kind') == 'src':
                            source_path = attrib['path']
                            if not source_path.endswith('/'):
                                source_path += '/'
                            source_jars.append(source_path)

                properties[source_jar_key] = ','.join(source_jars)

                if plugin_dir.name == 'org.python.pydev.refactoring':
                    # We need some additional fixes for it.
                    manifest = plugin_dir / 'META-INF' / 'MANIFEST.MF'
                    txt = manifest.read_text('utf-8')
                    assert txt.count('Bundle-ClassPath: refactoring.jar') == 1
                    assert 'tests/lib/xstream-1.4.4.jar' not in txt
                    txt = txt.replace('Bundle-ClassPath: refactoring.jar', '''Bundle-ClassPath: refactoring.jar,
 tests/lib/xpp3-1.1.3.4.O.jar,
 tests/lib/xstream-1.4.4.jar,
 contrib/ch/hsr/ukistler/astgraph/jgraph-5.8.3.1.jar''')
                    manifest.write_text(txt, 'utf-8')
                    print('Writing', manifest)

                    txt = classpath.read_text('utf-8')
                    assert txt.count('<classpathentry kind="lib"') == 3
                    txt = txt.replace('<classpathentry kind="lib"', '<classpathentry exported="true" kind="lib"')
                    classpath.write_text(txt, 'utf-8')
                    print('Writing', classpath)

                    properties['jars.extra.classpath'] = 'contrib/ch/hsr/ukistler/astgraph/jgraph-5.8.3.1.jar,tests/lib/xpp3-1.1.3.4.O.jar,tests/lib/xstream-1.4.4.jar'

                print('Updating', build_properties)
                with build_properties.open('w') as fp:
                    jprops.store_properties(fp, properties, timestamp=False)

    # Don't reenable runalltests (I haven't been able to make the JDT work, so, disabling for now).
    # plugins_pom = plugins_dir / 'pom.xml'
    # txt = plugins_pom.read_text('utf-8')
    # assert '<!-- <module>com.python.pydev.runalltests</module> -->' in txt
    # txt = txt.replace('<!-- <module>com.python.pydev.runalltests</module> -->', '<module>com.python.pydev.runalltests</module>')
    # print('Writing ', plugins_pom)
    # plugins_pom.write_text(txt, 'utf-8')




if __name__ == '__main__':
    main()
