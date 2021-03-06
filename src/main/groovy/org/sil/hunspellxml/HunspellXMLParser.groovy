package org.sil.hunspellxml

import groovy.util.Node;

class HunspellXMLParser
{
	//Parse HunspellXML file and create in-memory dictionary, thesaurus, and test files
	Log log = new Log()
	
	def data = new HunspellXMLData()
	
	HunspellXMLFlagChecker check
	
	Map exportOptions = [:]
	
	public static final EOL = "\r\n"
	static final String FS = File.separator
	
	def nameMap = [
		aliasFlags:"AF",
		aliasMorphemes:"AM",
		affixes:"Affix Rules",
		breakChars:"BREAK",
		characterSet:"SET",
		chars:"BREAK",
		checkCompoundCase:"CHECKCOMPOUNDCASE",
		checkCompoundDuplicates:"CHECKCOMPOUNDDUP",
		checkCompoundReplacements:"CHECKCOMPOUNDREP",
		checkCompoundTriple:"CHECKCOMPOUNDTRIPLE",
		checkSharpS:"CHECKSHARPS",
		circumfix:"CIRCUMFIX",
		complexPrefixes:"COMPLEXPREFIXES",
		compound:"COMPOUNDFLAG",
		compoundBegin:"COMPOUNDBEGIN",
		compoundForbid:"COMPOUNDFORBIDFLAG",
		compoundEnd:"COMPOUNDEND",
		compoundMiddle:"COMPOUNDMIDDLE",
		compoundMin:"COMPOUNDMIN",
		compoundMoreSuffixes:"COMPOUNDMORESUFFIXES",
		compoundPermit:"COMPOUNDPERMITFLAG",
		compoundRoot:"COMPOUNDROOT",
		compoundRules:"COMPOUNDRULE",
		compoundPatterns:"CHECKCOMPOUNDPATTERN",
		compounds:"Compounds",
		compoundSyllable:"COMPOUNDSYLLABLE",
		compoundWordMax:"COMPOUNDWORDMAX",
		//convert:"Input/Output Conversions",
		convertInput:"Input Conversions",
		convertOutput:"Output Conversions",
		flagType:"FLAG",
		forbiddenWord:"FORBIDDENWORD",
		forbidWarn:"FORBIDWARN",
		forceUpperCase:"FORCEUCASE",
		fullStrip:"FULLSTRIP",
		ignore:"IGNORE",
		input:"ICONV",
		keepCase:"KEEPCASE",
		keyboard:"KEY",
		languageCode:"LANG",
		mappings:"MAP",
		map:"MAP",
		maxCompoundSuggestions:"MAXCPDSUGS",
		maxDifference:"MAXDIFF",
		maxNGramSuggestions:"MAXNGRAMSUGS",
		metadata:"metadata",
		needAffix:"NEEDAFFIX",
		noSplitSuggestions:"NOSPLITSUGS",
		noSuggestions:"NOSUGGEST",
		onlyInCompound:"ONLYINCOMPOUND",
		onlyMaxDifference:"ONLYMAXDIFF",
		output:"OCONV",
		phone:"PHONE",
		prefix:"PFX",
		replace:"REP",
		replacements:"REP",
		s:"Synonym",
		settings:"General Settings",
		simplifiedTriple:"SIMPLIFIEDTRIPLE",
		substandard:"SUBSTANDARD",
		suffix:"SFX",
		suggestions:"Suggestions",
		suggestionsWithDots:"SUGSWITHDOTS",
		syllableNum:"SYLLABLENUM",
		tryChars:"TRY",
		w:"Word",
		warn:"WARN",
		wordChars:"WORDCHARS"
	]
	
	def functionMap = [
		"setting0": [
			"checkCompoundCase",
			"checkCompoundDuplicates",
			"checkCompoundReplacements",
			"checkCompoundTriple",
			"checkSharpS",
			"complexPrefixes",
			"compoundMoreSuffixes",
			"forbidWarn",
			"fullStrip",
			"noSplitSuggestions",
			"onlyMaxDifference",
			"simplifiedTriple",
			"suggestionsWithDots",
		],
	
		"setting1": [
			"characterSet",
			"compoundMin",
			"compoundWordMax",
			"flagType",
			"languageCode",
			"maxCompoundSuggestions",
			"maxDifference",
			"maxNGramSuggestions"
		],
	
		"settingCharList": [
			"ignore",
			"wordChars",
		],

		"setting1Flag": [
			"circumfix",
			"compound",
			"compoundBegin",
			"compoundEnd",
			"compoundForbid",
			"compoundMiddle",
			"compoundPermit",
			"compoundRoot",
			"forbiddenWord",
			"forceUpperCase",
			"keepCase",
			"needAffix",
			"noSuggestions",
			"onlyInCompound",
			"substandard",
			"warn",
		],
		
		"sectionComment": [
			"affixes",
			"compounds",
			"metadata",
			"settings",
			"suggestions",
		],
	]
		
	def functionList = ["setting0Map", "setting1Map", "settingCharListMap", "flag1SettingMap", "sectionComment"]
	def ignoreEnvelope = ["affixFile"]
	
