import dk.dren.hunspell.*
import org.sil.hunspellxml.*
import java.nio.charset.Charset
import java.nio.file.*

TEST_INPUT_DIRS = ["src/test/groovy/data/hunspellTests-1-3-2/",
			  "src/test/groovy/data/hunspellTestsPlus/"]

TEST_OUTPUT = "src/test/groovy/data/roundtripOutput/"
currentTest = ""
logLevel = Log.WARNING

def startWith = ""
def startAfter = ""
def doOne = false
def doFailing = false
def failingList = [
	//1.3.2
	"arabic.dic", //This fails when running Hunspell's test too:> ./test.sh arabic
	//"forbiddenword.dic", //Something weird here. Changing word order in .dic file to match original .dic file causes this to pass tests.
	"utf8_nonbmp.dic" //This fails when running Hunspell's test too:> ./test.sh utf8_nonbmp
	//1.6.2
	//"arabic.dic", //Is it detecting a long FLAG type without long being specified?
	//"base_utf.dic",
	//"forbiddenword.dic", //Something weird here. Changing word order in .dic file to match original .dic file causes this to pass tests. Dic file header indicates less words than actually appear.
]

//Some files may need special options to be set in order to process correctly.
def specialSettings = [
	"forbiddenword.dic":[h2x:[groupWordsByData:false], x2h:[sortDictionaryData:false]]
]

def fileList = []
for(testInputDir in TEST_INPUT_DIRS)
{
	hunspellTestDir = new File(testInputDir)
	if(doFailing)
	{
		fileList = failingList.collect{new File(testInputDir + it)}
	}
	else
	{
		hunspellTestDir.eachFileMatch(~/.*\.dic/){it-> fileList << it}
	}
}
fileList.sort()
println(fileList)

for(testFile in fileList)
{
	//if(testFile.getName() =~ /^[0-9]/){continue}
	if(startWith && testFile.getName().compareTo(startWith) < 0) {continue}
	if(startAfter && testFile.getName().compareTo(startAfter) <= 0){continue}
	if(failingList.contains(testFile.getName()) && !doFailing){continue}
	currentTest = testFile
	println("*"*40)
	println(testFile)
	
	deleteFiles()
	
	def options = specialSettings[new File(testFile.toString()).getName()]
	def encoding = getTestEncoding(new File(testFile.toString().replaceAll(/(\.dic|\.aff)/, ".test")))
	//println("Encoding: ${encoding}")
	hunspellToXML(testFile.toString(), encoding, options?.h2x ?: [:])
	xmlToHunspell("${TEST_OUTPUT}/und.xml", options?.x2h ?: [:])
	copyExtraFiles(testFile)
	testRoundtrip("${TEST_OUTPUT}")
	
	if(doOne){break}
	//Thread.currentThread().sleep(1000)
}

def deleteFiles()
{
	['und.aff','und.dic','und.good','und.morph','und.sug','und.wrong','und.xml'].each{file->
		try
		{
			Files.deleteIfExists(Paths.get(TEST_OUTPUT + file))
		}
		catch(Exception e) {System.err.println("Couldn't delete file.\r\n" + e.toString())}
	}
}

