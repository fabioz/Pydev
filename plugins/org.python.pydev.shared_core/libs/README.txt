Note: lucene-core-6.1.0 is actually org.apache.lucene.core_6.1.0.v20170628-1158.jar from http://download.eclipse.org/tools/orbit/I-builds/I20170628123958/repository

Which has a fix for https://www.brainwy.com/tracker/PyDev/826


Note: snakeyaml-engine-2.1-20200105.160423-4.jar is a snapshot build from snakeyaml gotten from
https://oss.sonatype.org/content/groups/public/org/snakeyaml/snakeyaml-engine/2.1-SNAPSHOT/
(following: https://bitbucket.org/asomov/snakeyaml-engine/wiki/Installation)

This was needed because the latest release (2.0 at the time of writing) did not have a fix
for the Billion laughs attack (https://bitbucket.org/asomov/snakeyaml/wiki/Billion%20laughs%20attack).