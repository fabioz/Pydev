<map version="0.8.0">
<!-- To view this file, download free mind mapping software FreeMind from http://freemind.sourceforge.net -->
<node CREATED="1139594066796" ID="Freemind_Link_1375434945" MODIFIED="1139596875734" STYLE="bubble" TEXT="Pydev">
<cloud COLOR="#99ffff"/>
<node CREATED="1139594080375" ID="_" MODIFIED="1139596863015" POSITION="right" TEXT="New Project Wizard">
<edge WIDTH="thin"/>
<node CREATED="1139594135046" ID="Freemind_Link_1097477604" MODIFIED="1139594152796" TEXT="Select external projects to add to pythonpath"/>
<node CREATED="1139594154062" ID="Freemind_Link_242417594" MODIFIED="1139594162953" TEXT="Select / create source folders"/>
</node>
<node CREATED="1139594201281" ID="Freemind_Link_91517654" MODIFIED="1139835823296" POSITION="left" TEXT="Bugs (not in sf)">
<node CREATED="1139594167765" ID="Freemind_Link_363994591" MODIFIED="1139594196687" TEXT="Ctrl+Alt+W: looses action"/>
<node CREATED="1139594377406" ID="Freemind_Link_442163909" MODIFIED="1139596778843" TEXT="from xxx &apos;import&apos; completion should only work in the &apos;code&apos; partition"/>
<node CREATED="1139595096843" ID="Freemind_Link_148953784" MODIFIED="1139596781765" TEXT="&#xa;def Foo(self):&#xa;    print _Bar()&#xa;&#xa;def _Bar():&#xa;    pass&#xa;&#xa;try:&#xa;    # Should show duplicated error (shows unused import)&#xa;    from empty2 import Bar2 as _Bar &#xa;except:&#xa;    #if available, use it&#xa;    pass&#xa;">
<edge WIDTH="thin"/>
</node>
<node CREATED="1139596523890" ID="Freemind_Link_1004815783" MODIFIED="1139596670812" TEXT="def Load(self):&#xa;    #Is giving Unused variable: i&#xa;    for i in xrange(10):    &#xa;        coerce(dict[i].text.strip())"/>
</node>
<node CREATED="1139594227703" ID="Freemind_Link_1643430887" MODIFIED="1139594232546" POSITION="right" TEXT="Code Completion">
<node CREATED="1139594237328" ID="Freemind_Link_215044614" MODIFIED="1139835764546" TEXT="Code Completion for parameters: &#xa;bring the data as &apos;ctx insensitive after &apos;x&apos; characters">
<edge WIDTH="thin"/>
</node>
<node CREATED="1139594311703" ID="Freemind_Link_1252891293" MODIFIED="1139835753078" TEXT="filter compatible interfaces if there is some &#xa;method already declared (in the context)"/>
<node CREATED="1139594344859" ID="Freemind_Link_378572787" MODIFIED="1139594366187" TEXT="filter method if there is some assert isinstance(xxx,Class)"/>
<node CREATED="1139594431937" ID="Freemind_Link_1599281754" MODIFIED="1139596817750" TEXT="calltips for methods should be implemented"/>
</node>
<node CREATED="1139594461453" ID="Freemind_Link_521048315" MODIFIED="1139594464906" POSITION="left" TEXT="Extensions">
<node CREATED="1139594467156" ID="Freemind_Link_1278244917" MODIFIED="1139594642328" TEXT="Pretty print">
<node CREATED="1139837309343" ID="Freemind_Link_1597348608" MODIFIED="1139837383812" TEXT="&apos;comment-blocks&apos; should be &apos;resized&apos; &#xa;to the default print margin size"/>
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
</node>
</map>
