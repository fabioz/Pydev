# -*- coding: utf-8 -*-

# Form implementation generated from reading ui file '/home/detlev/Development/Python/Eric/eric3/Refactoring/MatchesForm.ui'
#
# Created: Mit Aug 13 21:23:54 2003
#      by: The PyQt User Interface Compiler (pyuic) 3.7
#
# WARNING! All changes made in this file will be lost!


import sys
from qt import *


class MatchesForm(QDialog):
    def __init__(self,parent = None,name = None,modal = 0,fl = 0):
        QDialog.__init__(self,parent,name,modal,fl)

        if not name:
            self.setName("MatchesForm")

        self.setSizeGripEnabled(1)

        MatchesFormLayout = QVBoxLayout(self,6,6,"MatchesFormLayout")

        self.matchesList = QListView(self,"matchesList")
        self.matchesList.addColumn(self.__tr("Filename"))
        self.matchesList.addColumn(self.__tr("Line"))
        self.matchesList.addColumn(self.__tr("Confidence [%]"))
        self.matchesList.setAllColumnsShowFocus(1)
        self.matchesList.setShowSortIndicator(1)
        MatchesFormLayout.addWidget(self.matchesList)

        layout1 = QHBoxLayout(None,0,6,"layout1")
        spacer = QSpacerItem(40,20,QSizePolicy.Expanding,QSizePolicy.Minimum)
        layout1.addItem(spacer)

        self.closeButton = QPushButton(self,"closeButton")
        layout1.addWidget(self.closeButton)
        spacer_2 = QSpacerItem(40,20,QSizePolicy.Expanding,QSizePolicy.Minimum)
        layout1.addItem(spacer_2)
        MatchesFormLayout.addLayout(layout1)

        self.languageChange()

        self.resize(QSize(649,480).expandedTo(self.minimumSizeHint()))
        self.clearWState(Qt.WState_Polished)

        self.connect(self.closeButton,SIGNAL("clicked()"),self,SLOT("close()"))
        self.connect(self.matchesList,SIGNAL("doubleClicked(QListViewItem*)"),self.handleDoubleClicked)


    def languageChange(self):
        self.setCaption(self.__tr("Refactoring Matches"))
        self.matchesList.header().setLabel(0,self.__tr("Filename"))
        self.matchesList.header().setLabel(1,self.__tr("Line"))
        self.matchesList.header().setLabel(2,self.__tr("Confidence [%]"))
        self.closeButton.setText(self.__tr("&Close"))


    def handleDoubleClicked(self,a0):
        print "MatchesForm.handleDoubleClicked(QListViewItem*): Not implemented yet"

    def __tr(self,s,c = None):
        return qApp.translate("MatchesForm",s,c)

if __name__ == "__main__":
    a = QApplication(sys.argv)
    QObject.connect(a,SIGNAL("lastWindowClosed()"),a,SLOT("quit()"))
    w = MatchesForm()
    a.setMainWidget(w)
    w.show()
    a.exec_loop()
