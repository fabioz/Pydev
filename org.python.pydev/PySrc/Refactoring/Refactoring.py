# -*- coding: utf-8 -*-

# Copyright (c) 2003 Detlev Offenbach <detlev@die-offenbachs.de>
#

"""
Module implementing the refactoring interface to brm.
"""

import os
import sys

sys.path.insert(1, os.path.join(os.path.dirname(sys.argv[0]), 
    "ThirdParty", "brm"))
import ThirdParty.brm.bike as bike
    
from qt import *

from MatchesDialog import MatchesDialog
import Utilities
import Preferences

BRM_VERSION_STR = '0.9 cvs20040211'

class SilentLogger:
    """
    Class implementing a logger that doesn't log anything.
    """
    def write(*args):
        """
        Public method to write the arguments.
        """
        pass

class Refactoring(QObject):
    """
    Class implementing the refactoring interface to brm.
    """
    def __init__(self, parent = None, *args):
        """
        Constructor
        
        @param parent parent (QObject)
        @param *args arguments passed on to QObject
        """
        QObject.__init__(*(self, parent) + args)
        
        self.ui = parent
        self.projectpath = ''
        self.projectopen = 0
        
        self.refreshing = 0
        self.matchesDialog = None
        self.init()
        
    def initActions(self):
        """
        Public method to define the refactoring actions.
        """
        self.resetBRMAct = QAction(self.trUtf8('Reset'),
                self.trUtf8('Re&set'),0,
                self)
        self.resetBRMAct.setStatusTip(self.trUtf8('Reset the refactoring machine'))
        self.resetBRMAct.setWhatsThis(self.trUtf8(
            """<b>Reset</b>"""
            """<p>Reset the refactoring machine.</p>"""
        ))
        self.connect(self.resetBRMAct,SIGNAL('activated()'),self.handleReset)
        
        self.queryReferencesAct = QAction(self.trUtf8('Find references'),
                self.trUtf8('Find &References'),0,
                self)
        self.queryReferencesAct.setStatusTip(self.trUtf8('Find references of the highlighted item'))
        self.queryReferencesAct.setWhatsThis(self.trUtf8(
            """<b>Find references</b>"""
            """<p>Find references to the highlighted class, method, function or variable.</p>"""
        ))
        self.connect(self.queryReferencesAct,SIGNAL('activated()'),self.handleQueryReferences)
        
        self.queryDefinitionAct = QAction(self.trUtf8('Find definition'),
                self.trUtf8('Find &Definition'),0,
                self)
        self.queryDefinitionAct.setStatusTip(self.trUtf8('Find definition of the highlighted item'))
        self.queryDefinitionAct.setWhatsThis(self.trUtf8(
            """<b>Find definition</b>"""
            """<p>Find the definition to the highlighted class, method, function or variable.</p>"""
        ))
        self.connect(self.queryDefinitionAct,SIGNAL('activated()'),self.handleQueryDefinition)
        
        self.refactoringRenameAct = QAction(self.trUtf8('Rename'),
                self.trUtf8('&Rename'),0,
                self)
        self.refactoringRenameAct.setStatusTip(self.trUtf8('Rename the highlighted item'))
        self.refactoringRenameAct.setWhatsThis(self.trUtf8(
            """<b>Rename</b>"""
            """<p>Rename the highlighted class, method, function or variable.</p>"""
        ))
        self.connect(self.refactoringRenameAct,SIGNAL('activated()'),self.handleRename)
        
        self.refactoringExtractMethodAct = QAction(self.trUtf8('Extract method'),
                self.trUtf8('Extract &Method'),0,
                self)
        self.refactoringExtractMethodAct.setStatusTip(self.trUtf8('Extract the highlighted area as a method'))
        self.refactoringExtractMethodAct.setWhatsThis(self.trUtf8(
            """<b>Extract method</b>"""
            """<p>Extract the highlighted area as a method or function.</p>"""
        ))
        self.connect(self.refactoringExtractMethodAct,SIGNAL('activated()'),self.handleExtractMethod)
        
        self.refactoringInlineLocalVariableAct = QAction(self.trUtf8('Inline local variable'),
                self.trUtf8('&Inline Local Variable'),0,
                self)
        self.refactoringInlineLocalVariableAct.setStatusTip(self.trUtf8('Inlines the selected local variable'))
        self.refactoringInlineLocalVariableAct.setWhatsThis(self.trUtf8(
            """<b>Inline local variable</b>"""
            """<p>Inlines the selected local variable.</p>"""
        ))
        self.connect(self.refactoringInlineLocalVariableAct,SIGNAL('activated()'),self.handleInlineLocalVariable)
        
        self.refactoringExtractLocalVariableAct = QAction(self.trUtf8('Extract local variable'),
                self.trUtf8('Extract Local &Variable'),0,
                self)
        self.refactoringExtractLocalVariableAct.setStatusTip(self.trUtf8('Extract the highlighted area as a local variable'))
        self.refactoringExtractLocalVariableAct.setWhatsThis(self.trUtf8(
            """<b>Extract local variable</b>"""
            """<p>Extract the highlighted area as a local variable.</p>"""
        ))
        self.connect(self.refactoringExtractLocalVariableAct,SIGNAL('activated()'),self.handleExtractLocalVariable)
        
        self.refactoringUndoAct = QAction(self.trUtf8('Undo'),
                self.trUtf8('&Undo'),0,
                self)
        self.refactoringUndoAct.setStatusTip(self.trUtf8('Undo refactorings'))
        self.refactoringUndoAct.setWhatsThis(self.trUtf8(
            """<b>Undo</b>"""
            """<p>Undo the performed refactorings.</p>"""
        ))
        self.connect(self.refactoringUndoAct,SIGNAL('activated()'),self.handleUndo)
        
        self.refactoringMoveClassAct = QAction(self.trUtf8('Move Class'),
                self.trUtf8('Move &Class'),0,
                self)
        self.refactoringMoveClassAct.setStatusTip(self.trUtf8('Move class to new module'))
        self.refactoringMoveClassAct.setWhatsThis(self.trUtf8(
            """<b>Move class</b>"""
            """<p>Moves the class pointed to by the cursor to a new module.</p>"""
        ))
        self.connect(self.refactoringMoveClassAct,SIGNAL('activated()'),self.handleMoveClass)
        
        self.refactoringMoveFunctionAct = QAction(self.trUtf8('Move Function'),
                self.trUtf8('Move &Function'),0,
                self)
        self.refactoringMoveFunctionAct.setStatusTip(self.trUtf8('Move function to new module'))
        self.refactoringMoveFunctionAct.setWhatsThis(self.trUtf8(
            """<b>Move function</b>"""
            """<p>Moves the function pointed to by the cursor to a new module.</p>"""
        ))
        self.connect(self.refactoringMoveFunctionAct,SIGNAL('activated()'),self.handleMoveFunction)
        
        # disable some action until the code behind them works reliably
        self.refactoringMoveClassAct.setEnabled(0)
        self.refactoringMoveFunctionAct.setEnabled(0)
        
    def initMenu(self):
        """
        Public slot to initialize the refactoring menu.
        
        @return the menu generated (QPopupMenu)
        """
        self.menuitems = []
        menu = QPopupMenu(self.ui)
        
        lbl = QLabel('Bicycle Repair Man', menu)
        lbl.setFrameStyle( QFrame.Panel | QFrame.Sunken )
        lbl.setAlignment(Qt.AlignHCenter)
        font = lbl.font()
        font.setBold(1)
        lbl.setFont(font)
        menu.insertItem(lbl)
        
        menu.insertTearOffHandle()
        
        smenu = QPopupMenu(self.ui)
        self.queryReferencesAct.addTo(smenu)
        self.queryDefinitionAct.addTo(smenu)
        self.menuitems.append(menu.insertItem(self.trUtf8("&Query"), smenu))
        
        smenu = QPopupMenu(self.ui)
        self.refactoringRenameAct.addTo(smenu)
        smenu.insertSeparator()
        self.refactoringExtractMethodAct.addTo(smenu)
        smenu.insertSeparator()
        self.refactoringInlineLocalVariableAct.addTo(smenu)
        self.refactoringExtractLocalVariableAct.addTo(smenu)
        smenu.insertSeparator()
        self.refactoringMoveClassAct.addTo(smenu)
        self.refactoringMoveFunctionAct.addTo(smenu)
        smenu.insertSeparator()
        self.refactoringUndoAct.addTo(smenu)
        self.menuitems.append(menu.insertItem(self.trUtf8("&Refactoring"), smenu))
        
        menu.insertSeparator()
        self.resetBRMAct.addTo(menu)
        
        self.menu = menu
        return menu
        
    def init(self):
        """
        Private slot to handle the Reset action.
        """
        self.brmctx = bike.init()
        self.logging = Preferences.getRefactoring("Logging")
        if self.logging:
            self.brmctx.setProgressLogger(self.ui.stdout)
        else:
            self.brmctx.setProgressLogger(SilentLogger())
        self.brmctx.setWarningLogger(self.ui.stderr)
        
    def handleReset(self):
        """
        Private slot to handle the Reset action.
        """
        self.init()
        
    ##################################################################
    ## methods below are handling the various Query actions
    ##################################################################
    
    def handleQueryReferences(self):
        """
        Private slot to handle the Query References action.
        """
        self.showOutput()
        
        aw = self.ui.getViewManager().activeWindow()
        
        if aw is None:
            return
            
        if not aw.hasSelectedText():
            # no selection available
            QMessageBox.warning(self.ui, self.trUtf8("Query References"),
                self.trUtf8("Highlight the name of a Function, Class or Method and try again."),
                self.trUtf8("&OK"))
            return 
        
        if not self.confirmAllBuffersSaved():
            return
            
        filename = aw.getFileName()
        line, column, dummy1, dummy2 = aw.getSelection()
        
        qApp.setOverrideCursor(Qt.waitCursor)
        qApp.processEvents()
        self.matchesDialog = None
        if self.projectopen:
            sys.path.insert(0, self.projectpath)
        try:
            try:
                for ref in self.brmctx.findReferencesByCoordinates(filename, line + 1, column):
                    if self.matchesDialog is None:
                        self.matchesDialog = MatchesDialog(self.ui)
                        self.matchesDialog.show()
                    self.matchesDialog.addEntry(ref)
            except AssertionError:
                pass
            except Exception, e:
                self.__unhandledException(e)
        finally:
            if self.projectopen:
                del sys.path[0]
        qApp.restoreOverrideCursor()
            
        if self.matchesDialog is None:
            QMessageBox.information(self.ui, self.trUtf8("Query References"),
                self.trUtf8("No matches were found."),
                self.trUtf8("&OK"))
        
    def handleQueryDefinition(self):
        """
        Private slot to handle the Query Definition action
        """
        self.showOutput()
        
        aw = self.ui.getViewManager().activeWindow()
        
        if aw is None:
            return
            
        if not self.confirmAllBuffersSaved():
            return
            
        filename = aw.getFileName()
        if aw.hasSelectedText():
            line, column, dummy1, dummy2 = aw.getSelection()
        else:
            line, column = aw.getCursorPosition()
            
        qApp.setOverrideCursor(Qt.waitCursor)
        qApp.processEvents()
        if self.projectopen:
            sys.path.insert(0, self.projectpath)
        try:
            try:
                defns = self.brmctx.findDefinitionByCoordinates(filename, line + 1, column)
                
                try:
                    firstref = defns.next()
                    self.ui.getViewManager().displayPythonFile(firstref.filename, firstref.lineno)
                except StopIteration:
                    QMessageBox.information(self.ui, self.trUtf8("Query Definition"),
                        self.trUtf8("Definition wasn't found."),
                        self.trUtf8("&OK"))
                else:
                    self.matchesDialog = None
                    for ref in defns:
                        if self.matchesDialog is None:
                            self.matchesDialog = MatchesDialog(self.ui)
                            self.matchesDialog.show()
                            self.matchesDialog.addEntry(firstref)
                        self.matchesDialog.addEntry(ref)
            except AssertionError:
                pass
            except Exception, e:
                self.__unhandledException(e)
        finally:
            if self.projectopen:
                del sys.path[0]
        qApp.restoreOverrideCursor()
        
    ##################################################################
    ## methods below are handling the various Refactoring actions
    ##################################################################
    
    def renameMethodPromptCallback(self, filename, line, start, stop):
        """
        Private slot called by the refactoring machine to ask the user for confirmation.
        
        @param filename the name of the file (string)
        @param line the line of the object (int)
        @param start beginning column of the object (int)
        @param stop end column of the object (int)
        @return flag indicating renaming wanted (boolean)
        """
        vm = self.ui.getViewManager()
        
        # display the file and select the method call
        vm.displayPythonFile(filename)
        aw = vm.activeWindow()
        cline, cindex = aw.getCursorPosition()
        aw.ensureLineVisible(line-1)
        aw.gotoLine(line-1)
        aw.setSelection(line-1, start, line-1, stop)
        
        ans = QMessageBox.information(self.ui, self.trUtf8("Rename"),
            self.trUtf8("Cannot deduce the type of the highlighted object reference.<br>"
                "Rename this declaration?"),
            self.trUtf8("&Yes"), self.trUtf8("&No"))
            
        aw.setCursorPosition(cline, cindex)
        aw.ensureCursorVisible()
        
        return ans == 0
        
    def refreshEditors(self, savedfiles, filename, line):
        """
        Private method to refresh modified editors.
        
        @param savedfiles list of filenames of modified files (list of strings)
        @param filename filename of the active editor (string)
        @param line line to place cursor at (int)
        """
        vm = self.ui.getViewManager()
        openFiles = vm.getOpenFilenames()
        
        self.refreshing = 1
        for file in savedfiles:
            normfile = Utilities.normcasepath(file)
            if normfile in openFiles:
                dummy, editor = vm.getEditor(normfile)
                editor.refresh()
        
        if filename is not None:
            vm.displayPythonFile(filename, line)
        self.refreshing = 0
        
    def handleRename(self):
        """
        Private slot to handle the Rename action.
        """
        self.showOutput()
        
        aw = self.ui.getViewManager().activeWindow()
        
        if aw is None:
            return
            
        if not aw.hasSelectedText():
            # no selection available
            QMessageBox.warning(self.ui, self.trUtf8("Rename"),
                self.trUtf8("Highlight the declaration you want to rename and try again."),
                self.trUtf8("&OK"))
            return 
        
        if not self.confirmAllBuffersSaved():
            return
            
        filename = aw.getFileName()
        line, column, dummy1, dummy2 = aw.getSelection()
        
        if self.projectopen:
            sys.path.insert(0, self.projectpath)
        try:
            try:
                self.brmctx.setRenameMethodPromptCallback(self.renameMethodPromptCallback)
                
                newname, ok = QInputDialog.getText(self.trUtf8("Rename"),
                    self.trUtf8("Rename to:"))
                    
                if not ok or newname.isEmpty():
                    return  # user pressed cancel or didn't enter anything
                    
                self.brmctx.renameByCoordinates(filename, line+1, column, str(newname))
                savedfiles = self.brmctx.save()
                self.refreshEditors(savedfiles, filename, line+1)
            except AssertionError:
                pass
            except Exception, e:
                self.__unhandledException(e)
        finally:
            if self.projectopen:
                del sys.path[0]
        
    def handleExtractMethod(self):
        """
        Private slot to handle the Extract Method action.
        """
        self.showOutput()
        
        aw = self.ui.getViewManager().activeWindow()
        
        if aw is None:
            return
            
        if not aw.hasSelectedText():
            # no selection available
            QMessageBox.warning(self.ui, self.trUtf8("Extract Method"),
                self.trUtf8("Highlight the region of code you want to extract and try again."),
                self.trUtf8("&OK"))
            return 
        
        if not self.confirmAllBuffersSaved():
            return
            
        filename = aw.getFileName()
        startline, startcolumn, endline, endcolumn = aw.getSelection()
        
        newname, ok = QInputDialog.getText(self.trUtf8("Extract Method"),
            self.trUtf8("New Name:"))
            
        if not ok or newname.isEmpty():
            return  # user pressed cancel or didn't enter anything
            
        if self.projectopen:
            sys.path.insert(0, self.projectpath)
        try:
            try:
                self.brmctx.extractMethod(filename, startline+1, startcolumn, 
                    endline+1, endcolumn, str(newname))
                savedfiles = self.brmctx.save()
                self.refreshEditors(savedfiles, filename, startline+1)
            except AssertionError:
                pass
            except Exception, e:
                self.__unhandledException(e)
        finally:
            if self.projectopen:
                del sys.path[0]
            
    def handleInlineLocalVariable(self):
        """
        Private slot to handle the Inline Local Variable action.
        """
        self.showOutput()
        
        aw = self.ui.getViewManager().activeWindow()
        
        if aw is None:
            return
            
        if not self.confirmAllBuffersSaved():
            return
            
        filename = aw.getFileName()
        if aw.hasSelectedText():
            line, column, dummy1, dummy2 = aw.getSelection()
        else:
            line, column = aw.getCursorPosition()
        
        if self.projectopen:
            sys.path.insert(0, self.projectpath)
        try:
            try:
                self.brmctx.inlineLocalVariable(filename, line+1, column)
                savedfiles = self.brmctx.save()
                self.refreshEditors(savedfiles, filename, line+1)
            except AssertionError:
                pass
            except Exception, e:
                self.__unhandledException(e)
        finally:
            if self.projectopen:
                del sys.path[0]
        
    def handleExtractLocalVariable(self):
        """
        Private slot to handle the Extract Local Variable action.
        """
        self.showOutput()
        
        aw = self.ui.getViewManager().activeWindow()
        
        if aw is None:
            return
            
        if not aw.hasSelectedText():
            # no selection available
            QMessageBox.warning(self.ui, self.trUtf8("Extract Local Variable"),
                self.trUtf8("Highlight the region of code you want to extract and try again."),
                self.trUtf8("&OK"))
            return 
        
        if not self.confirmAllBuffersSaved():
            return
            
        filename = aw.getFileName()
        startline, startcolumn, endline, endcolumn = aw.getSelection()
        
        newname, ok = QInputDialog.getText(self.trUtf8("Extract Local Variable"),
            self.trUtf8("New Name:"))
            
        if not ok or newname.isEmpty():
            return  # user pressed cancel or didn't enter anything
            
        if self.projectopen:
            sys.path.insert(0, self.projectpath)
        try:
            try:
                self.brmctx.extractLocalVariable(filename, startline+1, startcolumn, 
                    endline+1, endcolumn, str(newname))
                savedfiles = self.brmctx.save()
                self.refreshEditors(savedfiles, filename, startline+1)
            except AssertionError:
                pass
            except Exception, e:
                self.__unhandledException(e)
        finally:
            if self.projectopen:
                del sys.path[0]
        
    def handleMoveClass(self):
        """
        Private slot to handle the Move Class action.
        """
        self.showOutput()
        
        aw = self.ui.getViewManager().activeWindow()
        
        if aw is None:
            return
            
        if not self.confirmAllBuffersSaved():
            return
            
        filename = aw.getFileName()
        line, column = aw.getCursorPosition()
        
        newname = QFileDialog.getSaveFileName(\
            None,
            self.trUtf8("Python Files (*.py)"),
            None, None,
            self.trUtf8("New Module"),
            None, 1)
            
        if newname.isEmpty():
            return  # user pressed cancel or didn't enter anything
            
        newname = str(newname)
        if not newname.endswith('.py'):
            newname = "%s.py" % newname
        if not os.path.exists(newname):
            try:
                f = open(newname, "w")
                f.close()
            except Exception, e:
                self.__unhandledException(e)
                return
        
        if self.projectopen:
            sys.path.insert(0, self.projectpath)
        try:
            try:
                self.brmctx.moveClassToNewModule(filename, line+1, str(newname))
                savedfiles = self.brmctx.save()
                self.refreshEditors(savedfiles, filename, line+1)
            except AssertionError:
                pass
            except Exception, e:
                self.__unhandledException(e)
        finally:
            if self.projectopen:
                del sys.path[0]
        
    def handleMoveFunction(self):
        """
        Private slot to handle the Move Function action.
        """
        self.showOutput()
        
        aw = self.ui.getViewManager().activeWindow()
        
        if aw is None:
            return
            
        if not self.confirmAllBuffersSaved():
            return
            
        filename = aw.getFileName()
        line, column = aw.getCursorPosition()
        
        newname = QFileDialog.getSaveFileName(\
            None,
            self.trUtf8("Python Files (*.py)"),
            None, None,
            self.trUtf8("New Module"),
            None, 1)
            
        if newname.isEmpty():
            return  # user pressed cancel or didn't enter anything
            
        newname = str(newname)
        if not newname.endswith('.py'):
            newname = "%s.py" % newname
        if not os.path.exists(newname):
            try:
                f = open(newname, "w")
                f.close()
            except Exception, e:
                self.__unhandledException(e)
                return
        
        if self.projectopen:
            sys.path.insert(0, self.projectpath)
        try:
            try:
                self.brmctx.moveFunctionToNewModule(filename, line+1, str(newname))
                savedfiles = self.brmctx.save()
                self.refreshEditors(savedfiles, filename, line+1)
            except AssertionError:
                pass
            except Exception, e:
                self.__unhandledException(e)
        finally:
            if self.projectopen:
                del sys.path[0]
        
    def handleUndo(self):
        """
        Private slot to handle the Undo action.
        """
        self.showOutput()
        
        aw = self.ui.getViewManager().activeWindow()
        
        if aw is not None:
            filename = aw.getFileName()
            line, column = aw.getCursorPosition()
        else:
            filename = None
            line = 0
        try:
            try:    
                self.brmctx.undo()
            except bike.UndoStackEmptyException:
                QMessageBox.information(self.ui, self.trUtf8("Undo"),
                    self.trUtf8("The Undo stack is empty."),
                    self.trUtf8("&OK"))
            
            savedfiles = self.brmctx.save()
            self.refreshEditors(savedfiles, filename, line+1)
        except AssertionError:
            pass
        except Exception, e:
            self.__unhandledException(e)
        
    ##################################################################
    ## methods below are private utility methods
    ##################################################################
    
    def confirmBufferIsSaved(self, editor):
        """
        Private method to check, if an editor has unsaved changes.
        
        @param editor Reference to the editor to be checked.
        """
        return editor.checkDirty()
        
    def confirmAllBuffersSaved(self):
        """
        Private method to check, if any editor has unsaved changes.
        """
        return self.ui.getViewManager().checkAllDirty()
        
    def setMenuItemsEnabled(self, enabled):
        """
        Private method to enable/disable menu items.
        
        @param enabled Flag indicating enabled or disabled status. (boolean)
        """
        for itm in self.menuitems:
            self.menu.setItemEnabled(itm, enabled)
            
    def __unhandledException(self, msg):
        """
        Private method handling not specifically handled exceptions.
        
        @param msg message describing the exception (string)
        """
        if not str(msg):
            msg = str(sys.exc_info()[0])
        QMessageBox.critical(None,
            self.trUtf8("Refactoring"),
            self.trUtf8("""Caught exception <b>%1</b>""")
                .arg(str(msg)),
            self.trUtf8("&OK"),
            None,
            None,
            0, -1)
            
    def showOutput(self):
        """
        Private method to switch to the relevant output tab.
        """
        if self.logging:
            qApp.mainWidget().showLogTab("stdout")
        else:
            qApp.mainWidget().showLogTab("stderr")

    ##################################################################
    ## slots below are public utility methods
    ##################################################################
    
    def handlePreferencesChanged(self):
        """
        Public slot called when the preferences have been changed.
        """
        self.logging = Preferences.getRefactoring("Logging")
        if self.logging:
            self.brmctx.setProgressLogger(self.ui.stdout)
        else:
            self.brmctx.setProgressLogger(SilentLogger())
        
    def getActions(self):
        """
        Public method to get a list of all actions.
        
        @return list of all actions (list of QAction)
        """
        actionList = []
        for act in self.queryList("QAction"):
            if not isinstance(act, QActionGroup):
                actionList.append(act)
                
        return actionList
        
    def handleProjectOpened(self):
        """
        Public slot to handle the projectOpened signal.
        """
        self.projectopen = 1
        self.projectpath = self.ui.getProject().ppath
        
    def handleProjectClosed(self):
        """
        Public slot to handle the projectClosed signal.
        """
        self.projectopen = 0
        self.projectpath = ''