	HunspellXMLParser(File basePath, String flagType, Log log, Map exportOptions)
	{
		this.log = log
		data.basePath = basePath
		data.metadata.flagType = flagType
		check = new HunspellXMLFlagChecker(flagType, log)
		this.exportOptions.putAll(exportOptions)
	}
	
	def /*HunspellXMLData*/ parseText(String xmlDoc) //returns HunspellXMLData. But specifying that is breaking Eclipse Groovy compilation for some reason.
	{
		def h
		try
		{
			h = new XmlParser().parseText(xmlDoc)
		}
		catch(Exception e)
		{
			log.error("Error parsing XML file. " + EOL + e.toString() + EOL)
		}
		processChildren(h)
		
		check.mapFlagPaths()
		check.checkRoutes()
		
		return data
	}
	
	List flagsToList(String flags)
	{
		return flags.trim().split(/\s/).toList()
	}
	
	List hunspellFlagsToList(String flags)
	{
		return HunspellXMLUtils.hunspellFlagsToList(log, data.metadata.flagType, flags)
	}
	
	String formatFlagList(String flags)
	{
		return formatFlagList(flags.trim().split(/\s/).toList())
	}
	
	String formatFlagList(List flags)
	{
		//remove any empty flags
		flags = flags.findAll{it}
		
		if(data.metadata.flagType == "short")
		{
			return flags.join("")
		}
		else if(data.metadata.flagType == "long")
		{
			return flags.join("")
		}
		else if(data.metadata.flagType == "UTF-8")
		{
			return flags.join("")
		}
		else if(data.metadata.flagType == "num")
		{
			return flags.join(",")
		}
	}
	
	String printBlankLine()
	{
		if(!exportOptions.suppressAutoBlankLines)
		{
			return EOL
		}
		return ""
	}
	
	String printComment(String comment)
	{
		if(!exportOptions.suppressAutoComments)
		{
			return "# " + comment + EOL
		}
		return ""
	}
	
	String printMetadata(String metadata)
	{
		if(!exportOptions.suppressMetadata)
		{
			return "# " + metadata + EOL
		}
		return ""
	}
	
	Object processChildren(Node node)
	{
		node.children().each{subnode->
			if(!(subnode instanceof String))
			{
				"${subnode.name()}"(subnode.name(), subnode)
			}
		}
	}
	
	Object processChild(Node node)
	{
		if(!(node instanceof String))
		{
			"${node.name()}"(node.name(), node)
		}
	}
	
	Object invokeMethod(String nodeName, Object array)
	{
		def list = array as List
		def node = list[1]
		def hunspellName = nameMap[nodeName]
		
		//Certain sets of nodes can be grouped generically. Look for them.
		//generic function nodes include monads(setting0), 1 text value nodes(setting1), 1 flag value nodes(setting1flag), etc.
		def function = ""
		for(entry in functionMap)
		{
			if(entry.value.contains(nodeName))
			{
				function = entry.key
				break
			}
		}
		if(function)
		{
			if(function == "setting0"){setting0(hunspellName, node)}
			else if(function == "setting1"){setting1(hunspellName, node)}
			else if(function == "settingCharList"){settingCharList(hunspellName, node)}
			else if(function == "setting1Flag"){setting1Flag(hunspellName, node)}
			else if(function == "sectionComment"){sectionComment(hunspellName, node)}
		}
		//Envelope functions have their names in the ignoreEnvelopes list.
		//They don't have any direct output but their children are processed.
		else if(ignoreEnvelope.contains(nodeName))
		{
			processChildren(node)
		}
		//unhandled/unimplemented nodes get default handling here.
		else
		{
			//data.affFile << "[" + nodeName + "|" + hunspellName + "] "
			log.error("Unhandled element: " + nodeName + EOL)
			if(node.attributes())
			{
				//data.affFile << " " + node.attributes()
				log.error("attributes: " + node.attributes() + EOL)
			}
			
			if(node.text())
			{
				//data.affFile << " " + node.text() + EOL
				log.error("text: " + node.text() + EOL)
			}
			else
			{
				//data.affFile << EOL
				log.error("children: " + EOL)
				processChildren(node)
			}
		}
	}
	
	String commAttr(node)
	{
		if(node?.attributes()?.comment)
		{
			return " # " + node.attributes().comment
		}
		return ""
	}
	
	
	Object sectionComment(String hunspellName, Node node)
	{
		data.affFile << printComment("#"*40)
		data.affFile << printComment(hunspellName)
		data.affFile << printComment("#"*40)
		processChildren(node)
	}
	
	Object setting0(String hunspellName, Node node)
	{
		data.affFile << hunspellName + commAttr(node) + EOL
		if(hunspellName == "COMPLEXPREFIXES"){check.complexPrefixes = true}
	}
	
	Object setting1(String hunspellName, Node node)
	{
		data.affFile << hunspellName + " " + node.text() + commAttr(node) + EOL
		
		if(node.name() == "languageCode")
		{
			data.metadata["languageCode"] = node.text()
		}
		else if(node.name() == "characterSet")
		{
			data.metadata["characterSet"] = node.text()
			data.metadata["javaEncoding"] = HunspellXMLUtils.javaCharacterSet(data.metadata.characterSet)
		}
	}
	
