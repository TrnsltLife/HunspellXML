import org.sil.hunspellxml.*;

//Test the warnings from HunspellXMLFlagChecker without creating any output files
testFileErrors("tests/data/circumfix1.xml"){d,i,w,e-> w.contains("Circumfix Path Error: The circumfix flag is never valid in the dictionary words' continuation rules.")}
testFileErrors("tests/data/circumfix2.xml"){d,i,w,e-> !w.contains("Circumfix Path Error:") && !w.contains("Circumfix Order Error:")}
testFileErrors("tests/data/circumfix3.xml"){d,i,w,e-> w.contains("Circumfix Order Error: When using the circumfix flag with COMPLEXPREFIXES disabled (2-suffix mode), the first affix must be a suffix and the second affix must be a prefix.")}
testFileErrors("tests/data/circumfix4.xml"){d,i,w,e-> !w.contains("Circumfix Path Error:") && !w.contains("Circumfix Order Error:")}
testFileErrors("tests/data/circumfix5.xml"){d,i,w,e-> w.contains("Circumfix Order Error: When using the circumfix flag with COMPLEXPREFIXES enabled (2-prefix mode), the first affix must be a prefix and the second affix must be a suffix.")}
testFileErrors("tests/data/circumfix6.xml"){d,i,w,e-> w.contains("Circumfix Path Error: Affixation paths containing the circumfix flag can only contain 2 affixes.") &&
		w.contains("Affix Crossover Error:")}
testFileErrors("tests/data/circumfix7.xml"){d,i,w,e-> w.contains("Circumfix Path Error: The circumfix flag can only be used in the first and second affixes' continuation rules.") && 
		w.contains("Circumfix Path Error: Affixation paths containing the circumfix flag can only contain 2 affixes. A third affix is always invalid.")}

def testFileErrors(file, closureAssertion)
{
	File xmlFile = new File(file)
	def fileName = xmlFile.getName().replaceAll(/\.[xX][mM][lL]$/, "")
	def hxc = new HunspellXMLConverter(xmlFile, 
		[hunspell:false, tests:false, thesaurus:false,
		license:false, readme:false,
		firefox:false, opera:false, libreOffice:false,
		//hunspellFileName:fileName,
		customPath:"/",
		relaxNG:false, runTests:true])
	hxc.convert()
	
	def data = hxc.parser.data
	def infoLog = hxc.log.infoLog.toString()
	def warningLog = hxc.log.warningLog.toString()
	def errorLog = hxc.log.errorLog.toString()
	
	assert(closureAssertion.call(data, infoLog, warningLog, errorLog))
}