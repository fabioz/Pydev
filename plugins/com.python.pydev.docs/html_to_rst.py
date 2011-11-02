import subprocess
def html2rst(html):
    p = subprocess.Popen(['pandoc', '--from=html', '--to=rst'],
                         stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    return p.communicate(html)[0]

f = open(r'W:\pydev\plugins\com.python.pydev.docs\merged_homepage\final\history_pydev.html', 'r')
contents = f.read()
f.close()
print html2rst(contents)