	Object settingCharList(String hunspellName, Node node)
	{
		data.affFile << hunspellName + " " + node.text().replaceAll(/\s/, "") + commAttr(node) + EOL
	}
	
	Object setting1Flag(String hunspellName, Node node)
	{
		data.affFile << hunspellName + " " + node.attributes().flag + commAttr(node) + EOL
		check.addSpecialFlag(node.attributes().flag, hunspellName)
	}
	
	Object currentOutputFile()
	{
		if(data.currentSection == "affixFile"){return data.affFile}
		else if(data.currentSection == "dictionaryFile"){return data.dicFile}
		else if(data.currentSection == "thesaurusFile"){return data.datFile}
		return data.affFile //default
	}
	
	Object aliasFlags(String nodeName, Node node)
	{
		data.affFile << "# " + nameMap[nodeName] + " (aliasFlags) is not yet implemented in the HunspellXML coverter." + EOL
	}

	Object aliasMorphemes(String nodeName, Node node)
	{
		data.affFile << "# " + nameMap[nodeName] + " (aliasMorphemes) is not yet implemented in the HunspellXML coverter." + EOL
	}


	Object br(String hunspellName, Node node)
	{
		if(!exportOptions.suppressMyBlankLines)
		{
			def commentFile = currentOutputFile()
			commentFile << EOL
		}
	}
	
	/*
	Object breakChars(String nodeName, Node node)
	{
		data.affFile << printBlankLine()
		data.affFile << printComment("Word Break Characters")
	
		def text = node.text().trim()
		def list = node.text().split(/\s+/).toList()
		def breakCount = list.size()
		if(node.attributes().off)
		{
			breakCount = 0
			list = null
		}
	
		data.affFile << nameMap[nodeName] + " " + breakCount + EOL
		list.each{item->
			data.affFile << nameMap[nodeName] + " " + item + EOL
		}
		data.affFile << printBlankLine()
	}
	*/
	
	Object breakChars(String nodeName, Node node)
	{
		data.affFile << printBlankLine()
		data.affFile << printComment("Word Break Characters")
	
		def breakCount = 0
		if(node.attributes().off)
		{
			breakCount = 0
		}
		else
		{
			for(child in node.children())
			{
				if(child.name() == "chars")
				{
					breakCount ++
				}
			}
		}
	
		data.affFile << nameMap[nodeName] + " " + breakCount + commAttr(node) + EOL
		processChildren(node)
		data.affFile << printBlankLine()
	}
	
	Object chars(String nodeName, Node node)
	{
		data.affFile << nameMap[nodeName] + " " + node.text() + commAttr(node) + EOL
	}

	Object comment(String hunspellName, Node node)
	{
		def lines = node.text().split(/\r?\n/).toList()
		if(!exportOptions.suppressMyComments)
		{
			def commentFile = currentOutputFile()
			for(line in lines)
			{
				commentFile << "# " + line + commAttr(node) + EOL
			}
		}
	}
	
	Object compoundPatterns(String nodeName, Node node)
	{
		def patCount = 0
		for(child in node.children())
		{
			if(child.name() == "pattern")
			{
				patCount ++
			}
		}
		
		data.affFile << printComment("Compound Patterns")
		data.affFile << nameMap[nodeName] + " " + patCount + commAttr(node) + EOL
		
		for(child in node.children())
		{
			def startChars = child.attributes().startChars ?: ""
			def startFlags = child.attributes().startFlags ?: ""
			def endChars = child.attributes().endChars ?: ""
			def endFlags = child.attributes().endFlags ?: ""
			def replacement = child.attributes().replacement ?: ""
			if(child.name() == "pattern")
			{
				data.affFile << nameMap[nodeName] + " "
				data.affFile << endChars + (endFlags?"/" + endFlags:"") + " " + startChars + (startFlags?"/" + startFlags:"")
				if(replacement){data.affFile << " " + replacement}
				data.affFile << commAttr(child) + EOL
			}
			else if(child.name() == "comment")
			{
				processChild(child)
			}
		}
		data.affFile << printBlankLine()
	}

	Object compoundRules(String nodeName, Node node)
	{
		def ruleCount = 0
		for(child in node.children())
		{
			if(child.name() == "rule")
			{
				ruleCount ++
			}
		}
		
		data.affFile << printComment("Compound Rules")
		data.affFile << nameMap[nodeName] + " " + ruleCount + commAttr(node) + EOL
		
		for(child in node.children())
		{
			if(child.name() == "rule")
			{
				data.affFile << nameMap[nodeName] + " " + child.text() + commAttr(child) + EOL
			}
			else if(child.name() == "comment")
			{
				processChild(child)
			}
		}
		data.affFile << printBlankLine()
	}
	
	Object convertInput(String nodeName, Node node)
	{
		return convert(nodeName, node, "input", "Input Conversions")
	}
	
	Object convertOutput(String nodeName, Node node)
	{
		return convert(nodeName, node, "output", "Output Conversions")
	}
	
