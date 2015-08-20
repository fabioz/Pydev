import os
import sys
import shutil

args = sys.argv[1:]
this_script_path = sys.argv[0]
this_script_dir = os.path.split(this_script_path)[0]

for arg in args:
    if arg.startswith('--version='):
        version = arg[len('--version='):]
        LAST_VERSION_TAG = version
else:
    LAST_VERSION_TAG = '4.3.0'  # Not specified (let's leave one there)


import build_python_code_block


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

    print 'Generating rst for merged_homepage'
    os.chdir(os.path.join(this_script_dir, 'merged_homepage'))

    # Copy the update site redirections
    shutil.rmtree(os.path.join('final', 'updates'), ignore_errors=True)
    shutil.copytree('updates', os.path.join('final', 'updates'))

    shutil.rmtree(os.path.join('final', 'nightly'), ignore_errors=True)
    shutil.copytree('nightly', os.path.join('final', 'nightly'))

    shutil.copyfile('stylesheet.css', os.path.join('final', 'stylesheet.css'))
    shutil.copyfile('favicon.ico', os.path.join('final', 'favicon.ico'))
    shutil.copyfile('pydev_certificate.cer', os.path.join('final', 'pydev_certificate.cer'))
    shutil.copyfile('video_pydev_20.html', os.path.join('final', 'video_pydev_20.html'))
    shutil.copyfile('video_swfobject.js', os.path.join('final', 'video_swfobject.js'))

    GenerateRstInDir('.', True)

    sys.path.insert(0, os.path.join(this_script_dir, 'merged_homepage', 'scripts'))
    sys.path.insert(0, '.')
#    print 'PYTHONPATH changed. Using:'
#    for p in sys.path:
#        print '    - ', p

    os.chdir(os.path.join(this_script_dir, 'merged_homepage', 'scripts'))
    import build_merged  # @UnresolvedImport
    os.chdir(os.path.join(this_script_dir, 'merged_homepage'))
    build_merged.LAST_VERSION_TAG = LAST_VERSION_TAG
    build_merged.DoIt()

    sys.stdout.write('Finished\n')
