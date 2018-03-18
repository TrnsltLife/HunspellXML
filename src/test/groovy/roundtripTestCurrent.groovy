import dk.dren.hunspell.*

import java.util.List

import org.sil.hunspellxml.*

def dictionaryPath = "src/test/groovy/data/roundtripOutput/"
testRoundtrip(dictionaryPath)

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
	
	def hunspellCharSet = HunspellXMLUtils.extractHunspellCharacterSet("${dictionaryPath}und.dic")
	def javaCharSet = HunspellXMLUtils.javaCharacterSet(hunspellCharSet)
	
	/*
	println("Testing ${dictionaryPath}und.dic with character set ${javaCharSet}...")
	
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
			System.err.println(("*" * 10) + "TestSug" + ("*" * 10))
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
		System.err.println("Test failed")
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