def copyExtraFiles(dicFile)
{
	def sugFile = dicFile.toString().replaceAll(/\.dic/, ".sug")
	def morphFile = dicFile.toString().replaceAll(/\.dic/, ".morph")
	if(Files.exists(Paths.get(sugFile)))
	{
		Files.copy(Paths.get(sugFile), Paths.get(TEST_OUTPUT + "und.sug"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
	}
	if(Files.exists(Paths.get(morphFile)))
	{
		Files.copy(Paths.get(morphFile), Paths.get(TEST_OUTPUT + "und.morph"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
	}
}

def getTestEncoding(file)
{
	for(line in file.text.split(/\r?\n/).toList())
	{
		//$DIR/test.sh $NAME -i ISO8859-1
		if(line =~ /^.DIR\/test.sh .NAME -i /)
		{
			encoding = line.replaceAll(/^.DIR\/test.sh .NAME -i /, "")
			encoding = encoding.replaceAll(/ *([-_a-zA-Z0-9]+).*/, "\$1")
			return encoding?.toUpperCase() ?: ""
		}
	}
	return "ISO8859-1"
}

def hunspellToXML(file, encoding, specialOptions)
{
	def options = [
			outputFileName:"${TEST_OUTPUT}/und.xml",
			defaultCharSet:encoding,
			suppressAutoComments:true, suppressAutoBlankLines:true,
			logLevel:logLevel,
			preferWallOfText:false,
			groupWordsByData:true]
	options.putAll(specialOptions)

	def hc = new HunspellConverter(file, options)
	hc.convert()
	
	def infoLog = hc.log.infoLog.toString()
	def warningLog = hc.log.warningLog.toString()
	def errorLog = hc.log.errorLog.toString()
}


def xmlToHunspell(file, specialOptions)
{
	File xmlFile = new File(file)
	def fileName = xmlFile.getName().replaceAll(/\.[xX][mM][lL]$/, "")
	
	def options = [hunspell:true, tests:true, thesaurus:false,
		license:false, readme:false,
		firefox:false, opera:false, libreOffice:false,
		hunspellFileName:"und",
		customPath:"${TEST_OUTPUT}",
		relaxNG:true, runTests:false,
		suppressAutoComments:true, suppressAutoBlankLines:true,
		sortDictionaryData:true,
		logLevel:logLevel]
	options.putAll(specialOptions)
	
	def hxc = new HunspellXMLConverter(xmlFile, options)
	hxc.convert()
	
	def data = hxc.parser?.data
	def infoLog = hxc.log.infoLog.toString()
	def warningLog = hxc.log.warningLog.toString()
	def errorLog = hxc.log.errorLog.toString()
	
	/*
	if(infoLog.indexOf("Correctly spelled words test completed without errors.") < 0)
	{
		System.err.println("Failed Test: Correctly spelled words test failed.")
		System.exit(1)
	}
	if(infoLog.indexOf("Misspelled words test completed without errors.") < 0)
	{
		System.err.println("Failed Test: Misspelled words test failed.")
		System.exit(2)
	}
	*/
}

def testRoundtrip(dictionaryPath)
{
	if( !new File(dictionaryPath + "und.dic").exists() ||
		!new File(dictionaryPath + "und.aff").exists() ||
		!new File(dictionaryPath + "und.xml").exists() ||
		!new File(dictionaryPath + "und.good").exists() ||
		!new File(dictionaryPath + "und.wrong").exists())
	{
		System.err.println("Error creating all files.")
		System.exit(2)
	}
	
	def hunspellCharSet = HunspellXMLUtils.extractHunspellCharacterSet("${dictionaryPath}und.aff")
	def javaCharSet = HunspellXMLUtils.javaCharacterSet(hunspellCharSet)
	
	//println("Testing ${dictionaryPath}und.dic with character set ${javaCharSet}...")
	
	/*
	println("************ und.aff ************")
	println(new File(dictionaryPath + "und.aff").getText(javaCharSet))
	println("************ und.dic ************")
	println(new File(dictionaryPath + "und.dic").getText(javaCharSet))
	println("************ und.good ***********")
	println(new File(dictionaryPath + "und.good").getText(javaCharSet))
	println("************ und.bad ************")
	println(new File(dictionaryPath + "und.dic").getText(javaCharSet))
	*/
	
	//Destroy old dictionary
	def hunspell = Hunspell.getInstance()
	hunspell.destroyDictionary(dictionaryPath + "und");
	
	def tester = new HunspellTester(dictionaryPath + "und.dic", javaCharSet)
	
	//Test .good file
	def goodPassed = true
	List goodResults = []
	if(new File(dictionaryPath + "und.good").exists())
	{
		goodResults = tester.checkTestFile(dictionaryPath + "und.good")
		if(!goodResults.find{it.misspelled})
		{
			//System.out.println(".good tests passed")
		}
		else {System.err.println(".good tests failed\r\nThe following words test as incorrectly spelled:")
			goodPassed = false
			for(result in goodResults)
			{
				if(result.misspelled) {System.err.println(result.word)}
			}
			System.err.flush()
		}
	}
	
	//Test .wrong file
	def badPassed = true
	List badResults = []
	if(new File(dictionaryPath + "und.wrong").exists())
	{
		badResults = tester.checkTestFile(dictionaryPath + "und.wrong")
		if(!badResults.find{!it.misspelled})
		{
			//System.out.println(".wrong tests passed")
		}
		else 
		{
			System.err.println(".wrong tests failed.\r\nThe following words test as correctly spelled:")
			badPassed = false
			for(result in badResults)
			{
				if(!result.misspelled) {System.err.println(result.word)}
			}
			System.err.flush()
		}
	}
	
	//Test .sug file
	def sugPassed = true
	List sugResults = []
	if(new File(dictionaryPath + "und.sug").exists())
	{
		for(result in badResults)
		{
			sugResults << result.suggest
		}
		sugResults = sugResults.findAll{it}.collect{it.join(", ")}

		def fileResults = new File(dictionaryPath + "und.sug").getText(javaCharSet).trim().readLines()
		
		sugPassed = (fileResults.join("\r\n") == sugResults.join("\r\n"))
		
		if(sugPassed)
		{
			//System.out.println(".sug tests passed")
		}
		else
		{
			System.err.println(".sug tests failed.\r\nCompare the output below to see what went wrong:")
			System.err.println(("*" * 10) + "gen sug" + ("*" * 10))
			System.err.println(sugResults.join("\r\n"))
			System.err.println(("*" * 10) + "und.sug" + ("*" * 10))
			System.err.println(fileResults.join("\r\n"))
			System.err.println("*" * 27)
		}
	}
	
	//Test .morph file
	def morphPassed = true
	List morphResults = []
	if(new File(dictionaryPath + "und.morph").exists())
	{
		for(result in goodResults)
		{
			def record = ("> ${result.word}").trim() + "\n"
			if(result.morph) {record += result.morph.sort().collect{("analyze(${result.word}) = ${it}").trim()}.join("\n")}
			if(result.stem) {record += "\n" + ("stem(${result.word}) = ${result.stem.join(" ")}").trim()}
			morphResults << record
		}

		def fileResults = sortMorphFile(dictionaryPath, javaCharSet)
		
		morphPassed = (fileResults.join("\n") == morphResults.join("\n"))
		
		if(morphPassed)
		{
			//System.out.println(".morph tests passed")
		}
		else
		{
			System.err.println(".morph tests failed.\r\nCompare the output below to see what went wrong:")
			System.err.println(("*" * 10) + "TestMorph" + ("*" * 10))
			System.err.println(morphResults.join("\r\n"))
			System.err.println(("*" * 10) + "und.morph" + ("*" * 10))
			System.err.println(fileResults.join("\r\n"))
			System.err.println("*" * 29)
		}
	}
	
	if(!(goodPassed && badPassed && sugPassed && morphPassed))
	{
		System.err.println("Test failed for " + currentTest)
		System.exit(1)
	}
	else
	{
		System.out.println("OK")
	}
}


List<String> sortMorphFile(dictionaryPath, javaCharSet)
{
	def morphResults = new File(dictionaryPath + "und.morph").getText(javaCharSet).trim().readLines()
	//Remove lines that start with "generate(" and convert tabs to spaces
	//Currently HunspellJNA can't produce the generate(word1, word2) information
	morphResults = morphResults.findAll{!it.startsWith("generate(")}.collect{it.replaceAll(/\t/, " ")}
	
	def sorted = []
	for(line in morphResults)
	{
		line = line.trim()
		if(line.startsWith("> "))
		{
			def sublist = [line]
			sorted << sublist	
		}
		else if(line.startsWith("analyze("))
		{
			sorted[-1] << line
		}
		else if(line.startsWith("stem("))
		{
			sorted[-1] << line
		}
	}
	for(List sublist in sorted)
	{
		sublist.sort()
	}
	sorted = sorted.collect{it.join("\n")}
	return sorted
}