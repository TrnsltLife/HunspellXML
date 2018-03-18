import org.sil.hunspellxml.*;

testParse("src/test/groovy/data/tst_US.xml")

def testParse(file)
{
	File xmlFile = new File(file)
	def fileName = xmlFile.getName().replaceAll(/\.[xX][mM][lL]$/, "")
	def hxc = new HunspellXMLConverter(xmlFile, 
		[hunspell:true, tests:false, thesaurus:false,
		license:false, readme:false,
		firefox:false, opera:false, libreOffice:false,
		//hunspellFileName:fileName,
		customPath:"/",
		relaxNG:true, runTests:true])
	hxc.convert()
	
	def data = hxc.parser?.data
	def infoLog = hxc.log.infoLog.toString()
	def warningLog = hxc.log.warningLog.toString()
	def errorLog = hxc.log.errorLog.toString()
}