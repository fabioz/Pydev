#
# This ProGuard configuration file illustrates how to process a program
# library, such that it remains usable as a library.
# Usage:
#     java -jar proguard.jar @library.pro
#

# Specify the input jars, output jars, and library jars.
# In this case, the input jar is the program library that we want to process.

-injars  original.jar
-outjars jython.jar

-libraryjars  <java.home>/lib/rt.jar

-ignorewarnings

-keepparameternames

-dontobfuscate

-optimizations !code/allocation/variable

# Your library may contain more items that need to be preserved; 
# typically classes that are dynamically created using Class.forName:

# -keep public class mypackage.MyClass
# -keep public interface mypackage.MyInterface
# -keep public class * implements mypackage.MyInterface

-keep class jni.*{
  public protected private *;
}
-keep class jni.Darwin.*{
  public protected private *;
}
-keep class jni.arm-Linux.*{
  public protected private *;
}
-keep class jni.i386-Linux.*{
  public protected private *;
}
-keep class jni.i386-SunOS.*{
  public protected private *;
}
-keep class jni.i386-Windows.*{
  public protected private *;
}
-keep class jni.sparcv9-SunOS.*{
  public protected private *;
}
-keep class jni.x86_64-FreeBSD.*{
  public protected private *;
}
-keep class jni.x86_64-Linux.*{
  public protected private *;
}
-keep class jni.x86_64-SunOS.*{
  public protected private *;
}
-keep class jni.x86_64-Windows.*{
  public protected private *;
}
-keep class jnr.*{
  public protected private *;
}
-keep class jnr.constants.*{
  public protected private *;
}
-keep class jnr.constants.platform.*{
  public protected private *;
}
-keep class jnr.constants.platform.darwin.*{
  public protected private *;
}
-keep class jnr.constants.platform.fake.*{
  public protected private *;
}
-keep class jnr.constants.platform.freebsd.*{
  public protected private *;
}
-keep class jnr.constants.platform.linux.*{
  public protected private *;
}
-keep class jnr.constants.platform.openbsd.*{
  public protected private *;
}
-keep class jnr.constants.platform.sunos.*{
  public protected private *;
}
-keep class jnr.constants.platform.windows.*{
  public protected private *;
}
-keep class jnr.ffi.*{
  public protected private *;
}
-keep class jnr.ffi.annotations.*{
  public protected private *;
}
-keep class jnr.ffi.byref.*{
  public protected private *;
}
-keep class jnr.ffi.mapper.*{
  public protected private *;
}
-keep class jnr.ffi.provider.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.arm.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.arm.linux.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.i386.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.i386.darwin.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.i386.freebsd.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.i386.linux.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.i386.openbsd.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.i386.solaris.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.i386.windows.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.mips.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.mips.linux.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.mipsel.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.mipsel.linux.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.powerpc.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.powerpc.aix.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.powerpc.darwin.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.powerpc.linux.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.s390.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.s390.linux.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.s390x.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.s390x.linux.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.sparc.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.sparc.solaris.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.sparcv9.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.sparcv9.solaris.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.x86_64.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.x86_64.darwin.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.x86_64.freebsd.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.x86_64.linux.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.x86_64.openbsd.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.x86_64.solaris.*{
  public protected private *;
}
-keep class jnr.ffi.provider.jffi.platform.x86_64.windows.*{
  public protected private *;
}
-keep class jnr.ffi.types.*{
  public protected private *;
}
-keep class jnr.ffi.util.*{
  public protected private *;
}
-keep class jnr.ffi.util.ref.*{
  public protected private *;
}
-keep class jnr.ffi.util.ref.internal.*{
  public protected private *;
}
-keep class jnr.netdb.*{
  public protected private *;
}
-keep class jnr.posix.*{
  public protected private *;
}
-keep class jnr.posix.util.*{
  public protected private *;
}
-keep class org.antlr.*{
  public protected private *;
}
-keep class org.antlr.runtime.*{
  public protected private *;
}
-keep class org.antlr.runtime.debug.*{
  public protected private *;
}
-keep class org.antlr.runtime.misc.*{
  public protected private *;
}
-keep class org.antlr.runtime.tree.*{
  public protected private *;
}
-keep class org.python.antlr.*{
  public protected private *;
}
-keep class org.python.antlr.Python.token.*{
  public protected private *;
}
-keep class org.python.antlr.PythonPartial.token.*{
  public protected private *;
}
-keep class org.python.antlr.adapter.*{
  public protected private *;
}
-keep class org.python.antlr.ast.*{
  public protected private *;
}
-keep class org.python.antlr.base.*{
  public protected private *;
}
-keep class org.python.antlr.op.*{
  public protected private *;
}
-keep class org.python.antlr.runtime.*{
  public protected private *;
}
-keep class org.python.antlr.runtime.debug.*{
  public protected private *;
}
-keep class org.python.antlr.runtime.misc.*{
  public protected private *;
}
-keep class org.python.antlr.runtime.tree.*{
  public protected private *;
}
-keep class org.python.compiler.*{
  public protected private *;
}
-keep class org.python.compiler.custom_proxymaker.*{
  public protected private *;
}
-keep class org.python.core.*{
  public protected private *;
}
-keep class org.python.core.adapter.*{
  public protected private *;
}
-keep class org.python.core.buffer.*{
  public protected private *;
}
-keep class org.python.core.io.*{
  public protected private *;
}
-keep class org.python.core.stringlib.*{
  public protected private *;
}
-keep class org.python.core.util.*{
  public protected private *;
}
-keep class org.python.expose.*{
  public protected private *;
}
-keep class org.python.indexer.*{
  public protected private *;
}
-keep class org.python.jsr223.*{
  public protected private *;
}
-keep class org.python.modules.*{
  public protected private *;
}
-keep class org.python.modules._collections.*{
  public protected private *;
}
-keep class org.python.modules._csv.*{
  public protected private *;
}
-keep class org.python.modules._functools.*{
  public protected private *;
}
-keep class org.python.modules._io.*{
  public protected private *;
}
-keep class org.python.modules._threading.*{
  public protected private *;
}
-keep class org.python.modules._weakref.*{
  public protected private *;
}
-keep class org.python.modules.bz2.*{
  public protected private *;
}
-keep class org.python.modules.itertools.*{
  public protected private *;
}
-keep class org.python.modules.jffi.*{
  public protected private *;
}
-keep class org.python.modules.posix.*{
  public protected private *;
}
-keep class org.python.modules.random.*{
  public protected private *;
}
-keep class org.python.modules.sre.*{
  public protected private *;
}
-keep class org.python.modules.thread.*{
  public protected private *;
}
-keep class org.python.modules.time.*{
  public protected private *;
}
-keep class org.python.modules.ucnhash.da.*{
  public protected private *;
}
-keep class org.python.modules.zipimport.*{
  public protected private *;
}
-keep class org.python.netty.*{
  public protected private *;
}
-keep class org.python.netty.bootstrap.*{
  public protected private *;
}
-keep class org.python.netty.buffer.*{
  public protected private *;
}
-keep class org.python.netty.channel.*{
  public protected private *;
}
-keep class org.python.netty.channel.embedded.*{
  public protected private *;
}
-keep class org.python.netty.channel.group.*{
  public protected private *;
}
-keep class org.python.netty.channel.local.*{
  public protected private *;
}
-keep class org.python.netty.channel.nio.*{
  public protected private *;
}
-keep class org.python.netty.channel.oio.*{
  public protected private *;
}
-keep class org.python.netty.channel.socket.*{
  public protected private *;
}
-keep class org.python.netty.channel.socket.nio.*{
  public protected private *;
}
-keep class org.python.netty.channel.socket.oio.*{
  public protected private *;
}
-keep class org.python.netty.handler.*{
  public protected private *;
}
-keep class org.python.netty.handler.codec.*{
  public protected private *;
}
-keep class org.python.netty.handler.codec.base64.*{
  public protected private *;
}
-keep class org.python.netty.handler.codec.bytes.*{
  public protected private *;
}
-keep class org.python.netty.handler.codec.compression.*{
  public protected private *;
}
-keep class org.python.netty.handler.codec.marshalling.*{
  public protected private *;
}
-keep class org.python.netty.handler.codec.protobuf.*{
  public protected private *;
}
-keep class org.python.netty.handler.codec.serialization.*{
  public protected private *;
}
-keep class org.python.netty.handler.codec.string.*{
  public protected private *;
}
-keep class org.python.netty.handler.logging.*{
  public protected private *;
}
-keep class org.python.netty.handler.ssl.*{
  public protected private *;
}
-keep class org.python.netty.handler.stream.*{
  public protected private *;
}
-keep class org.python.netty.handler.timeout.*{
  public protected private *;
}
-keep class org.python.netty.handler.traffic.*{
  public protected private *;
}
-keep class org.python.netty.util.*{
  public protected private *;
}
-keep class org.python.netty.util.concurrent.*{
  public protected private *;
}
-keep class org.python.netty.util.internal.*{
  public protected private *;
}
-keep class org.python.netty.util.internal.chmv8.*{
  public protected private *;
}
-keep class org.python.netty.util.internal.logging.*{
  public protected private *;
}
-keep class org.python.objectweb.*{
  public protected private *;
}
-keep class org.python.objectweb.asm.*{
  public protected private *;
}
-keep class org.python.objectweb.asm.commons.*{
  public protected private *;
}
-keep class org.python.objectweb.asm.signature.*{
  public protected private *;
}
-keep class org.python.objectweb.asm.util.*{
  public protected private *;
}
-keep class org.python.util.*{
  public protected private *;
}
