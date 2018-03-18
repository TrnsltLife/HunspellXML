import dk.dren.hunspell.*
import org.sil.hunspellxml.*
import java.nio.charset.Charset
import java.nio.file.*

TEST_INPUT = "src/test/groovy/data/hunspellTests/"
TEST_OUTPUT = "src/test/groovy/data/roundtripOutput/"
currentTest = ""
logLevel = Log.WARNING

def startWith = ""
def startAfter = ""
def doOne = false
def doFailing = false
def failingList = [
	//"alias.dic", //Need to extract flags from AF when converting to HunspellXML
	//"alias2.dic", //Need to extract flags from AF and AM when converting to HunspellXML
	//"alias3.dic", //Need to extract flags from AM when converting to HunspellXML
	"arabic.dic", //This fails when running Hunspell's test too:> ./test.sh arabic
	"forbiddenword.dic", //Something weird here. Changing word order in .dic file to match original .dic file causes this to pass tests.
	"utf8_nonbmp.dic" //This fails when running Hunspell's test too:> ./test.sh utf8_nonbmp
]
hunspellTestDir = new File(TEST_INPUT)
def fileList = []
if(doFailing)
{
	fileList = failingList.collect{new File(TEST_INPUT + it)}
}
else
{
	hunspellTestDir.eachFileMatch(~/.*\.dic/){it-> fileList << it}
}
fileList.sort()
println(fileList)

for(testFile in fileList)
{
	if(testFile.getName() =~ /^[0-9]/){continue}
	if(startWith && testFile.getName().compareTo(startWith) < 0) {continue}
	if(startAfter && testFile.getName().compareTo(startAfter) <= 0){continue}
	if(failingList.contains(testFile.getName()) && !doFailing){continue}
	currentTest = testFile
	println("*"*40)
	println(testFile)
	
	deleteFiles()
	
	
	def encoding = getTestEncoding(new File(testFile.toString().replaceAll(/(\.dic|\.aff)/, ".test")))
	//println("Encoding: ${encoding}")
	hunspellToXML(testFile.toString(), encoding)
	xmlToHunspell("${TEST_OUTPUT}/und.xml")
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

def hunspellToXML(file, encoding)
{
	def hc = new HunspellConverter(file, [
			outputFileName:"${TEST_OUTPUT}/und.xml",
			defaultCharSet:encoding,
			suppressAutoComments:true, suppressAutoBlankLines:true,
			logLevel:logLevel,
			preferWallOfText:false])
	//Possible options:
	//charSet
	hc.convert()
	
	def infoLog = hc.log.infoLog.toString()
	def warningLog = hc.log.warningLog.toString()
	def errorLog = hc.log.errorLog.toString()
}


def xmlToHunspell(file)
{
	File xmlFile = new File(file)
	def fileName = xmlFile.getName().replaceAll(/\.[xX][mM][lL]$/, "")
	def hxc = new HunspellXMLConverter(xmlFile,
		[hunspell:true, tests:true, thesaurus:false,
		license:false, readme:false,
		firefox:false, opera:false, libreOffice:false,
		//hunspellFileName:fileName,
		customPath:File.separator,
		relaxNG:true, runTests:false,
		suppressAutoComments:true, suppressAutoBlankLines:true,
		logLevel:logLevel])
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