# -*- coding: utf-8 -*-

# Copyright (c) 2003 Detlev Offenbach <detlev@die-offenbachs.de>
#

from qt import *
from MatchesForm import MatchesForm

"""
Module implementing a dialog to show matching references/definitions.
"""

class MatchesDialog(MatchesForm):
    """
    Class implementing a dialog to show matching references/definitions.
    """
    def __init__(self,ui,parent = None,name = None,modal = 0,fl = 0):
        """
        Constructor
        
        @param ui reference to the UI object
        @param parent parent of this dialog (QWidget)
        @param name name of this dialog (string or QString)
        @param modal flag indicating a modal window (boolean)
        @param fl window flags
        """
        MatchesForm.__init__(self,parent,name,modal,fl)
        
        self.ui = ui
        
        self.matchesList.setSorting(0)
        self.matchesList.setColumnAlignment(1, Qt.AlignRight)
        self.matchesList.setColumnAlignment(2, Qt.AlignRight)
        
        dummy = self.trUtf8("Dummy")
        
    def handleDoubleClicked(self, itm):
        """
        Private slot to handle the DoubleClicked signal of the list.
        """
        lineno = int(str(itm.text(1)))
        fn = str(itm.text(0))
        self.ui.getViewManager().displayPythonFile(fn, lineno)
        
    def addEntry(self, ref):
        """
        Public slot to add a reference to the listview.
        """
        itm = QListViewItem(self.matchesList, ref.filename, 
            " %5d" % ref.lineno, " %5d" % ref.confidence)
