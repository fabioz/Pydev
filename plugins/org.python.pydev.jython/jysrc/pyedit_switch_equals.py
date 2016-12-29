#===============================================================================
# Pydev Extensions in Jython code protocol
#===============================================================================
if False:
    from org.python.pydev.editor import PyEdit #@UnresolvedImport
    cmd = 'command string'
    editor = PyEdit
    systemGlobals = {}



if cmd == 'onCreateActions':
    # interface: PyEdit object: this is the actual editor that we will act upon
    assert editor is not None

    SwitchEquals = systemGlobals.get('SwitchEquals')
    if SwitchEquals is None:
        Action = editor.getActionClass() #from org.eclipse.jface.action import Action #@UnresolvedImport

        class SwitchEquals(Action):
            '''
            Switch the assign target and value (i.e.: make the target the value and vice-versa)
            '''

            def __init__(self, editor):
                self.editor = editor

            def run(self):
                editor = self.editor
                sel = editor.createPySelection()
                txt = sel.getSelectedText()
                if not txt:
                    txt = sel.getLine()
                    first_char_pos = sel.getFirstCharPosition(txt)
                    txt = txt[first_char_pos:]
                    offset, length = sel.getLineOffset() + first_char_pos, len(txt)
                else:
                    s = sel.getTextSelection()
                    offset, length = s.getOffset(), s.getLength()
                if '=' not in txt:
                    return

                t0, t1 = txt.split('=', 1)

                doc = sel.getDoc()
                doc.replace(offset, length, '%s = %s' % (t1.strip(), t0.strip()))

        systemGlobals['SwitchEquals'] = SwitchEquals

    # Change these constants if the default does not suit your needs
    ACTIVATION_STRING = 'sw'
    WAIT_FOR_ENTER = False

    # Register the extension as an ActionListener.
    editor.addOfflineActionListener(ACTIVATION_STRING, SwitchEquals(editor), \
                                    'Switch assign target and value.', \
                                    WAIT_FOR_ENTER)

