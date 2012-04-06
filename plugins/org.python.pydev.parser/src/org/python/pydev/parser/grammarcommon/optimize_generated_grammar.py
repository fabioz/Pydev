def main():
    import sys
    if len(sys.argv) < 1:
        print 'Expected directory containing grammar files to be optimized to be passed.'
        return
    
    import re
    target_dir = sys.argv[1]
    import os
    assert os.path.isdir(target_dir), 'Expected %s to be a directory.' % (target_dir,)
    
    print 'Optimizing grammar at: ', target_dir
    for f in os.listdir(target_dir):
        if f.endswith('.java'):
            if f.startswith('PythonGrammar'):
                full_filename = os.path.join(target_dir, f)
                with open(full_filename, 'rb') as stream:
                    contents = stream.read()
                   
        
                with open(full_filename, 'wb') as stream:
                    print 'Writing', full_filename
                    
                    contents = re.sub(r'\bCharStream\b', 'FastCharStream', contents)
                    contents = re.sub(r'\bStringBuffer\b', 'FastStringBuffer', contents)
                    contents = re.sub(r'\bStringBuilder\b', 'FastStringBuffer', contents)
                    
                    contents = re.sub(r'protected FastCharStream input_stream;', 'private final FastCharStream input_stream;', contents)
                    
                    regexp_to_match = re.compile(r'public void ReInit[^}]*')
                    contents = re.sub(regexp_to_match, '//Removed Reinit', contents)
                    
                    lines = []
                    for line in contents.splitlines():
                        if 'GetSuffix' in line:
                            #Change something as 
                            #image.append(input_stream.GetSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));
                            #to:
                            #input_stream.AppendSuffix(image, jjimageLen + (lengthOfMatch = jjmatchedPos + 1));
                            line = line.replace('));', ');')
                            line = line.replace('image.append(input_stream.GetSuffix(', 'input_stream.AppendSuffix(image, ')
                        lines.append(line)
                    
                    stream.write('\n'.join(lines))
                    

if __name__ == '__main__':
    main()