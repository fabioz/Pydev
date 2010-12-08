import pydev_runfiles_xml_rpc
import time
from _pytest import runner
import os
from py._code import code

#===============================================================================
# PydevPlugin
#===============================================================================
class PydevPlugin:
    
    def reportCond(self, cond, filename, test, captured_output, error_contents, delta):
        '''
        @param filename: 'D:\\src\\mod1\\hello.py'
        @param test: 'TestCase.testMet1'
        @param cond: fail, error, ok
        '''
        time_str = '%.2f' % (delta,)
        pydev_runfiles_xml_rpc.NotifyTest(cond, captured_output, error_contents, filename, test, time_str)
        

    def _MockFileRepresentation(self):
        code.ReprFileLocation._original_toterminal = code.ReprFileLocation.toterminal
        
        def toterminal(self, tw):
            # filename and lineno output for each entry,
            # using an output format that most editors understand
            msg = self.message
            i = msg.find("\n")
            if i != -1:
                msg = msg[:i]

            tw.line('File "%s", line %s\n%s' %(os.path.abspath(self.path), self.lineno, msg))
            
        code.ReprFileLocation.toterminal = toterminal


    def _UninstallMockFileRepresentation(self):
        code.ReprFileLocation.toterminal = code.ReprFileLocation._original_toterminal


    def pytest_runtestloop(self, session):
        pydev_runfiles_xml_rpc.NotifyTestsCollected(len(session.session.items))
        
        try:
            #Based on the default run test loop: _pytest.session.pytest_runtestloop
            #but getting the times we need, reporting the number of tests found and notifying as each
            #test is run.
            if session.config.option.collectonly:
                return True
            
            for item in session.session.items:
                start = time.time()
                
                #Don't use this hook because we need the actual reports.
                #item.config.hook.pytest_runtest_protocol(item=item)
                reports = runner.runtestprotocol(item)
                delta = time.time() - start
                
                captured_output = ''
                error_contents = ''
                
                filename = item.fspath.strpath
                test = item.location[2]
                
                status = 'ok'
                for r in reports:
                    if r.outcome not in ('passed', 'skipped'):
                        #It has only passed, skipped and failed (no error), so, let's consider error if not on call.
                        if r.when == 'setup':
                            if status == 'ok':
                                status = 'error'
                            
                        elif r.when == 'teardown':
                            if status == 'ok':
                                status = 'error'
                            
                        else:
                            #any error in the call (not in setup or teardown) is considered a regular failure.
                            status = 'fail'
                        
                    if r.longrepr:
                        self._MockFileRepresentation()
                        try:
                            rep = r.longrepr
                            reprcrash = rep.reprcrash
                            error_contents += str(reprcrash)
                            error_contents += '\n'
                            error_contents += str(rep.reprtraceback)
                            for name, content, sep in rep.sections:
                                error_contents += sep * 40 
                                error_contents += name 
                                error_contents += sep * 40 
                                error_contents += '\n'
                                error_contents += content 
                                error_contents += '\n'
                        finally:
                            self._UninstallMockFileRepresentation()
                
                self.reportCond(status, filename, test, captured_output, error_contents, delta)
                
                if session.shouldstop:
                    raise session.Interrupted(session.shouldstop)
        finally:
            pydev_runfiles_xml_rpc.NotifyTestRunFinished()
        return True
            

        
PYDEV_PYTEST_PLUGIN_SINGLETON = PydevPlugin()
