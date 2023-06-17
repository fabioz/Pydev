def ReplaceContents(contents, start, end, obtain_new_contents):
    i_start = contents.find(start)
    found = 0
    while i_start >= 0:
        i_end = contents.find(end, i_start + len(start))
        if i_end >= 0:
            found += 1
            new_contents = obtain_new_contents(contents[i_start + len(start):i_end])
            converted = new_contents
            contents = list(contents)
            contents[i_start:i_end + len(end)] = converted
            contents = ''.join(contents)
            i_start = contents.find(start, i_start + len(new_contents))
        else:
            break
    assert found == 1, 'Found: %s' % (found,)
    return contents

def main():
    import sys
    if len(sys.argv) < 1:
        print('Expected directory containing grammar files to be optimized to be passed.')
        return

    import re
    target_dir = sys.argv[1]
    import os
    assert os.path.isdir(target_dir), 'Expected %s to be a directory.' % (target_dir,)

    print('Optimizing grammar at: ', target_dir)
    for f in os.listdir(target_dir):
        if f.endswith('.java'):
            if f.startswith('PythonGrammar') or f.startswith('FStringsGrammar'):
                full_filename = os.path.join(target_dir, f)
                with open(full_filename, 'rb') as stream:
                    contents = stream.read().decode('utf-8')


                with open(full_filename, 'wb') as stream:
                    print('Writing', full_filename)

                    contents = re.sub(r'\bCharStream\b', 'FastCharStream', contents)
                    contents = re.sub(r'\bStringBuffer\b', 'FastStringBuffer', contents)
                    contents = re.sub(r'\bStringBuilder\b', 'FastStringBuffer', contents)

                    contents = re.sub(r'protected FastCharStream input_stream;', 'private final FastCharStream input_stream;', contents)

                    regexp_to_match = re.compile(r'public void ReInit[^}]*')
                    contents = re.sub(regexp_to_match, '//Removed Reinit', contents)

                    i = contents.find('Token jjFillToken')
                    regexp_to_match = re.compile(r'Token jjFillToken[^}]*')

                    if 'tokenmanager' in f.lower() and not f.startswith('FStringsGrammar'):
                        new_jj_fill_token = '''
Token jjFillToken()
{
   final Token t;
   final String curTokenImage;
   if (jjmatchedPos < 0)
   {
      if (image == null)
         curTokenImage = "";
      else
         curTokenImage = image.toString();
      t = Token.newToken(jjmatchedKind, curTokenImage);
      t.beginLine = t.endLine = input_stream.bufline[input_stream.tokenBegin];
    t.beginColumn = t.endColumn = input_stream.bufcolumn[input_stream.tokenBegin];
   }
   else
   {
      String im = jjstrLiteralImages[jjmatchedKind];
      curTokenImage = (im == null) ? input_stream.GetImage() : im;
      t = Token.newToken(jjmatchedKind, curTokenImage);
      t.beginLine = input_stream.bufline[input_stream.tokenBegin];
    t.beginColumn = input_stream.bufcolumn[input_stream.tokenBegin];
    t.endLine = input_stream.bufline[input_stream.bufpos];
    t.endColumn = input_stream.bufcolumn[input_stream.bufpos];

   }

   return t;
'''
                        contents = ReplaceContents(contents, 'Token jjFillToken', 'return t;', lambda *args:new_jj_fill_token)

                    lines = []
                    for line in contents.splitlines():
                        line = line.replace('jjtree.builder', 'builder')

                        if 'GetSuffix' in line:
                            #Change something as
                            #image.append(input_stream.GetSuffix(jjimageLen + (lengthOfMatch = jjmatchedPos + 1)));
                            #to:
                            #input_stream.AppendSuffix(image, jjimageLen + (lengthOfMatch = jjmatchedPos + 1));
                            line = line.replace('));', ');')
                            line = line.replace('image.append(input_stream.GetSuffix(', 'input_stream.AppendSuffix(image, ')

                        elif line.strip().startswith('public PythonGrammar') and line.strip().endswith(') {'):
                            line = line.replace('(', '(boolean generateTree, ')
                            line += '\n    super(generateTree);\n    builder = jjtree.builder;'

                        lines.append(line)

                    stream.write('\n'.join(lines).encode('utf-8'))


if __name__ == '__main__':
    main()