	Object convert(String nodeName, Node node, String childName, String sectionComment)
	{
		def count = 0
		def printed = false
		
		for(child in node.children())
		{
			if(child.name() == childName)
			{
				count++
			}
		}
		
		data.affFile << printComment(sectionComment)
		
		for(child in node.children())
		{
			if(child.name() == childName)
			{
				if(printed == false)
				{
					printed = true
					data.affFile << nameMap[child.name()] + " " + count + commAttr(node) + EOL
				}
				data.affFile << nameMap[child.name()] + " " + child.attributes().from + " " + child.attributes().to + commAttr(child) + EOL
			}
			else if(child.name() == "comment")
			{
				processChild(child)
			}
		}
		data.affFile << printBlankLine()
	}

	Object compoundSyllable(String nodeName, Node node)
	{
		def max = node.attributes().max ?: 0
		def vowels = node.attributes().vowels ?: ""
		data.affFile << nameMap[nodeName] + " " + max + " " + vowels + commAttr(node) + EOL
	}
	
	Object dictionaryFile(String nodeName, Node node)
	{
		data.currentSection = "dictionaryFile"
		if(node.attributes().containsKey("wordCount"))
		{
			data.dicOrigCount = Integer.parseInt(node.attributes().wordCount)
		}
		processChildren(node)
		
		//Create dicFile
		//Number of words
		//Based on the count in the wordCount attribute which reflects the original Hunspell .dic file's count on its first line
		if(exportOptions.useOriginalWordCount && data.dicOrigCount > -1)
		{
			data.dicFile << data.dicOrigCount + commAttr(node) + EOL
		}
		//Or the count of the actual number of lines with words found in the file
		else
		{
			data.dicFile << data.dicCount + commAttr(node) + EOL
		}
		//Sorted list of words
		if(exportOptions.sortDictionaryData)
		{
			data.dicList = data.dicList.sort()
		}
		data.dicList.each{line->
			data.dicFile << line
		}
	}
	
	//Synonym <entry> contains a list of <synonyms>
	Object entry(String nodeName, Node node)
	{
		def synCount = 0
		for(child in node.children())
		{
			if(child.name() == "synonyms")
			{
				synCount ++
			}
		}
		
		def lines = []
		lines << node.attributes().word + "|" + synCount
		
		for(child in node.children())
		{
			if(child.name() == "synonyms")
			{
				//Look for <s>synonym</s> in the grandchildren
				def grandchildren = child.children()
				if(grandchildren.find{it.name() == "s"})
				{
					def synonyms = []
					for(grandchild in grandchildren)
					{
						if(grandchild.name() == "s")
						{
							synonyms << grandchild.text().trim()
						}
					}
					lines << child.attributes().info + "|" + synonyms.join("|")
				}
				//Otherwise look for a list of synonym words as text syn1|syn2|syn3|syn4|etc.
				else if(child.text())
				{
					lines << child.attributes().info + "|" + child.text().trim()
				}
			}
		}
		
		addThesaurusData(lines)
	}
	
	//Synonym <entries> contains a wall-of-text in MyThes synonym format
	Object entries(String nodeName, Node node)
	{
		def lines = node.text().split(/\r?\n/).toList()
		addThesaurusData(lines)
	}
	
	Object flagType(String nodeName, Node node)
	{
		//flagType doesn't output if the value is "short"
		if(node.text().trim() == "short")
		{
			//Don't output any FLAG directive
		}
		else
		{
			data.affFile << nameMap[nodeName] + " " + node.text() + commAttr(node) + EOL
		}
		
		data.metadata["flagType"] = node.text()
	}

	Object keyboard(String nodeName, Node node)
	{
		def layout = node.attributes().layout
		def text = node.text()
		if(layout)
		{
			if(layout == "QWERTY")
			{
				text = "qwertyuiop|asdfghjkl|zxcvbnm|qaz|wsx|edc|rfv|tgb|yhn|ujm|ik|ol|pl|okm|ijn|uhb|ygv|tfc|rdx|esz|wa"
			}
			else if(layout == "Dvorak")
			{
				text = "pyfgcrl|aeouidhtns|qjkxbmwvz"
			}
			else if(layout == "AZERTY")
			{
				text = "azertyuiop|qsdfghjklm�|wxcvbn|a�z|y�u|i�o|o�p|aqz|zse|edr|rft|tgy|yhu|uji|iko|olpm|qws|sxd|dcf|fvg|gbh|hnj"
			}
		}
		data.affFile << nameMap[nodeName] + " " + text + commAttr(node) + EOL
	}
	
