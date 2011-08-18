if __name__ == '__main__':
    import os
    
    for root, dirs, files in os.walk(os.curdir):
        for file in files:
            file = file.lower()
            #import pdb;pdb.set_trace()
            if os.path.splitext(file)[1] in (
                '.py', 
                '.pyw', 
                '.java', 
                '.xml', 
                '.html', 
                '.htm', 
                '.txt', 
#                '.launch', 
                '.diff', 
                '.jjt', 
                '.jjt_template', 
                '.project', 
                '.pydevproject', 
                '.css', 
                '.exsd', 
                '.mf'
                ):
                path = os.path.join(root, file)
                contents = open(path, 'rb').read()
                if '\r' in contents:
                    print path
                    contents = contents.replace('\r\n', '\n').replace('\r','\n')
                    open(path, 'wb').write(contents)