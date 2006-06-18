<map version="0.8.0">
<!-- To view this file, download free mind mapping software FreeMind from http://freemind.sourceforge.net -->
<node BACKGROUND_COLOR="#ccffff" COLOR="#000000" CREATED="1139594066796" ID="Freemind_Link_1375434945" MODIFIED="1140001041875" STYLE="bubble" TEXT="Pydev">
<edge WIDTH="thin"/>
<cloud COLOR="#99ffff"/>
<font NAME="SansSerif" SIZE="20"/>
<hook NAME="accessories/plugins/AutomaticLayout.properties"/>
<node COLOR="#0033ff" CREATED="1139594080375" FOLDED="true" ID="_" MODIFIED="1140001041531" POSITION="right" TEXT="New Project Wizard">
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
<node COLOR="#00b439" CREATED="1139595096843" ID="Freemind_Link_148953784" MODIFIED="1140001041593" TEXT="&#xa;def Foo(self):&#xa;    print _Bar()&#xa;&#xa;def _Bar():&#xa;    pass&#xa;&#xa;try:&#xa;    # Should show duplicated error (shows unused import)&#xa;    from empty2 import Bar2 as _Bar &#xa;except:&#xa;    #if available, use it&#xa;    pass&#xa;">
<edge WIDTH="thin"/>
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1140103999359" ID="Freemind_Link_9314489" MODIFIED="1148498578002" TEXT="Chandler:&#xa;&#xa;http://wiki.osafoundation.org/bin/view/Projects/GettingChandler&#xa;http://wiki.osafoundation.org/bin/view/Projects/BuildingChandler">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1148498545612" FOLDED="true" ID="Freemind_Link_92003531" MODIFIED="1148498581221" TEXT="Fixed">
<font NAME="SansSerif" SIZE="16"/>
<icon BUILTIN="button_ok"/>
<node COLOR="#990000" CREATED="1139915855015" ID="Freemind_Link_577285573" MODIFIED="1148498554237" TEXT="Limit the number of modules with the AST&#xa;that can be in the memory at any time.">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="button_ok"/>
</node>
<node COLOR="#990000" CREATED="1139596523890" ID="Freemind_Link_1004815783" MODIFIED="1148498557174" TEXT="def Load(self):&#xa;    #Is giving Unused variable: i&#xa;    for i in xrange(10):    &#xa;        coerce(dict[i].text.strip())">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="button_ok"/>
</node>
<node COLOR="#990000" CREATED="1139594167765" ID="Freemind_Link_363994591" MODIFIED="1148498560659" TEXT="Ctrl+Alt+W: looses action">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="button_ok"/>
</node>
<node COLOR="#990000" CREATED="1139937931906" ID="Freemind_Link_1003255905" MODIFIED="1150237756437" TEXT="make memory profiling">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="button_ok"/>
</node>
<node COLOR="#990000" CREATED="1141847037452" ID="Freemind_Link_860460502" MODIFIED="1150237857125" TEXT=" result = {}, F3 no result d&#xe1; erro.">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="button_ok"/>
</node>
<node COLOR="#990000" CREATED="1139594377406" ID="Freemind_Link_442163909" MODIFIED="1150238062421" TEXT="from xxx &apos;import&apos; completion should only work in the &apos;code&apos; partition">
<font NAME="SansSerif" SIZE="14"/>
</node>
</node>
</node>
<node COLOR="#0033ff" CREATED="1139594227703" FOLDED="true" ID="Freemind_Link_1643430887" MODIFIED="1142643906171" POSITION="right" TEXT="Code Completion">
<font NAME="SansSerif" SIZE="18"/>
<node COLOR="#00b439" CREATED="1139594237328" ID="Freemind_Link_215044614" MODIFIED="1140001041687" TEXT="Code Completion for parameters: &#xa;bring the data as &apos;ctx insensitive after &apos;x&apos; characters">
<edge WIDTH="thin"/>
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1139594311703" ID="Freemind_Link_1252891293" MODIFIED="1142644060562" STYLE="bubble" TEXT="filter compatible interfaces if there is some &#xa;method already declared (in the context)">
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
<node COLOR="#990000" CREATED="1150238086375" ID="Freemind_Link_1906576894" MODIFIED="1150238101468" TEXT="Rename variables">
<font NAME="SansSerif" SIZE="14"/>
</node>
<node COLOR="#990000" CREATED="1150238102781" ID="Freemind_Link_1686959584" MODIFIED="1150238107421" TEXT="Extract Method">
<font NAME="SansSerif" SIZE="14"/>
<node COLOR="#111111" CREATED="1150238183500" ID="Freemind_Link_1994392803" MODIFIED="1150238288390" TEXT="Extract to class (self) -- if inside class"/>
<node COLOR="#111111" CREATED="1150238199984" ID="Freemind_Link_1246361948" MODIFIED="1150238217203" TEXT="Extract to module (if uses self, pass as firs parameter)"/>
<node COLOR="#111111" CREATED="1150238218937" ID="Freemind_Link_1819546957" MODIFIED="1150238228703" TEXT="Extract to some other module"/>
</node>
<node COLOR="#990000" CREATED="1150238111562" ID="Freemind_Link_1218789951" MODIFIED="1150238118218" TEXT="Inline variable">
<font NAME="SansSerif" SIZE="14"/>
</node>
</node>
<node COLOR="#00b439" CREATED="1139594467156" FOLDED="true" ID="Freemind_Link_1278244917" MODIFIED="1140001041734" TEXT="Pretty print">
<font NAME="SansSerif" SIZE="16"/>
<node COLOR="#990000" CREATED="1139837309343" ID="Freemind_Link_1597348608" MODIFIED="1140001041750" TEXT="&apos;comment-blocks&apos; should be &apos;resized&apos; &#xa;to the default print margin size">
<font NAME="SansSerif" SIZE="14"/>
</node>
</node>
<node COLOR="#00b439" CREATED="1148498592564" FOLDED="true" ID="Freemind_Link_335726182" MODIFIED="1148498595627" TEXT="Fixed">
<font NAME="SansSerif" SIZE="16"/>
<node COLOR="#990000" CREATED="1139835619343" FOLDED="true" ID="Freemind_Link_1180334227" MODIFIED="1148498600033" TEXT="A &apos;token&apos; browser should be provided &#xa;(something similar to the show quick outline)">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="button_ok"/>
<node COLOR="#111111" CREATED="1139835686546" ID="Freemind_Link_103166661" MODIFIED="1148498600033" TEXT="The user could choose to &#xa;show classes, methods, etc.">
<font NAME="SansSerif" SIZE="12"/>
</node>
</node>
<node COLOR="#990000" CREATED="1139835594796" ID="Freemind_Link_1604171880" MODIFIED="1148498703156" TEXT="Ctrl+. should go to the next marker">
<edge WIDTH="thin"/>
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="button_ok"/>
</node>
</node>
</node>
<node COLOR="#0033ff" CREATED="1139852640359" ID="Freemind_Link_504782647" MODIFIED="1140001041812" POSITION="right" TEXT="features">
<font NAME="SansSerif" SIZE="18"/>
<icon BUILTIN="full-3"/>
<node COLOR="#00b439" CREATED="1139999886640" ID="Freemind_Link_1097638367" MODIFIED="1140001041843" TEXT="analyze &apos;self&apos; attributes">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1139999925890" ID="Freemind_Link_901862754" MODIFIED="1140001041843" TEXT="make dict.foo give an error">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1139999953484" ID="Freemind_Link_382561698" MODIFIED="1140001041843" TEXT="Enable the user to &apos;annotate&apos; classes as &apos;dynamic&apos; &#xa;classes (and make it automatic if it has __getattr__)">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1140000250828" ID="Freemind_Link_111080914" MODIFIED="1140001041859" TEXT="folding">
<font NAME="SansSerif" SIZE="16"/>
<node COLOR="#990000" CREATED="1140000254328" ID="Freemind_Link_1755892659" MODIFIED="1140001041875" TEXT="add comment blocks">
<font NAME="SansSerif" SIZE="14"/>
</node>
</node>
<node COLOR="#00b439" CREATED="1148498654641" FOLDED="true" ID="Freemind_Link_107838497" MODIFIED="1148498657501" TEXT="Fixed">
<font NAME="SansSerif" SIZE="16"/>
<node COLOR="#990000" CREATED="1139852644671" ID="Freemind_Link_34736996" MODIFIED="1148498659235" TEXT="making analysis">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="button_ok"/>
<node COLOR="#111111" CREATED="1139852657828" ID="Freemind_Link_1434142630" MODIFIED="1148498659235" TEXT="Thread that does analysis should stop&#xa;when a new request is done, instead&#xa;of the other way around, as it is now.">
<font NAME="SansSerif" SIZE="12"/>
</node>
</node>
<node COLOR="#990000" CREATED="1141658334352" FOLDED="true" ID="Freemind_Link_1814459883" MODIFIED="1148498673735" TEXT="auto-indent &#xa;(Jorge Godoy &lt;godoy@ieee.org&gt;)">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="button_ok"/>
<node COLOR="#111111" CREATED="1141658346899" ID="Freemind_Link_1879754442" MODIFIED="1148498673735" TEXT="moving forward with sucessive TABs&#xa;pressed.  When I press TAB on Emacs it use a soft tab (i.e. what Eclips calls&#xa;&apos;space-tab&apos;) and indent my code to the next indentation level and stop&#xa;indenting.  Even when in the middle of the line and if I&apos;m in the beginning of&#xa;the line it moves the cursor to the beginning of the code.&#xa;">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node COLOR="#111111" CREATED="1141658490694" ID="Freemind_Link_836615977" MODIFIED="1148498673735" TEXT="TAB means go to the next indentation level, no matter where the line starts. &#xa;">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node COLOR="#111111" CREATED="1141658532282" ID="Freemind_Link_862633378" MODIFIED="1148498673735" TEXT="Using BACKSPACE to remove indentation from my code: if I&apos;m in a&#xa;nesting level and I want to move my code one level down, I have to press&#xa;backspace &apos;n&apos; times (n == the number of spaces used by soft tabs in my Eclipse&#xa;configuration) on lines where I have code.  (The menu option &apos;Python&#xa;backspace&apos; doesn&apos;t work.)&#xa;">
<font NAME="SansSerif" SIZE="12"/>
</node>
</node>
<node COLOR="#990000" CREATED="1140000211078" ID="Freemind_Link_1097971863" MODIFIED="1150237910656" TEXT="outline">
<font NAME="SansSerif" SIZE="14"/>
<node COLOR="#111111" CREATED="1140000220093" ID="Freemind_Link_683207700" MODIFIED="1150237910656" TEXT="add attributes">
<font NAME="SansSerif" SIZE="12"/>
<icon BUILTIN="button_ok"/>
</node>
<node COLOR="#111111" CREATED="1140000229859" ID="Freemind_Link_178802396" MODIFIED="1150237910671" TEXT="add comment blocks">
<font NAME="SansSerif" SIZE="12"/>
<icon BUILTIN="button_ok"/>
</node>
</node>
</node>
<node COLOR="#00b439" CREATED="1148498768420" ID="Freemind_Link_366418378" MODIFIED="1148498794654" TEXT="Making ctrl+2+kill should also clear the compiled modules cache">
<font NAME="SansSerif" SIZE="16"/>
</node>
</node>
<node COLOR="#0033ff" CREATED="1139937919875" ID="Freemind_Link_342607119" MODIFIED="1140001041875" POSITION="left" TEXT="other improvements">
<font NAME="SansSerif" SIZE="18"/>
<node COLOR="#00b439" CREATED="1140106715906" ID="Freemind_Link_1375402745" MODIFIED="1140106864265" TEXT="Make script to handle all the buld proccess:&#xa;- Creating build: OK&#xa;- Creating e-mail message:&#xa;- Uploading files to sourceforge (partial: only html)&#xa;- Uploading files to fabioz.com&#xa;- Updating site.xml&#xa;- Printing what else needs to be done in sf">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1150661220875" ID="Freemind_Link_332273753" MODIFIED="1150661230484" TEXT="If project is changed to jython, and there is no jython interpreter &#xa;configured, a message should be shown to the user.">
<font NAME="SansSerif" SIZE="16"/>
</node>
<node COLOR="#00b439" CREATED="1150661235406" ID="Freemind_Link_266065152" MODIFIED="1150661261218" TEXT="When trying to run with &apos;old&apos; interpreter, let the user know about it.">
<font NAME="SansSerif" SIZE="16"/>
</node>
</node>
</node>
</map>