	Object include(String nodeName, Node node)
	{
		if(data.currentSection == "dictionaryFile")
		{
			def text = ""
			def filepath = node.attributes().file
			File file
			def errors = ""
			try
			{
				file = new File(data.basePath.getCanonicalPath() + FS + filepath)
				text = file.newReader(data.metadata.characterSet).text
			}
			catch(Exception e)
			{
				errors += "Couldn't open file " + data.basePath.getCanonicalPath() + FS + filepath + "\r\n"
				errors += e.toString() + "\r\n"
				try
				{
					file = new File(filepath)
					text = file.newReader(data.metadata.characterSet).text
				}
				catch(Exception e2)
				{
					errors += "Couldn't open file " + filepath
					errors += e2.toString() + "\r\n"
					log.error(errors)
				}
			}

			def lines = text.split(/\r?\n/).toList()
			def flags = formatFlagList(node.attributes().flags ?: "")
			def morph = (node.attributes().morph ?: "").trim()
			addDictionaryWords(lines, flags, morph)
		}
		if(data.currentSection == "thesaurusFile")
		{
			def text = ""
			def filepath = node.attributes().file
			File file
			def errors = ""
			try
			{
				file = new File(data.basePath.getCanonicalPath() + FS + filepath)
				text = file.newReader(data.metadata.characterSet).text
			}
			catch(Exception e)
			{
				errors += "Couldn't open file " + data.basePath.getCanonicalPath() + FS + filepath + "\r\n"
				errors += e.toString() + "\r\n"
				try
				{
					file = new File(filepath)
					text = file.newReader(data.metadata.characterSet).text
				}
				catch(Exception e2)
				{
					errors += "Couldn't open file " + filepath
					errors += e2.toString() + "\r\n"
					log.error(errors)
				}
			}

			def lines = text.split(/\r?\n/).toList()
			addThesaurusData(lines)
		}
	}

	Object mappings(String nodeName, Node node)
	{
		def mapCount = 0
		for(child in node.children())
		{
			if(child.name() == "map")
			{
				mapCount ++
			}
		}
		
		data.affFile << printComment("Mappings")
		data.affFile << nameMap[nodeName] + " " + mapCount + commAttr(node) + EOL
		
		processChildren(node)
		data.affFile << printBlankLine()
	}

	Object map(String nodeName, Node node)
	{
		def list = node.text().split(/\s+/).toList()
		data.affFile << nameMap[nodeName] + " "
		list.each{item->
			if(item.size() > 1){data.affFile << "(" + item + ")"}
			else{data.affFile << item}
		}
		data.affFile << commAttr(node) + EOL
	}

	Object metadata(String nodeName, Node node)
	{
		node.children().each{child->
			def name = child.name()
			if(name == "contributors")
			{
				data.metadata.contributors = []
				child.children().each{cname->
					data.metadata.contributors << cname.text()
				}
			}
			else if(name == "customAttributes")
			{
				data.metadata.customAttributes = [:]
				child.children().each{attr->
					data.metadata.customAttributes[attr.atributes().name] = attr.text()
				}
			}
			else
			{
				def text = child.text()
				def attributes = child.attributes()
				if(attributes && text)
				{
					attributes.text = text
					data.metadata[name] = attributes
				}
				else if(attributes)
				{
					data.metadata[name] = attributes
				}
				else if(text)
				{
					data.metadata[name] = text
				}
			}
			data.affFile << printMetadata("$name ${data.metadata[name]}")
		}
		data.affFile << printBlankLine()
	}
	
	Object multiply(String parentName, Node parentNode, Node node, HunspellXMLFlagPath flagPathParent)
	{
		def returnRules = []
		
		//log.info("multiply...")
		def groups = []
		node.children().each{group->
			def rules = []
			group.children().each{child->
				rules << [
						add:child.attributes().add ?: "0",
						remove:child.attributes().remove ?: "0",
						where:child.attributes().where ?: ".",
						morph:child.attributes().morph ?: "",
						combineFlags:child.attributes().combineFlags ?: "",
						comment:(group.attributes().comment ?: "") + (child.attributes().comment ?: ""),
					]
					if(rules.add == "0"){rules.add = ""}
					if(rules.remove == "0"){rules.remove = ""}
			}
			groups << rules
		}
		
		def sizes = []
		groups.each{group-> sizes << group.size()}
		def groupCount = groups.size()
		//log.info(sizes)
		//log.info(groupCount)

		def indexes = sizes.collect{0}
		while(indexes[0] < sizes[0])
		{
			//Do multiplication
			def multiplyList = []
			for(int i=0; i<indexes.size(); i++)
			{
				multiplyList << groups[i][indexes[i]]
			}
			if(parentName == "prefix")
			{
				returnRules << multiplyPrefixList(multiplyList)
			}
			else
			{
				returnRules << multiplySuffixList(multiplyList)
			}
			
			//Increase counters
			def flip = true
			for(int i=1; i<=indexes.size(); i++)
			{
				if(flip)
				{
					indexes[-1*i]++
					if(indexes[-1*groupCount] >= sizes[-1*groupCount] )
					{
						break
					}
					if(indexes[-1*i] >= sizes[-1*i])
					{
						indexes[-1*i] = 0
						flip = true
					}
					else
					{
						flip = false
					}
				}
			}
		}

		def formattedRules = []
		if(returnRules)
		{
			def flag = parentNode.attributes().flag
			def cross = parentNode.attributes().cross
			cross = (cross == "true" ? "Y" : "N")
			//Note: this affix flag was added to the affixFlags list in the prefixSuffix() method

			returnRules.each{rule->
				def formattedRule = ""
				formattedRule += nameMap[parentName] + " ${flag} ${rule.remove} ${rule.add}"
				if(rule.combineFlags)
				{
					check.addCombineFlags(flagPathParent, flag, parentName, rule.combineFlags)
					rule.combineFlags = formatFlagList(rule.combineFlags)
					formattedRule += "/" + rule.combineFlags
				}
				formattedRule += " " + rule.where
				if(rule.morph)
				{
					formattedRule += "\t" + rule.morph
				}
				if(rule.comment)
				{
					formattedRule += " # " + rule.comment
				}
				
				if(!formattedRules.contains(formattedRule))
				{
					formattedRules << formattedRule
				}
			}
			
			if(formattedRules)
			{
				def comment = (parentNode?.attributes()?.comment ?: "") + (node?.attributes()?.comment ?: "")
				if(comment){comment = ' # ' + comment}
				data.affFile << nameMap[parentName] + " " + flag + " " + cross + " " + formattedRules.size() + comment + EOL
				
				formattedRules.each{data.affFile << it + EOL}
			}
		}
	}
	
