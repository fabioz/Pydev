<map version="0.8.0">
<!-- To view this file, download free mind mapping software FreeMind from http://freemind.sourceforge.net -->
<node BACKGROUND_COLOR="#ccffff" COLOR="#000000" CREATED="1139594066796" ID="Freemind_Link_1375434945" MODIFIED="1140001041875" STYLE="bubble" TEXT="Pydev">
<edge WIDTH="thin"/>
<cloud COLOR="#99ffff"/>
<font NAME="SansSerif" SIZE="20"/>
<hook NAME="accessories/plugins/AutomaticLayout.properties"/>
<node COLOR="#0033ff" CREATED="1139594080375" ID="_" MODIFIED="1140001041531" POSITION="right" TEXT="New Project Wizard">
<edge WIDTH="thin"/>
<font NAME="SansSerif" SIZE="18"/>
<node COLOR="#00b439" CREATED="1139594135046" ID="Freemind_Link_1097477604" MODIFIED="1140001041546" TEXT="Select external projects to add to pythonpath">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1139594154062" ID="Freemind_Link_242417594" MODIFIED="1140001041546" TEXT="Select / create source folders">
<font NAME="SansSerif" SIZE="16"/>
</node>
</node>
<node COLOR="#0033ff" CREATED="1139594201281" ID="Freemind_Link_91517654" MODIFIED="1140001041546" POSITION="left" TEXT="Bugs (not in sf)">
<font NAME="SansSerif" SIZE="18"/>
<icon BUILTIN="full-1"/>
<node COLOR="#00b439" CREATED="1139594167765" ID="Freemind_Link_363994591" MODIFIED="1140001041546" TEXT="Ctrl+Alt+W: looses action">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1139594377406" ID="Freemind_Link_442163909" MODIFIED="1140001041546" TEXT="from xxx &apos;import&apos; completion should only work in the &apos;code&apos; partition">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1139595096843" ID="Freemind_Link_148953784" MODIFIED="1140001041593" TEXT="&#xa;def Foo(self):&#xa;    print _Bar()&#xa;&#xa;def _Bar():&#xa;    pass&#xa;&#xa;try:&#xa;    # Should show duplicated error (shows unused import)&#xa;    from empty2 import Bar2 as _Bar &#xa;except:&#xa;    #if available, use it&#xa;    pass&#xa;">
<edge WIDTH="thin"/>
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1139596523890" ID="Freemind_Link_1004815783" MODIFIED="1140001041640" TEXT="def Load(self):&#xa;    #Is giving Unused variable: i&#xa;    for i in xrange(10):    &#xa;        coerce(dict[i].text.strip())">
<font NAME="SansSerif" SIZE="16"/>
<icon BUILTIN="button_ok"/>
</node>
<node COLOR="#00b439" CREATED="1139915855015" ID="Freemind_Link_577285573" MODIFIED="1140001041671" TEXT="Limit the number of modules with the AST&#xa;that can be in the memory at any time.">
<font NAME="SansSerif" SIZE="16"/>
<icon BUILTIN="full-1"/>
</node>
<node COLOR="#00b439" CREATED="1140103999359" ID="Freemind_Link_9314489" MODIFIED="1140104013437" TEXT="Chandler:&#xa;&#xa;http://wiki.osafoundation.org/bin/view/Projects/GettingChandler&#xa;http://wiki.osafoundation.org/bin/view/Projects/BuildingChandler">
<font NAME="SansSerif" SIZE="16"/>
</node>
</node>
<node COLOR="#0033ff" CREATED="1139594227703" ID="Freemind_Link_1643430887" MODIFIED="1140001041687" POSITION="right" TEXT="Code Completion">
<font NAME="SansSerif" SIZE="18"/>
<node COLOR="#00b439" CREATED="1139594237328" ID="Freemind_Link_215044614" MODIFIED="1140001041687" TEXT="Code Completion for parameters: &#xa;bring the data as &apos;ctx insensitive after &apos;x&apos; characters">
<edge WIDTH="thin"/>
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1139594311703" ID="Freemind_Link_1252891293" MODIFIED="1140001041718" TEXT="filter compatible interfaces if there is some &#xa;method already declared (in the context)">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1139594344859" ID="Freemind_Link_378572787" MODIFIED="1140001041734" TEXT="filter method if there is some assert isinstance(xxx,Class)">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1139594431937" ID="Freemind_Link_1599281754" MODIFIED="1140001041734" TEXT="calltips for methods should be implemented">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1140628578109" ID="Freemind_Link_1136288053" MODIFIED="1140628630953" TEXT="Should recognize assignment in range()&#xa;E.g.:&#xa;class A:&#xa;    a,b,c = range(3)&#xa;&#xa;A.a should appear in code-completion">
<font NAME="SansSerif" SIZE="16"/>
</node>
</node>
<node COLOR="#0033ff" CREATED="1139594461453" ID="Freemind_Link_521048315" MODIFIED="1140001041734" POSITION="left" TEXT="Extensions">
<font NAME="SansSerif" SIZE="18"/>
<icon BUILTIN="full-2"/>
<node COLOR="#00b439" CREATED="1140000887734" HGAP="29" ID="Freemind_Link_617691308" MODIFIED="1140001041734" TEXT="Find references" VSHIFT="-3">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1139594480906" ID="Freemind_Link_739678165" MODIFIED="1140001041734" TEXT="Refactoring">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1139594467156" ID="Freemind_Link_1278244917" MODIFIED="1140001041734" TEXT="Pretty print">
<font NAME="SansSerif" SIZE="16"/>
<node COLOR="#990000" CREATED="1139837309343" ID="Freemind_Link_1597348608" MODIFIED="1140001041750" TEXT="&apos;comment-blocks&apos; should be &apos;resized&apos; &#xa;to the default print margin size">
<font NAME="SansSerif" SIZE="14"/>
</node>
</node>
<node COLOR="#00b439" CREATED="1139835619343" ID="Freemind_Link_1180334227" MODIFIED="1140001041765" TEXT="A &apos;token&apos; browser should be provided &#xa;(something similar to the show quick outline)">
<font NAME="SansSerif" SIZE="16"/>
<node COLOR="#990000" CREATED="1139835686546" ID="Freemind_Link_103166661" MODIFIED="1140001041796" TEXT="The user could choose to &#xa;show classes, methods, etc.">
<font NAME="SansSerif" SIZE="14"/>
</node>
</node>
<node COLOR="#00b439" CREATED="1139835594796" ID="Freemind_Link_1604171880" MODIFIED="1140001041812" TEXT="Ctrl+. should go to the next marker">
<edge WIDTH="thin"/>
<font NAME="SansSerif" SIZE="16"/>
</node>
</node>
<node COLOR="#0033ff" CREATED="1139852640359" ID="Freemind_Link_504782647" MODIFIED="1140001041812" POSITION="right" TEXT="features">
<font NAME="SansSerif" SIZE="18"/>
<icon BUILTIN="full-3"/>
<node COLOR="#00b439" CREATED="1139852644671" ID="Freemind_Link_34736996" MODIFIED="1140001041812" TEXT="making analysis">
<font NAME="SansSerif" SIZE="16"/>
<node COLOR="#990000" CREATED="1139852657828" ID="Freemind_Link_1434142630" MODIFIED="1140001041828" TEXT="Thread that does analysis should stop&#xa;when a new request is done, instead&#xa;of the other way around, as it is now.">
<font NAME="SansSerif" SIZE="14"/>
</node>
</node>
<node COLOR="#00b439" CREATED="1139999886640" ID="Freemind_Link_1097638367" MODIFIED="1140001041843" TEXT="analyze &apos;self&apos; attributes">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1139999925890" ID="Freemind_Link_901862754" MODIFIED="1140001041843" TEXT="make dict.foo give an error">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1139999953484" ID="Freemind_Link_382561698" MODIFIED="1140001041843" TEXT="Enable the user to &apos;annotate&apos; classes as &apos;dynamic&apos; &#xa;classes (and make it automatic if it has __getattr__)">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1140000211078" ID="Freemind_Link_1097971863" MODIFIED="1140001041859" TEXT="outline">
<font NAME="SansSerif" SIZE="16"/>
<node COLOR="#990000" CREATED="1140000220093" ID="Freemind_Link_683207700" MODIFIED="1140001041859" TEXT="add attributes">
<font NAME="SansSerif" SIZE="14"/>
</node>
<node COLOR="#990000" CREATED="1140000229859" ID="Freemind_Link_178802396" MODIFIED="1140001041859" TEXT="add comment blocks">
<font NAME="SansSerif" SIZE="14"/>
</node>
</node>
<node COLOR="#00b439" CREATED="1140000250828" ID="Freemind_Link_111080914" MODIFIED="1140001041859" TEXT="folding">
<font NAME="SansSerif" SIZE="16"/>
<node COLOR="#990000" CREATED="1140000254328" ID="Freemind_Link_1755892659" MODIFIED="1140001041875" TEXT="add comment blocks">
<font NAME="SansSerif" SIZE="14"/>
</node>
</node>
</node>
<node COLOR="#0033ff" CREATED="1139937919875" ID="Freemind_Link_342607119" MODIFIED="1140001041875" POSITION="left" TEXT="other improvements">
<font NAME="SansSerif" SIZE="18"/>
<node COLOR="#00b439" CREATED="1139937931906" ID="Freemind_Link_1003255905" MODIFIED="1140001041875" TEXT="make memory profiling">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1140106715906" ID="Freemind_Link_1375402745" MODIFIED="1140106864265" TEXT="Make script to handle all the buld proccess:&#xa;- Creating build: OK&#xa;- Creating e-mail message:&#xa;- Uploading files to sourceforge (partial: only html)&#xa;- Uploading files to fabioz.com&#xa;- Updating site.xml&#xa;- Printing what else needs to be done in sf">
<font NAME="SansSerif" SIZE="16"/>
</node>
</node>
</node>
</map>
