import os
import sys

args = sys.argv[1:]
this_script_path = sys.argv[0]
this_script_dir = os.path.split(this_script_path)[0]

for arg in args:
    if arg.startswith('--version='):
        version = arg[len('--version='):]
        LAST_VERSION_TAG = version
else:
    LAST_VERSION_TAG = '1.4.7' #Not specified (let's leave one there)



#=======================================================================================================================
# BuildFromRst
#=======================================================================================================================
def BuildFromRst(source_filename):
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
        
    f = open(source_filename.split('.')[0]+'.contents.htm', 'w')
    print >> f, final
    f.close()


#=======================================================================================================================
# GenerateRstInDir
#=======================================================================================================================
def GenerateRstInDir(d):
    for f in os.listdir(d):
        if f.endswith('.rst'):
            BuildFromRst(f)
    
    
if __name__ == '__main__':
    os.chdir(os.path.join(this_script_dir, 'open_source'))
    GenerateRstInDir('.')
    
    d1 = 'open_source/scripts/'
    d2 = 'new_homepage/scripts/'
    
    sys.path.insert(0, './scripts')
    sys.path.insert(0, '.')
    
    os.chdir(os.path.join(this_script_dir, d1))
    import build_org #@UnresolvedImport
    build_org.LAST_VERSION_TAG = LAST_VERSION_TAG
    os.chdir('..')
    build_org.DoIt()
    
    os.chdir('..')
    os.chdir(os.path.join(this_script_dir, d2))
    import build_com #@UnresolvedImport
    build_com.LAST_VERSION_TAG = LAST_VERSION_TAG
    build_com.DoIt()
    
    sys.stdout.write('finished both\n')