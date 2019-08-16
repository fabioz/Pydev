import datetime
import os
import shutil
import sys

import build_python_code_block


args = sys.argv[1:]
this_script_path = sys.argv[0]
this_script_dir = os.path.split(this_script_path)[0]


CURRENT_DATE = datetime.datetime.now()
# CURRENT_DATE = datetime.datetime(2017, 9, 20)

update_site_versions = [
    '7.3.0',
    '7.2.1',
    '7.2.0',
    '7.1.0',
    '7.0.3',
    '6.5.0',
    '6.4.4',
    '6.4.3',
    '6.4.2',
    '6.4.1',
    '6.4.0',
    '6.3.3',
    '6.3.2',
    '6.3.1',
    '6.3.0',
    '6.2.0',
    '6.1.0',
    '6.0.0',
    '5.9.2',
    '5.9.1',
    '5.9.0',
    '5.8.0',
    '5.7.0',
    '5.6.0',
    '5.5.0',
    '5.4.0',
    '5.3.1',
    '5.3.0',
    '5.2.0',
    '5.1.2',
    '5.1.1',
    '5.0.0',
    '4.5.5',
    '4.5.4',
    '4.5.3',
    '4.5.1',
    '4.5.0',
    'old',
]

LAST_VERSION_TAG = update_site_versions[0]


DEFAULT_CONTENTS_TEMPLATE = '''<doc>
<contents_area></contents_area>

%s

</doc>
'''

DEFAULT_AREAS = '''
<right_area>
</right_area>


<image_area></image_area>


<quote_area></quote_area>

'''

DEFAULT_AREAS_MANUAL = '''
<right_area>
</right_area>


<image_area>manual.png</image_area>


<quote_area></quote_area>

'''
#=======================================================================================================================
# BuildFromRst
#=======================================================================================================================
def BuildFromRst(source_filename, is_new_homepage=False):
    print source_filename
    import os
    from docutils import core

    # dict of default settings to override (same as in the cmdline params, but as attribute names:
    #   "--embed-stylesheet" => "embed_stylesheet"
    settings_overrides = {}

    import os
    # publish as html
    ret = core.publish_file(
        writer_name='html',
        source_path=source_filename,
        destination_path=os.tempnam(),
        settings_overrides=settings_overrides,
    )

    final = ret[ret.find('<body>') + 6: ret.find('</body>')].strip()
    if final.startswith('<div'):
        final = final[final.find('\n'):]
        final = final[:final.rfind('</div>')]

    rst_contents = open(source_filename, 'r').read()

    if rst_contents.startswith('..'):
        image_area_right_area_and_quote_area = ''
        # lines = []
        # for line in rst_contents.splitlines():
        #    if line.strip().startswith('..'):
        #        lines.append(line.strip()[2:].strip())
        # lines = lines[1:] #remove the first (empty) line
        # image_area_right_area_and_quote_area = '\n'.join(lines)
    else:
        if rst_contents.startswith('manual_adv'):
            image_area_right_area_and_quote_area = DEFAULT_AREAS
        else:
            image_area_right_area_and_quote_area = DEFAULT_AREAS_MANUAL

    name = source_filename.split('.')[0]
    if is_new_homepage:
        if os.path.exists(name + '.contents.htm'):
            raise AssertionError('This file should not exist: ' + name + '.contents.htm')
        if os.path.exists(name + '.contents.html'):
            raise AssertionError('This file should not exist: ' + name + '.contents.html')
        contents = DEFAULT_CONTENTS_TEMPLATE % (image_area_right_area_and_quote_area,)
        final = contents.replace('<contents_area></contents_area>', '<contents_area>%s</contents_area>' % final)


    final = final.replace('\r\n', '\n').replace('\r', '\n')
    f = open(name + '.contents.rst_html', 'wb')
    print >> f, final
    f.close()


COMPOSITE_CONTENT = '''<?xml version='1.0' encoding='UTF-8'?>
<?compositeMetadataRepository version='1.0.0'?>
<repository name='&quot;Eclipse Project Test Site&quot;'
    type='org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository' version='1.0.0'>
  <properties size='1'>
    <property name='p2.timestamp' value='{timestamp}'/>
  </properties>
 <children size='1'>
    <child location='https://dl.bintray.com/fabioz/pydev/{version}'/>
  </children>
</repository>
'''