	Map multiplyPrefixList(multiplyList)
	{
		def flagList = []
		def morphList = []
		def commentList = []
		def form = ""
		def startAt = multiplyList.size() -1
		
		//By this point, add="0" and remove="0" have been changed to add="" and remove = ""
		for(int i=startAt; i >= 0; i--)
		{
			//Only attach the following prefixes if this one's
			//'where' rule matches the start of the current
			//prefix stack, or if this one is the first prefix
			def where = multiplyList[i].where
			if(i==startAt || form =~ /^${where}/)
			{
				def remove = multiplyList[i].remove
				if(remove)
				{
					//remove characters from the front of the prefix stack
					form -= remove
				}
				def add = multiplyList[i].add
				form = add + form
				
				def flags = multiplyList[i].combineFlags.split(/ /).toList()
				flags.each{flag->
					if(flag && !flagList.contains(flag))
					{
						flagList << flag
					}
				}
				
				def morph = multiplyList[i].morph
				if(morph && !morphList.contains(morph))
				{
					morphList << morph
				}
				
				def comment = multiplyList[i].comment
				if(comment)
				{
					commentList.add(0, comment)
				}
			}
			else
			{
				break
			}
		}
		return [add:form?:"0",
				remove:multiplyList[startAt].remove?:"0",
				where:multiplyList[startAt].where?:".",
				combineFlags:flagList.join(" "),
				morph:morphList.join(" "),
				comment:commentList.join("")
				]
	}
	
	Map multiplySuffixList(multiplyList)
	{
		def flagList = []
		def morphList = []
		def commentList = []
		def form = ""
		def endAt = multiplyList.size() -1
		
		//By this point, add="0" and remove="0" have been changed to add="" and remove = ""
		for(int i=0; i <= endAt; i++)
		{
			//Only attach the following suffixes if this one's
			//'where' rule matches the end of the current
			//suffix stack, or if this one is the first suffix
			def where = multiplyList[i].where
			if(i==0 || form =~ /${where}$/)
			{
				def remove = multiplyList[i].remove
				if(remove)
				{
					//remove characters from the end of the prefix stack
					form = (form.reverse() - remove.reverse()).reverse()
				}
				def add = multiplyList[i].add
				form += add
				
				def flags = multiplyList[i].combineFlags.split(/ /).toList()
				flags.each{flag->
					if(flag && !flagList.contains(flag))
					{
						flagList << flag
					}
				}
				
				def morph = multiplyList[i].morph
				if(morph && !morphList.contains(morph))
				{
					morphList << morph
				}
				
				def comment = multiplyList[i].comment
				if(comment)
				{
					commentList << comment
				}
			}
			else
			{
				break
			}
		}
		return [add:form?:"0",
				remove:multiplyList[0].remove?:"0",
				where:multiplyList[0].where?:".",
				combineFlags:flagList.join(" "),
				morph:morphList.join(" "),
				comment:commentList.join("")
				]
	}
	
	
	Object prefix(String nodeName, Node node){prefixSuffix(nodeName, node)}
	
	Object suffix(String nodeName, Node node){prefixSuffix(nodeName, node)}

	Object prefixSuffix(String nodeName, Node node)
	{
		def ruleCount = 0
		for(child in node.children())
		{
			if(child.name() == "rule")
			{
				ruleCount ++
			}
		}
		
		def flag = node.attributes().flag
		def cross = node.attributes().cross
		def flagPathParent = check.addAffixFlag(flag, nodeName, cross)
		cross = (cross == "true" ? "Y" : "N")
		
		if(ruleCount)
		{
			data.affFile << nameMap[nodeName] + " " + flag + " " + cross + " " + ruleCount + commAttr(node) + EOL
		}
		
		for(child in node.children())
		{
			if(child.name() == "comment" || child.name() == "tests")
			{
				processChild(child)
			}
			else if(child.name() == "rule")
			{
				def add = child.attributes().add ?: "0"
				def remove = child.attributes().remove ?: "0"
				def where = child.attributes().where ?: "."
				def morph = child.attributes().morph ?: ""
				def combineFlags = child.attributes().combineFlags ?: ""
				data.affFile << nameMap[nodeName] + " ${flag} ${remove} ${add}"
				if(combineFlags)
				{
					check.addCombineFlags(flagPathParent, flag, nodeName, combineFlags)
					
					combineFlags = formatFlagList(combineFlags)
					data.affFile << "/" + combineFlags
				}
				data.affFile << " " + where
				if(morph)
				{
					data.affFile << "\t" + morph
				}
				data.affFile << commAttr(child) + EOL
			}
			else if(child.name() == "multiply")
			{
				multiply(nodeName, node, child, flagPathParent)
			}
		}
		data.affFile << printBlankLine()
	}

