"""This example shows how we can use Eclipse markers in Jython scripts.

PyDev provides following custom set of marker types that can be used
in scripts without interferences with other marker types:

  `org.python.pydev.jython.problemmarker`

  `org.python.pydev.jython.taskmarker`

  `org.python.pydev.jython.bookmark`

These marker types are derived from standard Eclipse marker types so for more
details about them please check Eclipse documentation:

https://help.eclipse.org/neon/index.jsp?topic=%2Forg.eclipse.platform.doc.user%2Fconcepts%2Fconcepts-11.htm
"""
if False:
    from org.python.pydev.editor import PyEdit #@UnresolvedImport
    cmd = 'command string'
    editor = PyEdit
    systemGlobals = {}

#--------------------------------------------------------------- REQUIRED LOCALS
#interface: String indicating which command will be executed
#As this script will be watching the PyEdit (that is the actual editor in Pydev), and this script
#will be listening to it, this string can indicate any of the methods of org.python.pydev.editor.IPyEditListener
assert cmd is not None

#interface: PyEdit object: this is the actual editor that we will act upon
assert editor is not None

# Uncomment the block below then press Ctrl+2 and select add-example-markers
# or remove-example-markers (accept with <ENTER>).
# Currently opened file should have at least three lines.

"""
if cmd == 'onCreateActions':
    from org.python.pydev.shared_ui.utils import PyMarkerUtils

    action_class = editor.getActionClass()

    AddMarkersCommand = systemGlobals.get('AddMarkersCommand')
    if AddMarkersCommand is None:
        class AddMarkersCommand(action_class):

            def __init__(self, editor_):
                self._editor = editor_
                self._document = editor_.getDocument()

            def run(self):
                resource = PyMarkerUtils.getResourceForTextEditor(self._editor)

                # define attributes for Problem marker
                marker_info = PyMarkerUtils.MarkerInfo(
                    self._document,
                    'An example Problem marker',
                    'org.python.pydev.jython.problemmarker',
                    1,  # marker severity
                    False,  # is user editable?
                    True,  # is transient?
                    0,  #  start line number
                    0,  # start column of text marker
                    0,  # end line number
                    7,  # end column of text marker,
                    None)

                # this will remove all existing Problem markers
                # and create new one with given attributes
                PyMarkerUtils.replaceMarkers(
                    [marker_info],
                    resource,
                    'org.python.pydev.jython.problemmarker',
                    True,
                    None)

                # define attributes for Task marker
                marker_info = PyMarkerUtils.MarkerInfo(
                    self._document,
                    'An example Task marker',
                    'org.python.pydev.jython.taskmarker',
                    1,  # marker severity
                    False,  # is user editable?
                    True,  # is transient?
                    1,  #  start line number
                    1,  # start column of text marker
                    1,  # end line number
                    1,  # end column of text marker,
                    None)

                # this will remove all existing Task markers
                # and create new one with given attributes
                PyMarkerUtils.replaceMarkers(
                    [marker_info],
                    resource,
                    'org.python.pydev.jython.taskmarker',
                    True,
                    None)

                # define attributes for Bookmark
                marker_info = PyMarkerUtils.MarkerInfo(
                    self._document,
                    'An example Bookmark',
                    'org.python.pydev.jython.bookmark',
                    1,  # marker severity
                    False,  # is user editable?
                    True,  # is transient?
                    2,  #  start line number
                    1,  # start column of text marker
                    2,  # end line number
                    1,  # end column of text marker,
                    None)

                # this will remove all existing Bookmarks
                # and create new one with given attributes
                PyMarkerUtils.replaceMarkers(
                    [marker_info],
                    resource,
                    'org.python.pydev.jython.bookmark',
                    True,
                    None)

        systemGlobals['AddMarkersCommand'] = AddMarkersCommand

    RemoveMarkersCommand = systemGlobals.get('RemoveMarkersCommand')
    if RemoveMarkersCommand is None:
        class RemoveMarkersCommand(action_class):

            def __init__(self, editor_):
                self._editor = editor_

            def run(self):
                resource = PyMarkerUtils.getResourceForTextEditor(self._editor)
                # each type of marker is removed separately
                PyMarkerUtils.removeMarkers(
                    resource,
                    'org.python.pydev.jython.problemmarker')
                PyMarkerUtils.removeMarkers(
                    resource,
                    'org.python.pydev.jython.taskmarker')
                PyMarkerUtils.removeMarkers(
                    resource,
                    'org.python.pydev.jython.bookmark')

        systemGlobals['RemoveMarkersCommand'] = RemoveMarkersCommand

    editor.addOfflineActionListener("add-example-markers",
                                    AddMarkersCommand(editor),
                                    'Add example markers.',
                                    True)

    editor.addOfflineActionListener("remove-example-markers",
                                    RemoveMarkersCommand(editor),
                                    'Remove example markers.',
                                    True)
"""
