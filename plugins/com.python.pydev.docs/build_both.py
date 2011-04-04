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
    LAST_VERSION_TAG = '2.0' #Not specified (let's leave one there)


import build_python_code_block


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
    
    final = ret[ret.find('<body>')+6: ret.find('</body>')].strip()
    if final.startswith('<div'):
        final = final[final.find('\n'):]
        final = final[:final.rfind('</div>')]
    
    postfix = '.contents.htm'
    name = source_filename.split('.')[0]
    if is_new_homepage:
        f = open(name+postfix, 'r')
        contents = f.read()
        f.close()
        final = contents.replace('<contents_area></contents_area>', '<contents_area>%s</contents_area>' % final)
        
        #make the output html (and not htm)
        postfix += 'l'
    
    final = final.replace('\r\n','\n').replace('\r','\n')
    f = open(name+postfix, 'wb')
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
    
    #Copy the update site redirections
    shutil.rmtree(os.path.join('final', 'updates'), ignore_errors=True)
    shutil.copytree('updates', os.path.join('final', 'updates'))
    
    shutil.rmtree(os.path.join('final', 'nightly'), ignore_errors=True)
    shutil.copytree('nightly', os.path.join('final', 'nightly'))
    
    shutil.copyfile('stylesheet.css', os.path.join('final', 'stylesheet.css'))
    shutil.copyfile('video_pydev_20.html', os.path.join('final', 'video_pydev_20.html'))
    shutil.copyfile('video_swfobject.js', os.path.join('final', 'video_swfobject.js'))
    
    GenerateRstInDir('.', True)
    
    sys.path.insert(0, os.path.join(this_script_dir, 'merged_homepage', 'scripts'))
    sys.path.insert(0, '.')
#    print 'PYTHONPATH changed. Using:'
#    for p in sys.path:
#        print '    - ', p
    
    os.chdir(os.path.join(this_script_dir, 'merged_homepage', 'scripts'))
    import build_merged #@UnresolvedImport
    os.chdir(os.path.join(this_script_dir, 'merged_homepage'))
    build_merged.LAST_VERSION_TAG = LAST_VERSION_TAG
    build_merged.DoIt()
    
    sys.stdout.write('Finished\n')