	Object phone(String nodeName, Node node)
	{
		data.affFile << printComment("Phone Tables")
		data.affFile << printComment("Check this carefully. HunspellXML converter doesn't validate this code.")
		
		def ruleCount = 0
		for(child in node.children())
		{
			if(child.name() == "rule")
			{
				ruleCount ++
			}
		}
		
		data.affFile << nameMap[nodeName] + " " + ruleCount + commAttr(node) + EOL
		
		for(child in node.children())
		{
			if(child.name() == "rule")
			{
				data.affFile << nameMap[nodeName] + " " + child.text() + commAttr(child) + EOL
			}
			else if(child.name() == "comment")
			{
				processChild(child)
			}
		}
		data.affFile << printBlankLine()
	}
		
	Object replacements(String nodeName, Node node)
	{
		def repCount = 0
		for(child in node.children())
		{
			if(child.name() == "replace")
			{
				repCount ++
				if(child.attributes().reverse == "true")
				{
					repCount++
				}
			}
		}
		
		data.affFile << printComment("Replacements")
		data.affFile << nameMap[nodeName] + " " + repCount + commAttr(node) + EOL
		
		processChildren(node)
		data.affFile << printBlankLine()
	}
	
	Object replace(String nodeName, Node node)
	{
		data.affFile << nameMap[nodeName] + " " + node.attributes().from + " " + node.attributes().to + commAttr(node) + EOL
		if(node.attributes().reverse == "true")
		{
			data.affFile << nameMap[nodeName] + " " + node.attributes().to + " " + node.attributes().from + commAttr(node) + EOL
		}
	}
	
	Object suppress(String nodeName, Node node)
	{
		if(node.attributes().autoBlankLines == "true")
		{
			exportOptions.suppressAutoBlankLines = true
		}
		if(node.attributes().autoComments == "true")
		{
			exportOptions.suppressAutoComments = true
		}
		if(node.attributes().metadata == "true")
		{
			exportOptions.suppressMetadata = true
		}
		if(node.attributes().myBlankLines == "true")
		{
			exportOptions.suppressMyBlankLines = true
		}
		if(node.attributes().myComments == "true")
		{
			exportOptions.suppressMyComments = true
		}
	}
	
	Object syllableNum(String nodeName, Node node)
	{
		def flags = node.attributes().flags
		data.affFile << nameMap[nodeName] + " " + formatFlagList(flags) + commAttr(node) + EOL
	}
	
	Object tests(String nodeName, Node node)
	{
		for(child in node.children())
		{
			if(child.name() == "good")
			{
				//preserve line structure, eliminate leading/trailing spaces and blank lines
				def text = child.text().readLines().collect{it.trim()}.findAll{it != ""}.join(EOL)
				data.goodTestFile << text + EOL
			}
			else if(child.name() == "bad")
			{
				//preserve line structure, eliminate leading/trailing spaces and blank lines
				def text = child.text().readLines().collect{it.trim()}.findAll{it != ""}.join(EOL)
				data.badTestFile << text + EOL
			}
		}
	}
	
	Object thesaurusFile(String nodeName, Node node)
	{
		data.currentSection = "thesaurusFile"
		
		//Set the character encoding at the top of the MyThes .dat file
		def line = HunspellXMLUtils.myThesCharacterSet(data.metadata.characterSet) + "\n"
		data.datFile << line
		data.idxOffset = line.getBytes(data.metadata.javaEncoding).size()
		
		processChildren(node)
		
		data.idxFile << HunspellXMLUtils.myThesCharacterSet(data.metadata.characterSet) + "\n"
		data.idxFile << data.idxCount + "\n"
		data.idxList.sort{it.entry}.each{item->
			data.idxFile << item.entry + "|" + item.offset + "\n"
		}
	}

	Object tryChars(String nodeName, Node node)
	{
		def chars = node.text().replaceAll(/\s/, "")
		if(node.attributes().capitalize == "true")
		{
			def stop = chars.size()
			for(int i = 0; i<stop; i++)
			{
				def lower = chars[i]
				def upper = lower.toUpperCase()
				if(lower != upper)
				{
					chars += upper
				}
			}
		}
		data.affFile << nameMap[nodeName] + " " + chars + commAttr(node) + EOL
	}
	
