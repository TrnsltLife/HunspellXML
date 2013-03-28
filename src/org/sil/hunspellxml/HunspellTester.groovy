package org.sil.hunspellxml

import dk.dren.hunspell.*

public class HunspellTester
{
	Hunspell hunspell
	Hunspell.Dictionary dict
	
	HunspellTester(String dictionaryFilePath)
	{
		String basePath
		if(dictionaryFilePath.endsWith(".aff")){basePath = dictionaryFilePath.replaceAll(/\.aff$/, "")}
		else if(dictionaryFilePath.endsWith(".dic")){basePath = dictionaryFilePath.replaceAll(/\.dic$/, "")}
		else{basePath = dictionaryFilePath}
		
		hunspell = Hunspell.getInstance()
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
		boolean bad = filename.endsWith("_bad.txt")
		def wordList = []
		new File(filename).eachLine{line->
			line.split(/\s+/).toList().each{word->
				wordList << word
			}
		}
		def results = []
		for(word in wordList)
		{
			if(bad)
			{
				if(dict.misspelled(word)){}
				else
				{
					results << checkWord(word)
				}
			}
			else
			{
				if(dict.misspelled(word))
				{
					results << checkWord(word)
				}
				else{}
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
				log.info("<br>Testing 'correctly' spelled words in " + hxe.goodFile + "...")
				def errorList = checkTestFile(hxe.goodFile)
				if(errorList)
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
				if(errorList)
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