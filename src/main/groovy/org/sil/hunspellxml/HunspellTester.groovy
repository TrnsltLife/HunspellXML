package org.sil.hunspellxml

import dk.dren.hunspell.*

public class HunspellTester
{
	Hunspell hunspell
	Hunspell.Dictionary dict
	String javaCharSet = ""
	
	HunspellTester(String dictionaryFilePath, String javaCharSet, boolean destroy=false)
	{
		this.javaCharSet = javaCharSet
		
		String basePath
		if(dictionaryFilePath.endsWith(".aff")){basePath = dictionaryFilePath.replaceAll(/\.aff$/, "")}
		else if(dictionaryFilePath.endsWith(".dic")){basePath = dictionaryFilePath.replaceAll(/\.dic$/, "")}
		else{basePath = dictionaryFilePath}
		hunspell = Hunspell.getInstance()
		if(destroy)
		{
			hunspell.destroyDictionary(basePath);
		}
		dict = hunspell.getDictionary(basePath);
	}
	
	def misspelled(String word)
	{
		if(dict.misspelled(word))
		{
			return true
		}
		else
		{
			return false
		}
	}
	
	HunspellTesterResult checkWord(String word)
	{
		def result = new HunspellTesterResult(word)
		result.misspelled = dict.misspelled(word)
		if(result.misspelled)
		{
			result.suggest = dict.suggest(word)
		}
		else
		{
			result.stem = dict.stem(word)
			result.morph = dict.analyze(word)
		}
		return result
	}
	
	List<HunspellTesterResult> checkTestFile(String filename)
	{
		//Hunspell's analyze function accepts 1 word per line.
		//If there are 2 words per line, it runs its generate function.
		//HunspellJNA doesn't implement the generate interface so skip a line if it contains spaces.
		boolean bad = filename.endsWith(".wrong")
		def wordList = []
		/*
		new File(filename).newReader(javaCharSet).eachLine{line->
			line.split(/\s+/).toList().each{word->
				wordList << word
			}
		}
		*/
		def lineList = new File(filename).newReader(javaCharSet).getText().split(/\r?\n/).toList().findAll{it.trim()}
		for(line in lineList)
		{
			line = line.trim()
			if(line =~ /[^\s\t]+[\s\t]+[^\s\t]/)
			{
				//skip lines with more than one word in them
				continue
			}
			else
			{
				wordList << line
			}
		}
		def results = []
		for(word in wordList)
		{
			if(bad)
			{
				if(dict.misspelled(word))
				{
					results << checkWord(word)
				}
				else
				{
					results << checkWord(word)
				}
			}
			else //good
			{
				if(dict.misspelled(word))
				{
					results << checkWord(word)
				}
				else
				{
					results << checkWord(word)
				}
			}
		}
		return results
	}
	
	//Check the test files for conformance with the Hunspell dictionary
	public checkTestFiles(HunspellXMLExporter hxe)
	{
		def log = hxe.log
		
		if(hxe?.dicFile)
		{
			if(hxe?.goodFile)
			{
				//test correct spellings file
				log.info("Testing 'correctly' spelled words in " + hxe.goodFile + "...")
				def errorList = checkTestFile(hxe.goodFile)
				if(errorList.find{it.misspelled})
				{
					log.warning("Some words listed in " + (new File(hxe.goodFile)).getName() + " (which should contain only correct spellings) are rejected as misspellings by the current Hunspell dictionary:\r\n" +
						"{\r\n\t" +
						errorList.collect{e-> "${e.word} :: ${e.morph? 'morph:'+e.morph : ''} ${e.stem? 'stem:'+e.stem : ''} ${e.suggest? 'suggest:'+e.suggest : ''}"}.join("\r\n\t") +
						"\r\n}\r\n"
					)
				}
				else
				{
					log.info("Correctly spelled words test completed without errors.")
				}
			}
			if(hxe?.badFile)
			{
				//test misspellings file
				log.info("Testing 'misspelled' words in " + hxe.badFile + "...")
				def errorList = checkTestFile(hxe.badFile)
				if(errorList.find{!it.misspelled})
				{
					log.warning("Some words listed in " + (new File(hxe.badFile)).getName() + " (which should contain only incorrect spellings)  are accepted as correctly spelled by the current Hunspell dictionary:\r\n" +
						"{\r\n\t" +
						errorList.collect{e-> "${e.word} :: ${e.morph? 'morph:'+e.morph : ''} ${e.stem? 'stem:'+e.stem : ''} ${e.suggest? 'suggest:'+e.suggest : ''}"}.join("\r\n\t") +
						"\r\n}\r\n"
					)
				}
				else
				{
					log.info("Misspelled words test completed without errors.")
				}
			}
		}
	}
}