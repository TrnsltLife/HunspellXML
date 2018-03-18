import org.sil.hunspellxml.*;  

String file
//file = "D:/Dev/LangDev/Hunspell/HunspellXML/hunspell-tests/lingala-word-test.xml"
//file = "D:/Dev/LangDev/Hunspell/HunspellXML/ln_test/ln_hunspell.xml"
//file = "D:/Dev/LangDev/Hunspell/HunspellXML/hunspell-tests/slash-test.xml"
//file = "D:/Dev/LangDev/Hunspell/HunspellXML/hunspell-tests/lingalaSP2P1-Test.xml"
//file = "D:/Dev/LangDev/Hunspell/HunspellXML/hunspell-tests/forbidden-test.xml"
//file = "D:/Dev/LangDev/Hunspell/HunspellXML/hunspell-tests/circumfix-test.xml"
//file = "D:/Dev/LangDev/SpellCheck/test-continuation-rules/word-[P1+S1]-(S1)-S2.xml"
//file = "D:/Dev/LangDev/SpellCheck/test-continuation-rules/word-S1-CF[P1-S2].xml"
//file = "D:/Dev/LangDev/SpellCheck/test-continuation-rules/circumfix-sp.xml"
//file = "D:/Dev/LangDev/SpellCheck/test-continuation-rules/circumfix-ps.xml"
file = "D:/Dev/LangDev/SpellCheck/test-continuation-rules/circumfix-s-sp.xml"
//file = "D:/Dev/LangDev/SpellCheck/test-continuation-rules/circumfix-p-ps.xml"

File xmlFile = new File(file)
def fileName = xmlFile.getName().replaceAll(/\.[xX][mM][lL]$/, "")

def hxc = new HunspellXMLConverter(xmlFile, 
	[thesaurus:false,
	license:false, readme:false,
	firefox:false, opera:false, libreOffice:false,
	hunspellFileName:fileName,
	customPath:"/temp",
	relaxNG:true,
	runTests:true])

//def hxc = new HunspellXMLConverter(xmlFile)
hxc.convert()

if(hxc?.parser?.check?.checkMap)
{
	hxc.log.info("List of affixation paths in your dictionary:")
}
for(entry in hxc?.parser?.check?.checkMap)
{
	def route = entry.value.route
	StringBuffer sb = new StringBuffer()
	hxc.log.info(hxc.parser.check.shortFormatRoute(route, true))
}
