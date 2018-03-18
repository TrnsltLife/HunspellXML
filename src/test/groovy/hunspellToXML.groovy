import org.sil.hunspellxml.*;

//hunspellToXML("tests/data/test.dic")
//hunspellToXML("D:/Dev/LangDev/Hunspell/hunspell-1.3.2/tests/affixes.dic")
//hunspellToXML("D:/Dev/LangDev/Hunspell/hunspell-1.3.2/tests/break.dic")
//hunspellToXML("D:/Dev/LangDev/Hunspell/hunspell-1.3.2/tests/opentaal_keepcase.dic")
//hunspellToXML("D:/Dev/LangDev/Hunspell/hunspell-1.3.2/tests/phone.dic")
//hunspellToXML((new File("src/test/groovy/data/affixes.aff")).getCanonicalPath())
//hunspellToXML((new File("src/test/groovy/data/tst_US.aff")).getCanonicalPath())
hunspellToXML((new File("src/test/groovy/data/tst_US.dic")).getCanonicalPath())

def hunspellToXML(file)
{
	def name = new File(file).getName().replaceAll(/\.(aff|dic)/, ".xml")
	def hc = new HunspellConverter(file, [suppressComments:true, suppressBlankLines:true, outputFileName:(new File('src/test/groovy/data/hunspell_test_output/' + name)).getCanonicalPath()])
	//Possible options:
	//charSet
	hc.convert()
	
	def infoLog = hc.log.infoLog.toString()
	def warningLog = hc.log.warningLog.toString()
	def errorLog = hc.log.errorLog.toString()
}