COMPOSITE_ARTIFACTS = '''<?xml version='1.0' encoding='UTF-8'?>
<?compositeArtifactRepository version='1.0.0'?>
<repository name='&quot;Eclipse Project Test Site&quot;'
    type='org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository' version='1.0.0'>
  <properties size='1'>
    <property name='p2.timestamp' value='{timestamp}'/>
  </properties>
  <children size='3'>
    <child location='https://dl.bintray.com/fabioz/pydev/{version}'/>
  </children>
</repository>
'''

INDEX_CONTENTS = '''<!DOCTYPE html>
<html>
<head></head>
<body>PyDev update site aggregator.<br>
<br>
Bundles the following PyDev update site(s):<br>
<br>
<a href="https://dl.bintray.com/fabioz/pydev/{version}">https://dl.bintray.com/fabioz/pydev/{version}</a><br>
</body>
</html>

'''

#=======================================================================================================================
# GenerateRstInDir
#=======================================================================================================================
def GenerateRstInDir(d, is_new_homepage=False):
    for f in os.listdir(d):
        if f.endswith('.rst'):
            BuildFromRst(f, is_new_homepage)


if __name__ == '__main__':
    this_script_dir = os.path.realpath(os.path.abspath(this_script_dir))
    print 'Directory with this script:', this_script_dir

    print 'Generating rst for homepage'
    os.chdir(os.path.join(this_script_dir, 'homepage'))

    # Copy the update site redirections
    shutil.rmtree(os.path.join('final', 'updates'), ignore_errors=True)
    shutil.copytree('updates', os.path.join('final', 'updates'))

    shutil.rmtree(os.path.join('final', 'nightly'), ignore_errors=True)
    shutil.copytree('nightly', os.path.join('final', 'nightly'))

    import time
    timestamp = str(int(time.time()))

    def make_update_site_at_dir(directory, version, force):
        try:
            os.mkdir(directory)
        except:
            pass
        xml1 = os.path.join(directory, 'compositeArtifacts.xml')
        if force or not os.path.exists(xml1):
            with open(xml1, 'w') as stream:
                stream.write(COMPOSITE_ARTIFACTS.replace('{version}', version).replace('{timestamp}', timestamp))

        xml2 = os.path.join(directory, 'compositeContent.xml')
        if force or not os.path.exists(xml2):
            with open(xml2, 'w') as stream:
                stream.write(COMPOSITE_CONTENT.replace('{version}', version).replace('{timestamp}', timestamp))

        html = os.path.join(directory, 'index.html')
        if force or not os.path.exists(html):
            with open(html, 'w') as stream:
                stream.write(INDEX_CONTENTS.replace('{version}', version).replace('{timestamp}', timestamp))

    make_update_site_at_dir(os.path.join('final', 'updates'), LAST_VERSION_TAG, force=True)
    make_update_site_at_dir(os.path.join('final', 'nightly'), LAST_VERSION_TAG, force=True)

    for update_site_version in update_site_versions:
        make_update_site_at_dir(os.path.join('final', 'update_sites', update_site_version), update_site_version, force=False)


    shutil.copyfile('stylesheet.css', os.path.join('final', 'stylesheet.css'))
    shutil.copyfile('favicon.ico', os.path.join('final', 'favicon.ico'))
    shutil.copyfile('pydev_certificate.cer', os.path.join('final', 'pydev_certificate.cer'))
    shutil.copyfile('video_pydev_20.html', os.path.join('final', 'video_pydev_20.html'))
    shutil.copyfile('video_swfobject.js', os.path.join('final', 'video_swfobject.js'))

    GenerateRstInDir('.', True)

    sys.path.insert(0, os.path.join(this_script_dir, 'homepage', 'scripts'))
    sys.path.insert(0, '.')
#    print 'PYTHONPATH changed. Using:'
#    for p in sys.path:
#        print '    - ', p

    os.chdir(os.path.join(this_script_dir, 'homepage', 'scripts'))
    import build_merged  # @UnresolvedImport
    os.chdir(os.path.join(this_script_dir, 'homepage'))

    build_merged.LAST_VERSION_TAG = LAST_VERSION_TAG
    build_merged.CURRENT_DATE = CURRENT_DATE
    build_merged.DoIt()

    sys.stdout.write('Finished\n')
