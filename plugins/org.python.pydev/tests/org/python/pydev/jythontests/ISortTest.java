package org.python.pydev.jythontests;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.TestDependent;
import org.python.pydev.jython.JythonModules;
import org.python.pydev.ui.BundleInfoStub;

import junit.framework.TestCase;

public class ISortTest extends TestCase {

    public void testJythonTests() throws Exception {
        CorePlugin.setBundleInfo(new BundleInfoStub());

        File f = new File(TestDependent.TEST_PYSRC_TESTING_LOC + "extendable/isortWithCfg/snippet.py");

        byte[] encoded = Files.readAllBytes(f.toPath());
        String fileContents = new String(encoded, StandardCharsets.UTF_8);

        Set<String> knownThirdParty = new HashSet<String>();
        String[] knownThirdPartyContent = new String[] { "wincertstore", "six", "cython", "wheel", "asgiref",
                "setuptools", "numpy",
                "easy_install", "mkl", "sqlparse", "PIL", "django", "pip", "pkg_resources", "certifi", "olefile",
                "pytz", "Cython", "mkl_fft", "pyximport" };

        for (String s : knownThirdPartyContent) {
            knownThirdParty.add(s);
        }

        for (String s : knownThirdPartyContent) {
            fileContents.concat(s);
        }

        String actual = JythonModules.makeISort(fileContents, f, knownThirdParty);

        String expected = "" +
                "from __future__ import absolute_import\n" +
                "\n" +
                "import os\n" +
                "import sys\n" +
                "\n" +
                "from my_lib import Object, Object2, Object3\n" +
                "from third_party import lib1, lib2, lib3, lib4, lib5, lib6, lib7, lib8, lib9, lib10, lib11, lib12, lib13, lib14, lib15\n"
                +
                "\n" +
                "print(\"Hey\")\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "print(\"yo\")\n";

        assertEquals(expected, actual);

    }

    public void testJythonTests2() throws Exception {

        CorePlugin.setBundleInfo(new BundleInfoStub());

        File f = new File(TestDependent.TEST_PYSRC_TESTING_LOC + "extendable/isortWithoutCfg/snippet.py");

        byte[] encoded = Files.readAllBytes(f.toPath());
        String fileContents = new String(encoded, StandardCharsets.UTF_8);

        Set<String> knownThirdParty = new HashSet<String>();
        String[] knownThirdPartyContent = new String[] { "wincertstore", "six", "cython", "wheel", "asgiref",
                "setuptools", "numpy",
                "easy_install", "mkl", "sqlparse", "PIL", "django", "pip", "pkg_resources", "certifi", "olefile",
                "pytz", "Cython", "mkl_fft", "pyximport" };

        for (String s : knownThirdPartyContent) {
            knownThirdParty.add(s);
        }

        for (String s : knownThirdPartyContent) {
            fileContents.concat(s);
        }

        String actual = JythonModules.makeISort(fileContents, f, knownThirdParty);

        String expected = "from __future__ import absolute_import\n" +
                "\n" +
                "import os\n" +
                "import sys\n" +
                "\n" +
                "from my_lib import Object, Object2, Object3\n" +
                "from third_party import (lib1, lib2, lib3, lib4, lib5, lib6, lib7, lib8, lib9,\n" +
                "                         lib10, lib11, lib12, lib13, lib14, lib15)\n" +
                "\n" +
                "print(\"Hey\")\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "print(\"yo\")\n";

        assertEquals(expected, actual);

    }
}