	Object words(String nodeName, Node node)
	{		
		if(data.currentSection == "dictionaryFile")
		{
			def lines = []
			def flags = formatFlagList(node.attributes().flags ?: "")
			def morph = (node.attributes().morph ?: "").trim()
	
			//Look for <w>word</w> in the children
			def children = node.children()
			if(children.find{it.name() == "w"})
			{
				def synonyms = []
				for(child in children)
				{
					if(child.name() == "w")
					{
						/*
						def word = child.text().trim()
						def childFlags = formatFlagList(child.attributes().flags ?: "")
						def childMorph = (child.attributes().morph ?: "").trim()
						def wordLine = child.text().trim()
						if(childFlags){wordLine += "/" + childFlags}
						if(childMorph){wordLine += "\t" + childMorph}
						lines << wordLine
						*/
						def line = child.text().trim()
						def childFlags = formatFlagList(child.attributes().flags ?: "")
						def childMorph = (child.attributes().morph ?: "").trim()
						addDictionaryWords([line], [flags,childFlags].join(" "), [morph, childMorph].join(" "))
					}
				}
			}
			//Otherwise look for a list of words as text, one word per line
			else if(child.text())
			{
				lines = node.text().split(/\r?\n/).toList()
				addDictionaryWords(lines, flags, morph)
			}
		}
	}
	
	def addDictionaryWords(List lines, String flags, String morph)
	{
		for(line in lines)
		{
			////Remove leading space and t
			//line = line.replaceAll(/^[\s\t]*/, "")
			////Add non-blank lines to the data.dicFile
			//if(line != "")
			//{
			//	data.dicFile << line
			//	data.dicFile << (flags ? ("/" + flags) : "")
			//	data.dicFile << (morph ? ("\t" + morph) : "")
			//	data.dicFile << EOL
			//	data.dicCount++
			//}
			
			line = line.trim()
			if(line != "")
			{
				def word = ""
				def wordFlags = ""
				def wordMorph = ""
				
				//flagStart starts at the first / not preceded by a \.
				//I.e. \/ is the escape character for / in the Hunspell file.
				def flagStart = line.indexOf("/")
				while(flagStart > -1)
				{
					if(flagStart > 0 && line.charAt(flagStart - 1) == '\\')
					{
						flagStart = line.indexOf("/", flagStart+1)
					}
					else
					{
						break
					}
				}
				
				//morphStart starts at the first space or tab character
				def morphStart = line.indexOf("\t")
				if(morphStart == -1){morphStart = line.indexOf(" ")}
				
				//Split out the word, flags, and morphology fields
				if(flagStart > 0 && flagStart+1 < line.size() && morphStart > 0 && morphStart+1 < line.size())
				{
					word = line[0..flagStart-1]
					wordFlags = line[flagStart+1..morphStart-1]
					wordMorph = line[morphStart+1..-1]
				}
				else if(flagStart > 0 && flagStart+1 < line.size())
				{
					word = line[0..flagStart-1]
					wordFlags = line[flagStart+1..-1]
				}
				else if(morphStart > 0 && morphStart+1 < line.size())
				{
					word = line[0..morphStart-1]
					wordMorph = line[morphStart+1..-1]
				}
				else if(flagStart > 0)
				{
					word = line[0..flagStart-1]
				}
				else if(morphStart > 0)
				{
					word = line[0..morphStart-1]
				}
				else
				{
					word = line
				}
				//log.info("[$line] parsed as [$word][$wordFlags][$wordMorph]")
				
				//Merge the flags, merge the morphology
				List addFlags = flags.split(/ /).toList()
				List addMorph = morph.split(/ /).toList()
				List curFlags = hunspellFlagsToList(wordFlags)
				List curMorph = wordMorph.split(/[\s\t]/).toList()
				//log.info(curFlags.toString() + " + " + addFlags.toString() + " ... ")
				for(f in addFlags)
				{
					if(!curFlags.contains(f)){curFlags << f}
				}
				for(m in addMorph)
				{
					if(!curMorph.contains(m)){curMorph << m}
				}
				//log.info(" => " + curFlags.toString())
				
				//Remove any unneeded spaces/tabs
				curFlags = curFlags.findAll{it.trim()}.collect{it.trim()} //keep only non-blank list items, then trim all the remaining non-blank items
				curMorph = curMorph.findAll{it.trim()}.collect{it.trim()} //keep only non-blank list items, then trim all the remaining non-blank items

				//Output the word
				def entry = word
				entry += (curFlags ? ("/" + formatFlagList(curFlags)) : "")
				entry += (curMorph ? ("\t" + curMorph.join(" ")) : "")
				entry += EOL
				data.dicList << entry
				data.dicCount++
				
				if(curFlags)
				{
					check.addWordFlag(word, formatFlagList(curFlags))
				}
			}
		}
	}
	
	def addThesaurusData(List lines)
	{
		for(line in lines)
		{
			//Remove leading space
			line = line.trim()
			//Add non-blank lines to the data.datFile
			if(line != "")
			{
				line = line + "\n"
				data.datFile << line
				
				//Check if this is a new entry
				if(line =~ /^[^\|]+\|[0-9]+$/)
				{
					def entry = line.split(/\|/).toList()[0]
					data.idxCount++
					data.idxList << [entry:entry, offset:data.idxOffset]
				}
				
				//Update the byte offset
				data.idxOffset += line.getBytes(data.metadata.javaEncoding).size()
			}
		}
	}
}