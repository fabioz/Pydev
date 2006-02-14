<map version="0.8.0">
<!-- To view this file, download free mind mapping software FreeMind from http://freemind.sourceforge.net -->
<node BACKGROUND_COLOR="#ccffff" CREATED="1139594066796" ID="Freemind_Link_1375434945" MODIFIED="1139852901500" STYLE="bubble" TEXT="Pydev">
<edge WIDTH="thin"/>
<cloud COLOR="#99ffff"/>
<font NAME="SansSerif" SIZE="15"/>
<node CREATED="1139594080375" ID="_" MODIFIED="1139596863015" POSITION="right" TEXT="New Project Wizard">
<edge WIDTH="thin"/>
<node COLOR="#000000" CREATED="1139594135046" ID="Freemind_Link_1097477604" MODIFIED="1139852838265" TEXT="Select external projects to add to pythonpath"/>
<node CREATED="1139594154062" ID="Freemind_Link_242417594" MODIFIED="1139594162953" TEXT="Select / create source folders"/>
</node>
<node CREATED="1139594201281" ID="Freemind_Link_91517654" MODIFIED="1139852751656" POSITION="left" TEXT="Bugs (not in sf)">
<icon BUILTIN="full-1"/>
<node CREATED="1139594167765" ID="Freemind_Link_363994591" MODIFIED="1139594196687" TEXT="Ctrl+Alt+W: looses action"/>
<node CREATED="1139594377406" ID="Freemind_Link_442163909" MODIFIED="1139596778843" TEXT="from xxx &apos;import&apos; completion should only work in the &apos;code&apos; partition"/>
<node CREATED="1139595096843" ID="Freemind_Link_148953784" MODIFIED="1139596781765" TEXT="&#xa;def Foo(self):&#xa;    print _Bar()&#xa;&#xa;def _Bar():&#xa;    pass&#xa;&#xa;try:&#xa;    # Should show duplicated error (shows unused import)&#xa;    from empty2 import Bar2 as _Bar &#xa;except:&#xa;    #if available, use it&#xa;    pass&#xa;">
<edge WIDTH="thin"/>
</node>
<node CREATED="1139596523890" ID="Freemind_Link_1004815783" MODIFIED="1139596670812" TEXT="def Load(self):&#xa;    #Is giving Unused variable: i&#xa;    for i in xrange(10):    &#xa;        coerce(dict[i].text.strip())"/>
<node CREATED="1139915855015" ID="Freemind_Link_577285573" MODIFIED="1139915931671" TEXT="Limit the number of modules with the AST&#xa;that can be in the memory at any time.">
<icon BUILTIN="full-1"/>
</node>
</node>
<node CREATED="1139594227703" ID="Freemind_Link_1643430887" MODIFIED="1139594232546" POSITION="right" TEXT="Code Completion">
<node CREATED="1139594237328" ID="Freemind_Link_215044614" MODIFIED="1139835764546" TEXT="Code Completion for parameters: &#xa;bring the data as &apos;ctx insensitive after &apos;x&apos; characters">
<edge WIDTH="thin"/>
</node>
<node CREATED="1139594311703" ID="Freemind_Link_1252891293" MODIFIED="1139835753078" TEXT="filter compatible interfaces if there is some &#xa;method already declared (in the context)"/>
<node CREATED="1139594344859" ID="Freemind_Link_378572787" MODIFIED="1139594366187" TEXT="filter method if there is some assert isinstance(xxx,Class)"/>
<node CREATED="1139594431937" ID="Freemind_Link_1599281754" MODIFIED="1139596817750" TEXT="calltips for methods should be implemented"/>
</node>
<node CREATED="1139594461453" ID="Freemind_Link_521048315" MODIFIED="1139852756875" POSITION="left" TEXT="Extensions">
<icon BUILTIN="full-2"/>
<node CREATED="1139594467156" ID="Freemind_Link_1278244917" MODIFIED="1139594642328" TEXT="Pretty print">
<node CREATED="1139837309343" ID="Freemind_Link_1597348608" MODIFIED="1139852792765" TEXT="&apos;comment-blocks&apos; should be &apos;resized&apos; &#xa;to the default print margin size">
<font NAME="SansSerif" SIZE="12"/>
</node>
</node>
<node CREATED="1139594480906" ID="Freemind_Link_739678165" MODIFIED="1139594644687" TEXT="Refactoring"/>
<node CREATED="1139835594796" ID="Freemind_Link_1604171880" MODIFIED="1139837476250" TEXT="Ctrl+. should go to the next marker">
<edge WIDTH="thin"/>
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1139835619343" ID="Freemind_Link_1180334227" MODIFIED="1139835730625" TEXT="A &apos;token&apos; browser should be provided &#xa;(something similar to the show quick outline)">
<node CREATED="1139835686546" ID="Freemind_Link_103166661" MODIFIED="1139835736765" TEXT="The user could choose to &#xa;show classes, methods, etc."/>
</node>
</node>
<node CREATED="1139852640359" ID="Freemind_Link_504782647" MODIFIED="1139852883906" POSITION="right" TEXT="features">
<font NAME="SansSerif" SIZE="12"/>
<icon BUILTIN="full-3"/>
<node CREATED="1139852644671" ID="Freemind_Link_34736996" MODIFIED="1139852656953" TEXT="making analysis">
<node CREATED="1139852657828" ID="Freemind_Link_1434142630" MODIFIED="1139852688406" TEXT="Thread that does analysis should stop&#xa;when a new request is done, instead&#xa;of the other way around, as it is now."/>
</node>
</node>
<node CREATED="1139937919875" ID="Freemind_Link_342607119" MODIFIED="1139937929359" POSITION="left" TEXT="other improvements">
<node CREATED="1139937931906" ID="Freemind_Link_1003255905" MODIFIED="1139937941671" TEXT="make memory profiling"/>
</node>
</node>
</map>
