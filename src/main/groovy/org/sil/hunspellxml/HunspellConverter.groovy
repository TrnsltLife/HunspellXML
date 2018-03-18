package org.sil.hunspellxml

import java.io.File;

class HunspellConverter 
{
	//Utility class to parse Hunspell .aff and .dic files
	//and convert them to HunspellXML format
	
	File dicFile   //Input Hunspell dictionary file
	File affFile   //Input Hunspell affix file
	File xmlFile   //Output HunspellXML file
	File goodFile  //Test file for good words
	File wrongFile //Test file for wrong words
	File datFile   //Input MyThes file
	Log log = new Log()
	public static final defaultOptions = Collections.unmodifiableMap([
		baseFile:"",
		datFile:"",
		charSet:"",
		defaultCharSet:"",
		defaultFlagType:"",
		defaultLangCode:"",
		logLevel:Log.WARNING,
		outputFileName:"",
		preferWallOfText:false,	//prefer a big wall of text for MyThes data instead of putting it in XML tags
		skipAff:false,
		skipDic:false,
		skipTests:false,
		skipDat:false,
		suppressMyBlankLines:false,
		suppressMyComments:false
		])
	def options = [:]
	
	def defaultCharSet = "UTF-8"
	def charSet = ""
	def javaCharSet = ""
	def outCharSet = ""
	def outJavaCharSet = ""
	
	def defaultFlagType = "short"
	def flagType = ""
	
	def defaultLangCode = "und" //Code for undefined language in ISO639-3
	
	def foundLangCode = false
	def foundCharacterSet = false
	def foundFlagType = false
	
	def mapCount = 0
	def phoneCount = 0
	def replaceCount = 0
	def iconvCount = 0
	def oconvCount = 0
	def breakCount = 0
	def compoundRulesCount = 0
	def compoundPatternsCount = 0
	def affixCount = [prefix:0, suffix:0]
	def lastAffixFlag = ""
	def aliasFlags = []
	def aliasMorphemes = []
	
	def writer
	def mostRecentWriter
	def affixesWriter
	def compoundsWriter
	def convertWriter
	def settingsWriter
	def suggestionsWriter
	
	def phoneWriter
	def replacementsWriter
	def mappingsWriter
	
	public static final EOL = "\r\n"
	
	def nameMap = [
"#":[function:"comment", level:[]],
"AF":[function:"aliasFlags", level:["hunspell","affixFile"]],
"AM":[function:"aliasMorphemes", level:["hunspell","affixFile"]],
"BREAK":[function:"breakChars", level:["hunspell","affixFile","compounds","breakChars"]],
"br":[function:"br", level:[]],
"SET":[function:"characterSet", level:["hunspell","affixFile","settings"]],
"CHECKCOMPOUNDCASE":[function:"checkCompoundCase", level:["hunspell","affixFile","compounds"]],
"CHECKCOMPOUNDDUP":[function:"checkCompoundDuplicates", level:["hunspell","affixFile","compounds"]],
"CHECKCOMPOUNDREP":[function:"checkCompoundReplacements", level:["hunspell","affixFile","compounds"]],
"CHECKCOMPOUNDTRIPLE":[function:"checkCompoundTriple", level:["hunspell","affixFile","compounds"]],
"CHECKSHARPS":[function:"checkSharpS", level:["hunspell","affixFile","settings"]],
"CIRCUMFIX":[function:"circumfix", level:["hunspell","affixFile","settings"]],
"COMPLEXPREFIXES":[function:"complexPrefixes", level:["hunspell","affixFile","settings"]],
"COMPOUNDFLAG":[function:"compound", level:["hunspell","affixFile","compounds"]],
"COMPOUNDBEGIN":[function:"compoundBegin", level:["hunspell","affixFile","compounds"]],
"COMPOUNDFORBIDFLAG":[function:"compoundForbid", level:["hunspell","affixFile","compounds"]],
"COMPOUNDEND":[function:"compoundEnd", level:["hunspell","affixFile","compounds"]],
"COMPOUNDMIDDLE":[function:"compoundMiddle", level:["hunspell","affixFile","compounds"]],
"COMPOUNDMIN":[function:"compoundMin", level:["hunspell","affixFile","compounds"]],
"COMPOUNDMORESUFFIXES":[function:"compoundMoreSuffixes", level:["hunspell","affixFile","compounds"]],
"COMPOUNDPERMITFLAG":[function:"compoundPermit", level:["hunspell","affixFile","compounds"]],
"COMPOUNDROOT":[function:"compoundRoot", level:["hunspell","affixFile","compounds"]],
"COMPOUNDRULE":[function:"compoundRules", level:["hunspell","affixFile","compounds","compoundRules"]],
"CHECKCOMPOUNDPATTERN":[function:"compoundPatterns", level:["hunspell","affixFile","compounds","compoundPatterns"]],
"COMPOUNDSYLLABLE":[function:"compoundSyllable", level:["hunspell","affixFile","compounds"]],
"COMPOUNDWORDMAX":[function:"compoundWordMax", level:["hunspell","affixFile","compounds"]],
"FLAG":[function:"flagType", level:["hunspell","affixFile","settings"]],
"FORBIDDENWORD":[function:"forbiddenWord", level:["hunspell","affixFile","settings"]],
"FORBIDWARN":[function:"forbidWarn", level:["hunspell","affixFile","suggestions"]],
"FORCEUCASE":[function:"forceUpperCase", level:["hunspell","affixFile","compounds"]],
"FULLSTRIP":[function:"fullStrip", level:["hunspell","affixFile","settings"]],
"ICONV":[function:"input", level:["hunspell","affixFile","convertInput"]],
"IGNORE":[function:"ignore", level:["hunspell","affixFile","settings"]],
"KEEPCASE":[function:"keepCase", level:["hunspell","affixFile","settings"]],
"KEY":[function:"keyboard", level:["hunspell","affixFile","suggestions"]],
"LANG":[function:"languageCode", level:["hunspell","affixFile","settings"]],
"LEMMA_PRESENT":[function:"lemmaPresent", level:["hunspell","affixFile","settings"]],
"MAP":[function:"mappings", level:["hunspell","affixFile","suggestions","mappings"]],
"MAXCPDSUGS":[function:"maxCompoundSuggestions", level:["hunspell","affixFile","suggestions"]],
"MAXDIFF":[function:"maxDifference", level:["hunspell","affixFile","suggestions"]],
"MAXNGRAMSUGS":[function:"maxNGramSuggestions", level:["hunspell","affixFile","suggestions"]],
"NEEDAFFIX":[function:"needAffix", level:["hunspell","affixFile","settings"]],
"NOSPLITSUGS":[function:"noSplitSuggestions", level:["hunspell","affixFile","suggestions"]],
"NOSUGGEST":[function:"noSuggestions", level:["hunspell","affixFile","suggestions"]],
"ONLYINCOMPOUND":[function:"onlyInCompound", level:["hunspell","affixFile","compounds"]],
"ONLYMAXDIFF":[function:"onlyMaxDifference", level:["hunspell","affixFile","suggestions"]],
"OCONV":[function:"output", level:["hunspell","affixFile","convertOutput"]],
"PHONE":[function:"phone", level:["hunspell","affixFile","suggestions","phone"]],
"PSEUDOROOT":[function:"pseudoroot", level:["hunspell","affixFile","settings"]],
"PFX":[function:"prefix", level:["hunspell","affixFile","affixes","prefix"]],
"REP":[function:"replacements", level:["hunspell","affixFile","suggestions","replacements"]],
"SIMPLIFIEDTRIPLE":[function:"simplifiedTriple", level:["hunspell","affixFile","compounds"]],
"SUBSTANDARD":[function:"substandard", level:["hunspell","affixFile","settings"]],
"SFX":[function:"suffix", level:["hunspell","affixFile","affixes","suffix"]],
"SUGSWITHDOTS":[function:"suggestionsWithDots", level:["hunspell","affixFile","suggestions"]],
"SYLLABLENUM":[function:"syllableNum", level:["hunspell","affixFile","compounds"]],
"TRY":[function:"tryChars", level:["hunspell","affixFile","suggestions"]],
"WARN":[function:"warn", level:["hunspell","affixFile","suggestions"]],
"WORDCHARS":[function:"wordChars", level:["hunspell","affixFile","settings"]]
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
			//"flagType", removed for custom processing
			"languageCode",
			"maxCompoundSuggestions",
			"maxDifference",
			"maxNGramSuggestions",
			"keyboard"
		],
	
		"settingCharList": [
			"ignore",
			"wordChars",
			"tryChars"
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
	
	public HunspellConverter(baseFile, Map opts=[:])
	{
		options.putAll(HunspellConverter.defaultOptions)
		options.putAll(opts)
		
		if(options.logLevel) {log.logLevel = options.logLevel}
		
		if(!baseFile instanceof File)
		{
			baseFile = new File(baseFile)
		}
		
		determineFileNames(baseFile)
		
		if(options.outputFileName)
		{
			xmlFile = new File(options.outputFileName)
		}
		else
		{
			xmlFile = new File(affFile.toString().replaceAll(/\.aff/, ".xml"))
		}
		
		if(options.defaultFlagType)
		{
			defaultFlagType = options.defaultFlagType
		}
		if(options.defaultCharSet)
		{
			defaultCharSet = options.defaultCharSet
		}
		if(options.defaultLangCode)
		{
			defaultLangCode = options.defaultLangCode
		}
	}
	
	def determineFileNames(baseFile)
	{
		//Set the files to process. Find the .aff file from the .dic file or one of the other files, .dat, .good, .wrong
		if(baseFile.toString().endsWith(".dic"))
		{
			dicFile = new File(baseFile)
			affFile = new File(baseFile.toString().replaceAll(/\.dic$/, ".aff"))
		}
		else if(baseFile.toString().endsWith(".aff"))
		{
			affFile = new File(baseFile)
			dicFile = new File(baseFile.toString().replaceAll(/\.aff/, ".dic"))
		}
		else if(baseFile.toString().endsWith(".dat"))
		{
			datFile = new File(baseFile)
			def name = datFile.getName()
			if(name.startsWith("thes_"))
			{
				name = name - "thes_"
			}
			name = name.replaceAll(/\.dat/, ".aff")
			affFile = new File(datFile.getParent() + File.separator + name)
		}
		else if(baseFile.toString().endsWith(".good"))
		{
			goodFile = new File(baseFile)
			affFile = new File(baseFile.toString().replaceAll(/\.good$/, ".aff"))
		}
		else if(baseFile.toString().endsWith(".wrong"))
		{
			wrongFile = new File(baseFile)
			affFile = new File(baseFile.toString().replaceAll(/\.wrong$/, ".aff"))
		}
		if(!dicFile)
		{
			dicFile = new File(affFile.toString().replaceAll(/\.aff$/, ".dic"))
		}
		if(!goodFile)
		{
			goodFile = new File(affFile.toString().replaceAll(/\.aff/, ".good"))
		}
		if(!wrongFile)
		{
			wrongFile = new File(affFile.toString().replaceAll(/\.aff/, ".wrong"))
		}
		if(!datFile)
		{
			if(options.datFile)
			{
				datFile = new File(options.datFile) //This way .dat file doesn't have to have the same base name as the .aff file
			}
			else
			{
				datFile = new File(affFile.getParent() + File.separator + "thes_" + affFile.getName().replaceAll(/\.aff/, ".dat"))
			}
		}
	}
	
	
	def convert()
	{
		def processFiles = []
		def missingFiles = []
		if(affFile.exists() && !options.skipAff) {processFiles << affFile}
		else if(!affFile.exists() && !options.skipAff) {missingFiles << affFile}
		if(dicFile.exists() && !options.skipDic) {processFiles << dicFile}
		else if(!dicFile.exists() && !options.skipDic) {missingFiles << dicFile}
		if(goodFile.exists() && !options.skipTests) {processFiles << goodFile}
		if(wrongFile.exists() && !options.skipTests) {processFiles << wrongFile}
		if(datFile.exists() && !options.skipDat) {processFiles << datFile}
		//Tests should never be required unless we've been told to skip the affFile and the dicFile too
		if(!goodFile.exists() && !wrongFile.exists() && !options.skipTests && options.skipAff && options.skipDic)
		{
			missingFiles << goodFile
			missingFiles << wrongFile
		}
		//Thesaurus should never be required unless we've been told to skip the affFile, dicFile, and test files too
		if(!datFile.exists() && !options.skipDat && options.skipAff && options.skipDic && options.skipTests)
		{
			missingFiles << datFile
		}
		
		if(missingFiles)
		{
			log.error("Missing required file${missingFiles.size()>1?'s':''}. ${missingFiles.join(", ")}")
		}
		else
		{
			log.info("HunspellConvert converting ${processFiles.collect{it.getName()}.join(", ")}")
		}
		
		/*
		//If neither file exists, print an error
		if(!dicFile.exists() || !affFile.exists())
		{
			def files = [dicFile.exists()?null:dicFile, affFile.exists()?null:affFile].findAll{it}
			log.error("Missing required file${files.size()>1?'s':''}. ${files.join(", ")}")
		}
		//Otherwise log the files that are being processed
		else
		{
			def files = [options.skipDic?null:dicFile, options.skipAff?null:affFile].findAll{it != null}
			log.info("HunspellConvert converting ${files.collect{it.getName()}.join(", ")}")
		}
		*/

		//Get the actual charset from the file
		//charSet = extractCharacterSet()
		charSet = HunspellXMLUtils.extractHunspellCharacterSet(affFile.getCanonicalPath(), defaultCharSet)
		log.info("Character Set: " + charSet)
		javaCharSet = HunspellXMLUtils.javaCharacterSet(charSet)
		log.info("(Java Character Set: " + javaCharSet + ")")
		
		//Get the output charset
		outCharSet = options.charSet ?: charSet //You can change the output character set by setting the charSet option
		outJavaCharSet = HunspellXMLUtils.javaCharacterSet(outCharSet)
		log.info("Output Character Set: " + outCharSet)
		log.info("(Java Output Character Set: " + outJavaCharSet + ")")
		
		//Create the XML file to write out as we read in the .aff and .dic files
		this.writer = new StringBuffer()
		write("""<?xml version="1.0" encoding="${outJavaCharSet}"?>""")
		write("<hunspell>")
		convertAffFile()
		write(EOL)
		if(!options.skipDic) //convert the dictionary unless told to skip it
		{
			convertDicFile()
			write(EOL)
		}
		else
		{
			log.info("Skipping .dic file.")
		}
		if(!options.skipTests) //convert the tests unless told to skip them
		{
			write("<tests>")
			convertTestFiles()
			write("</tests>")
		}
		else
		{
			log.info("Skipping .good and .wrong files.")
		}
		if(!options.skipDat) //convert the MyThes .dat file unless told to skip it
		{
			convertDatFile()
			write(EOL)
		}
		else
		{
			log.info("Skipping MyThes .dat file.")
		}
		write("</hunspell>")
		
		prettyPrintXML()
		
		//Create directory
		xmlFile.getParentFile().mkdirs()
		
		//Output the file
		xmlFile.withWriter(outJavaCharSet){fileWriter ->
			fileWriter << writer.toString()
		}
	}
	
	private write(text)
	{
		write(writer, text)
		mostRecentWriter = writer
	}
	
	private write(whichWriter, text)
	{
		if(text != "")
		{
			whichWriter << text + EOL
		}
		mostRecentWriter = whichWriter
	}
	
	private prettyPrintXML()
	{
		//Give the XML nice indent levels
		def indent = 0
		def lines = writer.toString().split(/\r?\n/).toList()
		writer = new StringBuffer()
		for(line in lines)
		{
			//Decrease indent level with closing tags
			if(line.startsWith("</"))
			{
				indent--
			}
			//Print lines starting with XML tags/end-tags at the appropriate indent level
			if(line.startsWith("<"))
			{
				writer << "\t" * (indent>=0?indent:0) + line + EOL
			}
			//Print plain text flush to the margin
			else
			{
				writer << line + EOL
			}
			//Line starts with an opening tags
			if(line.startsWith("<") && !line.startsWith("</") && !line.startsWith("<!"))
			{
				//If there's no ending tag on this line, increase the indent level
				if(line.indexOf("/>") < 0 && line.indexOf("</") < 0) //no end tag on this line
				{
					indent++
				}
			}
		}
	}
	
	def newAnnotatedLine()
	{
		return [level:[], text:"", xml:"", verb:"", function:"", params:"", comment:"", insertBefore:""]
	}
	
	private convertAffFile()
	{
		affixesWriter = new StringBuffer()
		compoundsWriter = new StringBuffer()
		convertWriter = new StringBuffer()
		settingsWriter = new StringBuffer()
		suggestionsWriter = new StringBuffer()
		
		def text = ""
		//Load the text from the affFile unless options.skipAff is true
		//Even if the affFile isn't read in, convertAffFile() will still generate
		//an <affixFile> section with <settings> for <languageCode>, <characterSet> and <flagType>
		if(!options.skipAff)
		{
			text = affFile.getText(javaCharSet)
		}
		else
		{
			log.info("Skipping .aff file.")
		}
		if(text.startsWith("\uFEFF")){text -= "\uFEFF"} //remove UTF-8 BOM
		def lines = text.split("\r?\n").toList()
		
		//Set up a list of annotated lines
		def annotatedLines = []
		for(line in lines)
		{
			def newLine = newAnnotatedLine()
			newLine.text = line
			annotatedLines << newLine
		}
		
		//Loop over all the lines in the .aff file and assign their values to annotateLines entries
		for(int i=0; i<annotatedLines.size(); i++)
		{
			def line = annotatedLines[i]
			if(line.text.trim() == "")
			{
				line.verb = "br"
				line.function = "br"
				line.level = nameMap[line.verb]?.level?.clone() ?: []
				line.xml = br(line)
			}
			else
			{
				//Sometimes comments don't have a space after the # - fix that
				if(line.text =~ /^#[^\s]/)
				{
					line.text = line.text.replaceFirst(/^#/, "# ")
				}
				
				line.verb = line.text.replaceAll(/^([^\s]+).*/, "\$1")

				line.function = nameMap[line.verb]?.function ?: ""
				line.level = nameMap[line.verb]?.level?.clone() ?: []

				if(line.function)
				{
					//line.param and line.comment will be set in the function
					callFunction(line)
				}
				
				//Insert before current line. Currently used by the affix(...) method
				if(line.insertBefore)
				{
					annotatedLines.add(i, line.insertBefore)
					i++
				}
			}
		}

		
		//Loop forwards to assign initial comments to Hunspell level
		//Stop assigning after the end of the first set of blank lines
		//This is to try to get comments to come out associated with
		//the right parts of the file
		def foundFirstComment = false
		def foundFirstBreak = false
		for(line in annotatedLines)
		{
			if(line.level && line.level.size() >= 1)
			{
				//log.debug("break forwards comment levels on " + line.text)
				break
			}
			else
			{
				if(line.function == "br")
				{
					if(foundFirstComment)
					{
						foundFirstBreak = true
					}
					line.level = ["hunspell","affixFile"]
				}
				else if(line.function == "comment")
				{
					if(foundFirstBreak){break} //break when we reach the first comment after the first break
					else
					{
						foundFirstComment = true
						line.level = ["hunspell","affixFile"]
					}
				}
			}
		}
		
		
		//Loop backwards to assign comments and blank lines backward from groupings like settings, suggestions
		def prevLevel = ["hunspell","affixFile"]
		for(int i = annotatedLines.size() - 1; i>=0; i--)
		{
			def line = annotatedLines[i]
			if(line.level == [])
			{
				if(line.function == "br" || line.function == "comment")
				{
					 prevLevel = adjustCommentLevel(prevLevel)
					 line.level = prevLevel
				}
			}
			else
			{
				prevLevel = line.level
			}
		}
		
		//log.debug("First Levels")
		//showLevels(annotatedLines)
		
		//Loop over the file and gather element groups together (settings, suggestions, compounds, convertInput, convertOutput, affixes)
		for(type in ["settings", "suggestions", "compounds", "convertInput", "convertOutput", "affixes"])
		{
			def lastIndex = -1
			def skipped = false
			for(int i=0; i < annotatedLines.size(); i++)
			{
				if(annotatedLines[i].level.contains(type))
				{
					if(!skipped)
					{
						lastIndex = i
					}
					else
					{
						annotatedLines.add(lastIndex, annotatedLines.remove(i))
						lastIndex++
						i--
					}
				}
				else
				{
					if(lastIndex > -1)
					{
						skipped = true
					}
				}
			}
		}
		

//log.debug("After the Gathering")
//showLevels(annotatedLines)
		
		
		//Loop forwards and write each line to its appropriate section (e.g. settings, convert, suggestions...)
		prevLevel = ["hunspell"]
		def prevLine = newAnnotatedLine()
		for(line in annotatedLines)
		{
			def thisLevel = line.level
			
			//Print closing and opening tags as we shift levels in the XML hierarchy
			printCloseOpenTags(prevLevel, thisLevel, prevLine)

			if(!line.level)
			{
				log.debug("Warning: Line not handled.${EOL}\t ${line.text}")
				continue; //and don't change the prevLevel
			}
			else
			{
				if(options.suppressMyComments && (line.function == "comment" || line.function == "br"))
				{
					//don't output this <br/> or comment
				}
				else if(options.suppressMyBlankLines && line.xml.trim() == "")
				{
					//don't output this blank line
				}
				else
				{
					write(line.xml) // + (line.comment?"<!-- ${line.comment} -->":"")
				}
			}
			prevLevel = line.level
			prevLine = line
		}
		printCloseOpenTags(prevLevel, ["hunspell"], prevLine) //print the end of the xml file
	}
	
	
	def setParamsAndComments(line, nbrParams, nbrParams2 = -1, nbrParams3 = -1)
	{
		//Given the line text and a target for a number of params,
		//determine how many params there are and extract
		//the end-of-line comment if there is one
		if(nbrParams2 < nbrParams) {nbrParams2 = nbrParams}
		if(nbrParams3 < nbrParams2) {nbrParams3 = nbrParams2}
		
		//log.debug("setParamsAndComments('" + line.text + "', $nbrParams, $nbrParams2, $nbrParams3)")
		String text = line.text.replaceAll(/^[^\s]+(.*)/, "\$1") //remove the verb and space after the verb
		text = text.replaceAll(/\t/, " ") //replace tabs with spaces
		text = text.replaceAll(/ +/, " ") //Reduce multiple spaces to a single space
		
		List paramList = []
		String comment = ""
		
		if(text.indexOf(" # ") >= 0)
		{
			//Treat the whole string as params
			List options = []
			def testText = text.trim()
			def testComment = ""
			List testParamList = testText.split(/\s/, nbrParams3).toList().collect{it.trim()}
			testParamList = testParamList.findAll{it != ""}
			//Check the greatest nbrParam first, then check each smaller one
			for(nbr in [nbrParams3, nbrParams2, nbrParams])
			{
				if(testParamList.size() == nbr)
				{
					List newList = new ArrayList()
					newList.addAll(testParamList)
					options << [size: testParamList.size(), paramList:newList, comment:testComment]
					break;
				}
			}
			
			//Treat the whole string as comments. Good for functions with 0 params.
			if(nbrParams == 0 && text.trim().startsWith("#"))
			{
				testComment = text.trim().replaceAll(/^#/, "").trim()
				//Check the greatest nbrParam first, then check each smaller one
				options << [size:0, paramList:[], comment:testComment]
			}
			
			//Loop over each potential start of a comment (splitting out the comment) to check if there are enough params
			//This requires end of line comments to start with space-pound-space, " # "
			//Comments that start with just space-pound " #" will not be processed correctly
			for(int i=0; i >= 0; i = text.indexOf(" # ", i))
			{
				testText = text.substring(0, i).trim()
				testComment = text.substring(i).trim()
				testParamList = testText.split(/\s/, nbrParams3).toList().collect{it.trim()}
				testParamList = testParamList.findAll{it.trim() != ""}
				//Check the greatest nbrParam first, then check each smaller one
				for(nbr in [nbrParams3, nbrParams2, nbrParams])
				{
					//Matches the size of the greatest allowed number of params
					if(testParamList.size() == nbr)
					{
						//The last candidate param in the list doesn't start with a comment tag: "# "
						if(testParamList.size() > 0 && !testParamList[-1].startsWith("# "))
						{
							List newList = new ArrayList()
							newList.addAll(testParamList)
							options << [size: testParamList.size(), paramList:newList, comment:testComment]
							break;
						}
					}
				}
				i++
			}
			
			//Sort options, ascending by the size of the comment, then sort by number of params.
			options.sort{a,b->
				if(a.comment.size() == b.comment.size())
				{return a.size() <=> b.size()}
				return a.comment.size() <=> b.comment.size()}

			
			//log.debug("Sort of options:")
			//options.each{log.debug("\t" + it)}
			
			if(options.size() > 0)
			{
				//Prefer the greatest number of params and longest comment
				//log.debug("Selected" + options[-1] + "\r\n")
				paramList = options[-1].paramList
				comment = options[-1].comment
			}
		}
		else
		{
			def paramList1 = text.split(/\s/, nbrParams).toList() 
			def paramList2 = text.split(/\s/, nbrParams2).toList()
			def paramList3 = text.split(/\s/, nbrParams3).toList()
			if(paramList3.size() > paramList2.size())
			{
				paramList = paramList3
			}
			else if(paramList2.size() > paramList1.size())
			{
				paramList = paramList2
			}
			else
			{
				paramList = paramList1
			}			
		}
		
		paramList = paramList.collect{it.trim()}
		line.paramList = paramList
		line.params = paramList.join(" ").trim()
		line.comment = comment.replaceAll(/^#/, "").trim()
	}
	
	
	def showLevels(annotatedLines)
	{
		//Show levels
		for(line in annotatedLines)
		{
			//log.debug(line.level.toString() + "\t" + line.function + "\t" + line.text)
			log.debug(line.level.toString() + " " + line.function + " " + line.params + 
				(line.comment ? " # " + line.comment : ""))
		}
	}
	
	
	private convertDicFile()
	{
		def text = dicFile.getText(javaCharSet)
		if(text.startsWith("\uFEFF")){text -= "\uFEFF"} //remove UTF-8 BOM
		def lines = text.split("\r?\n").toList()
		
		//The first line contains the count of how many words in the dictionary.
		//It can also contain an end-of-line comment. Get this comment.
		def comment = ""
		if(lines[0].indexOf("#") > -1)
		{
			comment = lines[0].replaceAll(/^[^#]*#/, "").trim()
			comment = " comment=" + '"' + esc(comment) + '"'
		}
		
		//Loop over all the words and sort them into the wordMap keyed by flag and morph
		def wordMap = [:]
		for(int i=1; i<lines.size(); i++)
		{
			def line = lines[i]
			def word = ""
			def flags = ""
			def morph = ""
			if(line =~ /^(.*?[^\\])\/.*/)
			{
				word = line.replaceAll(/^(.*?[^\\])\/.*/, "\$1") //read up to the first / not preceded by a \ and keep that as the word
				def rest = line - word
				rest = rest.replaceAll(/\//, "")
				(flags, morph) = rest.split(/[\s\t]/, 2).toList()
				if(!flags){flags = ""}
				if(!morph){morph = ""}
			}
			else
			{
				(word, morph) = line.split(/[\s\t]/, 2).toList()
				if(!flags){flags = ""}
				if(!morph){morph = ""}
			}
			flags = flags.trim()
			morph = morph.trim()
			
			def key = flags + "\t" + morph
			if(!wordMap[key])
			{
				wordMap[key] = []
			}
			wordMap[key] << word
		}
		
		//Create output for all the words, each in a <words...> group by flag and morph
		StringBuffer wordWriter = new StringBuffer()
		wordWriter << "<dictionaryFile${comment}>" + EOL
		for(key in wordMap.keySet().sort())
		{
			def (flags, morph) = key.split("\t", 2).toList()
			wordWriter << "<words"
			if(flags){wordWriter << """ flags="${esc(toExpandedFlagList(flags, "dic: /${flags}"))}\""""}
			if(morph){wordWriter << """ morph="${esc(expandMorphemeAlias(morph))}\""""}
			wordWriter << ">"
			if(options.preferWallOfText)
			{
				wordWriter << EOL //EOL after <words>
				wordMap[key].sort().each{word->
					wordWriter << esc(word) + EOL
				}
			}
			else
			{
				int wCount = 0
				wordMap[key].sort().each{word->
					wordWriter << "<w>" + esc(word) + "</w>"
					wCount++
					if(wCount >= 5)
					{
						wordWriter << EOL
						wCount = 0
					}
				}
			}
			wordWriter << "</words>" + EOL
		}
		wordWriter << "</dictionaryFile>" + EOL
		write(wordWriter.toString())
	}
	
	private convertTestFiles()
	{
		if(!options.skipTests)
		{
			//Good words
			if(goodFile.exists())
			{
				def text = goodFile.getText(javaCharSet)
				if(text.startsWith("\uFEFF")){text -= "\uFEFF"} //remove UTF-8 BOM
				def lines = text.split("\r?\n").toList()
				write("<good>" + EOL + esc(lines.join(EOL)) + EOL + "</good>" + EOL)
			}
			
			//Wrong word
			if(wrongFile.exists())
			{
				def text = wrongFile.getText(javaCharSet)
				if(text.startsWith("\uFEFF")){text -= "\uFEFF"} //remove UTF-8 BOM
				def lines = text.split("\r?\n").toList()
				write("<bad>" + EOL + esc(lines.join(EOL)) + EOL + "</bad>" + EOL)
			}
		}
	}
	
	private convertDatFile()
	{
		if(!options.skipDat)
		{
			if(datFile.exists())
			{
				//Get encoding on first line
				def text = datFile.getText(javaCharSet)
				if(text.startsWith("\uFEFF")){text -= "\uFEFF"} //remove UTF-8 BOM
				def lines = text.split("\r?\n").toList()
				def datCharSet = lines[0]
				
				def javaDatCharSet = HunspellXMLUtils.javaCharacterSet(datCharSet)
				text = datFile.getText(javaDatCharSet)
				lines = text.split("\r?\n").toList()

				//Create output for all the words, each in a <words...> group by flag and morph
				StringBuffer wordWriter = new StringBuffer()
				wordWriter << "<thesaurusFile>" + EOL
				
				if(options.preferWallOfText)
				{
					if(lines.size() > 1)
					{
						wordWriter << "<entries>" + EOL
						for(int i=1; i<lines.size(); i++)
						{
							wordWriter << lines[i] + EOL
						}
						wordWriter << "</entries>" + EOL
					}
				}
				else
				{
					def foundStart = false
					for(int i=0; i<lines.size(); i++)
					{
						def line = lines[i]
						if(line =~ /^[^|]+\|[0-9]+$/)
						{
							if(foundStart) //Close the previous entry
							{
								wordWriter << "</entry>" + EOL
							}
							//Find the word for this thesaurus entry, and the number of synonym lists it has
							def (word, count) = line.split(/\|/).toList()
							count = count.trim()
							if(count.isInteger()) {count = count.toInteger()}
							else
							{
								log.error("Couldn't determine synonym count. ${count} is not an integer." + EOL + line)
								continue
							}
							wordWriter << """<entry word="${esc(word)}">""" + EOL
							//get all the synonym lists for the word
							for(int j=i+1; j<i+count+1 && j<lines.size(); j++)
							{
								line = lines[j]
								def (pos, synonyms) = line.split(/\|/, 2).toList()
								wordWriter << """<synonyms info="${esc(pos)}">"""
								wordWriter << synonyms.split(/\|/).toList().collect{"<s>" + esc(it) + "</s>"}.join("")
								wordWriter << "</synonyms>" + EOL
							}
							i += count
							wordWriter << "</entry>" + EOL
						}
					}
				}
				
				wordWriter << "</thesaurusFile>" + EOL
				write(wordWriter.toString())
			}
		}
	}
	
	def callFunction(line)
	{
		def function = line.function
		
		//Search for one of the generic conversion methods
		for(entry in functionMap)
		{
			if(entry.value.contains(function))
			{
				function = entry.key
				break
			}
		}
		if(function)
		{
			this."${function}"(line)
		}
	}
	
	def List adjustCommentLevel(prevLevel)
	{
		def validLevels = ["hunspell", "metadata", "customAttributes", "affixFile", "affixes", "compounds", "convertInput", "convertOutput", /*"output",*/ "settings", "suggestions"]
		while(prevLevel.size() > 0)
		{
			if(validLevels.contains(prevLevel[-1])){return prevLevel}
			else
			{
				prevLevel.pop()
			}
		}
		return []
	}

	
	def printCloseOpenTags(prevLevel, thisLevel, prevLine)
	{
		if(thisLevel && thisLevel != prevLevel)
		{
			//Close the previous tag(s)
			if(prevLevel.size() > 1)
			{
				for(int i=prevLevel.size()-1; i>=0; i--)
				{
					if(thisLevel.size() > i && prevLevel[i] == thisLevel[i]){break}
					if(!["prefix","suffix"].contains(prevLevel[i]))
					{
						affixCount["prefix"] = 0
						affixCount["suffix"] = 0
					}
					
					//Print out required settings if they haven't been found by the end of <settings> or <affixFile>
					if(prevLevel[i] == "settings" || prevLevel[i] == "affixFile")
					{
						if(prevLevel[i] == "affixFile" && (!foundLangCode || !foundCharacterSet || !foundFlagType))
						{
							write("<settings>")
						}
						if(!foundLangCode)
						{
							write("<languageCode>${defaultLangCode}</languageCode>")
						}
						if(!foundCharacterSet)
						{
							if(options.charSet)
							{
								write("<characterSet>${charSet}</characterSet>")
							}
							else
							{
								write("<characterSet>${defaultCharSet}</characterSet>")
							}
						}
						if(!foundFlagType)
						{
							write("<flagType>${defaultFlagType}</flagType>")
						}
						if(prevLevel[i] == "affixFile" && (!foundLangCode || !foundCharacterSet || !foundFlagType))
						{
							write("</settings>")
						}
						foundLangCode = true
						foundCharacterSet = true
						foundFlagType = true
					}
					
					write("</${prevLevel[i]}>")
				}
			}
			
			//Open new tags
			if(thisLevel.size() > 0)
			{
				//def s = ""
				for(int i=0; i<thisLevel.size(); i++)
				{
					if(prevLevel.size() > i && prevLevel[i] == thisLevel[i]){continue}
					//s =	"${EOL}<${thisLevel[i]}>" + s
					if(!["prefix","suffix"].contains(thisLevel[i]))
					{
						def comment = ""
						if(thisLevel[i] == prevLine.function)
						{
							comment = commAttr(prevLine)
						}
						write("<${thisLevel[i]}${comment}>")
					}
				}
				//write(s)
			}
		}
	}
	
	public String expandFlagAlias(String alias)
	{
		//Take a numeric flag alias and expand it into the flag(s) specified in the Hunspell AF commands
		if(aliasFlags.size() > 0)
		{
			int index = alias.isInteger() ? Integer.parseInt(alias) : 0
			if(index > 0 && index < aliasFlags.size())
			{
				return aliasFlags.get(index)
			}
		}
		return alias
	}
	
	public String expandMorphemeAlias(String alias)
	{
		//Take a numeric morpheme alias and expand it into the morpheme(s) specified in the Hunspell AM commands
		if(aliasMorphemes.size() > 0)
		{
			int index = alias.isInteger() ? Integer.parseInt(alias) : 0
			if(index > 0 && index < aliasMorphemes.size())
			{
				return aliasMorphemes.get(index)
			}
		}
		return alias
	}
	
	public String toExpandedFlagList(String flags, origText)
	{
		flags = expandFlagAlias(flags)
		return toFlagList(flags, origText)
	}
	
	public String toFlagList(String flags, origText)
	{
		flags = flags.trim()
		flags = flags.replaceAll(/\s+/, "") //remove all spaces
		def flagList = []
		if(flagType == "short" || flagType == "")
		{
			for(char c in flags)
			{
				flagList << c
				if(c > '\u00ff')
				{
					log.warning("Flag character outside of the extended ASCII range: ${c}")
				}
			}
		}
		else if(flagType == "UTF-8")
		{
			for(char c in flags)
			{
				flagList << c
			}
		}
		else if(flagType == "long")
		{
			for(int i=0; i<flags.size(); i+=2)
			{
				if(i+1 < flags.size())
				{
					def flag = "" + flags.charAt(i) + flags.charAt(i+1)
					flagList << flag
					if(flags.charAt(i) > '\u00ff' || flags.charAt(i+1) > '\u00ff')
					{
						log.warning("Flag character outside of the extended ASCII range: ${flag}${EOL}\t${origText}")
					}
				}
				else
				{
					def flag = flags.charAt(i)
					log.warning("Partial flag: ${flag}. Flags of type 'long' should be two characters long.${EOL}\t${origText}")
				}
			}
		}
		else if(flagType == "num")
		{
			if(!(flags =~ /^[0-9,]+$/))
			{
				log.warning("Lists of flags of type 'num' should contain only 0-9 and comma (,).${EOL}\t${origText}")
			}
			flagList = flags.split(",").toList()
		}
		
		//Check each individual flag
		for(flag in flagList)
		{
			checkFlag(flag.toString(), origText.toString())
		}
		
		
		return flagList.join(" ")
	}
	
	public String expandAndCheckFlag(String flag, origText)
	{
		flag = expandFlagAlias(flag)
		return checkFlag(flag, origText)
	}
	
	public String checkFlag(String flag, origText)
	{
		if(flagType == "short" || flagType == "")
		{
			if(flag.size() < 1)
			{
				log.warning("Flag of type 'short' (default) has too few characters. It should be one character long: ${flag}${EOL}\t${origText}")
			}
			else if(flag.size() > 1)
			{
				log.warning("Flag of type 'short' (default) has too many characters. It should be one character long: ${flag}${EOL}\t${origText}")
			}
			else if(flag.charAt(0) > '\u00ff')
			{
				log.warning("Flag of type 'short' is a character outside of the extended ASCII range: ${flag}${EOL}\t${origText}")
			}
		}
		else if(flagType == "UTF-8")
		{
			if(flag.size() < 1)
			{
				log.warning("Flag of type 'UTF-8' has too few characters. It should be one character long: ${flag}${EOL}\t${origText}")
			}
			else if(flag.size() > 1)
			{
				log.warning("Flag of type 'UTF-8' has too many characters. It should be one character long: ${flag}${EOL}\t${origText}")
			}
		}
		else if(flagType == "long")
		{
			if(flag.size() < 2)
			{
				log.warning("Flag of type 'long' has too few characters. It should be two characters long: ${flag}${EOL}\t${origText}")
			}
			else if(flag.size() > 2)
			{
				log.warning("Flag of type 'long' has too many characters. It should be two characters long: ${flag}${EOL}\t${origText}")
			}
			else if(flag.charAt(0) > '\u00ff' || flag.charAt(1) > '\u00ff')
			{
				log.warning("Flag of type 'long' has a character outside of the extended ASCII range: ${flag}${EOL}\t${origText}")
			}
		}
		else if(flagType == "num")
		{
			if(!(flag =~ /^[0-9]+$/))
			{
				log.warning("Flag of type 'num' should contain only the digits 0-9.${EOL}\t${origText}")
			}
		}
		
		return flag //unchanged
	}
	
	public String toCharList(s)
	{
		def list = []
		for(char c in s)
		{
			list << c
		}
		return list.join(" ")
	}
	
	public static String esc(s)
	{
		if(!s){return ""}
		//Escape XML entities
		s = s.replaceAll(/&/, "&amp;")
		s = s.replaceAll(/</, "&lt;")
		s = s.replaceAll(/>/, "&gt;")
		s = s.replaceAll(/"/, "&quot;")
		s = s.replaceAll(/'/, "&apos;")
		return s
	}
	
	//*********************
	//Generic Converter Functions
	//*********************
	def setting0(line)
	{
		setParamsAndComments(line, 0) //sets the line.params and line.comment attributes
		line.xml = "<${line.function}${commAttr(line)}/>"
	}
	
	def setting1(line)
	{
		setParamsAndComments(line, 1) //sets the line.params and line.comment attributes
		line.xml = "<${line.function}${commAttr(line)}>${esc(line.params)}</${line.function}>"
		
		if(line.function == "languageCode")
		{
			foundLangCode = true
		}
		else if(line.function == "characterSet")
		{
			foundCharacterSet = true
		}
	}
	
	def settingCharList(line)
	{
		setParamsAndComments(line, 1) //sets the line.params and line.comment attributes
		line.xml = "<${line.function}${commAttr(line)}>" + esc(toCharList(line.params)) + "</${line.function}>"
	}
	
	def setting1Flag(line)
	{
		setParamsAndComments(line, 1) //sets the line.params and line.comment attributes
		line.xml = """<${line.function} flag="${esc(expandAndCheckFlag(line.params, line.text))}"${commAttr(line)}/>"""
	}
	
	def commAttr(line)
	{
		if(line.comment)
		{
			return " comment=" + '"' + esc(line.comment) + '"' 
		}
		return ""
	}
	
	//*********************
	//Converter functions that transform Hunspell into HunspellXML
	//*********************
	def br(line)
	{
		line.xml = "<br/>"
		//line.xml = ""
	}
	
	def comment(line)
	{
		line.params = line.text.replaceAll(/^#\s(.*)/, "\$1")
		line.xml = "<comment>${esc(line.params)}</comment>"
		//line.xml = ""
	}
	
	def aliasFlags(line)
	{
		setParamsAndComments(line, 1)
		if(aliasFlags.size() == 0)
		{
			aliasFlags << line.params.trim()
		}
		else
		{
			aliasFlags << toFlagList(line.params.trim(), line.text)
		}
		line.xml = "<comment>${esc(line.text)} # ${aliasFlags.size()-1}${line.comment ? " # " + line.comment : ""}</comment>"
	}
	
	def aliasMorphemes(line)
	{
		setParamsAndComments(line, 1)
		aliasMorphemes << line.params.trim()
		line.xml = "<comment>${esc(line.text)} # ${aliasMorphemes.size()-1}${line.comment ? " # " + line.comment : ""}</comment>"
	}
	
	def flagType(line)
	{
		setParamsAndComments(line, 1) //sets the line.params and line.comment attributes
		flagType = line.params.trim()
		if(!["short", "UTF-8", "long", "num"].contains(flagType))
		{
			flagType = ""
			line.xml = ""
			log.warning("Invalid option for flag type. Defaulting to 'short'.${EOL}\t${line.text}")
		}
		else
		{
			line.xml = "<flagType${commAttr(line)}>${esc(flagType)}</flagType>"
			foundFlagType = true
		}
		log.info("Flag type set to ${flagType}.")
	}
	
	def mappings(line)
	{
		setParamsAndComments(line, 1) //sets the line.params and line.comment attributes
		if(mapCount == 0 && line.params.trim() =~ /^[0-9]+$/)
		{
			mapCount++
			line.xml = ""
		}
		else
		{
			mapCount++
			def mappings = line.params.trim()
			def mapList = []
			def inParen = false
			def textInParen = ""
			for(int i=0; i<mappings.size(); i++)
			{
				def c = "" + mappings.charAt(i)
				if(c == ")" && inParen)
				{
					inParen = false
					mapList << textInParen
					textInParen = ""
				}
				else if(c == "(" && !inParen){inParen = true}
				else if(inParen)
				{
					textInParen += c
				}
				else
				{
					mapList << c
				}
			}
			
			line.xml = "<map${commAttr(line)}>${esc(mapList.join(" "))}</map>"
		}
	}
	
	def phone(line)
	{
		setParamsAndComments(line, 1) //sets the line.params and line.comment attributes
		if(phoneCount == 0 && line.params.trim() =~ /^[0-9]+$/)
		{
			phoneCount++
			line.xml = ""
		}
		else
		{
			//TODO: Parse the PHONE rule at some point in the future?
			setParamsAndComments(line, 2) //sets the line.params and line.comment attributes
			phoneCount++
			line.xml = "<rule${commAttr(line)}>${esc(line.params)}</rule>"
		}
	}
	
	def replacements(line)
	{
		setParamsAndComments(line, 1) //sets the line.params and line.comment attributes
		if(replaceCount == 0 && line.params.trim() =~ /^[0-9]+$/)
		{
			replaceCount++
			line.xml = ""
		}
		else
		{
			setParamsAndComments(line, 2) //sets the line.params and line.comment attributes
			replaceCount++
			def (from, to) = line.params.trim().split(" ").toList()
			line.xml = """<replace from="${esc(from)}" to="${esc(to)}"${commAttr(line)}/>"""
		}
	}
	
	def breakChars(line)
	{
		setParamsAndComments(line, 1) //sets the line.params and line.comment attributes
		if(breakCount == 0 && line.params.trim() =~ /^[0-9]+$/)
		{
			if(line.params == "0")
			{
				line.xml = """<breakChars off="true"${commAttr(line)}/>"""
				line.level.pop()
			}
			else
			{
				breakCount++
				line.xml = ""
			}
		}
		else
		{
			setParamsAndComments(line, 1) //sets the line.params and line.comment attributes
			breakCount++
			line.xml = """<chars${commAttr(line)}>${esc(line.params)}</chars>"""
		}
	}
	
	def compoundRules(line)
	{
		setParamsAndComments(line, 1) //sets the line.params and line.comment attributes
		if(compoundRulesCount == 0 && line.params.trim() =~ /^[0-9]+$/)
		{
			compoundRulesCount++
			line.xml = ""
		}
		else
		{
			compoundRulesCount++
			line.xml = "<rule${commAttr(line)}>${esc(line.params)}</rule>"

			//TODO: Check compound rule syntax and emit warnings
			checkCompoundRuleSyntax(line.params)
		}
	}
	
	def checkCompoundRuleSyntax(rule)
	{
		//TODO
	}
	
	def compoundPatterns(line)
	{
		setParamsAndComments(line, 1) //sets the line.params and line.comment attributes
		if(compoundPatternsCount == 0 && line.params.trim() =~ /^[0-9]+$/)
		{
			compoundPatternsCount++
			line.xml = ""
		}
		else
		{
			setParamsAndComments(line, 2, 3) //Number of params ranges from 2 - 3. The 3rd, replace, is optional.
			compoundPatternsCount++
			def end = "", start = "", replace = ""
			(end, start, replace) = line.params.trim().split(" ").toList()
			def endChars = "", endFlags = "", startChars = "", startFlags = ""
			(endChars, endFlags) = end.split("/").toList()
			(startChars, startFlags) = start.split("/").toList()
			line.xml = "<pattern"
			line.xml += """ endChars="${esc(endChars)}\""""
			if(endFlags){line.xml += """ endFlags="${esc(toExpandedFlagList(endFlags, line.text))}\""""}
			line.xml += """ startChars="${esc(startChars)}\""""
			if(startFlags){line.xml += """ startFlags="${esc(toExpandedFlagList(startFlags, line.text))}\""""}
			if(replace){line.xml += """ replacement="${esc(replace)}\""""}
			line.xml += "${commAttr(line)}/>"
		}
	}

	def input(line)
	{
		setParamsAndComments(line, 1) //sets the line.params and line.comment attributes
		if(iconvCount == 0 && line.params.trim() =~ /^[0-9]+$/)
		{
			iconvCount++
			line.xml = ""
		}
		else
		{
			setParamsAndComments(line, 2) //sets the line.params and line.comment attributes
			iconvCount++
			def (from, to) = line.params.trim().split(" ").toList()
			line.xml = """<input from="${esc(from)}" to="${esc(to)}"${commAttr(line)}/>"""
		}
	}
	
	def output(line)
	{
		setParamsAndComments(line, 1) //sets the line.params and line.comment attributes
		if(oconvCount == 0 && line.params.trim() =~ /^[0-9]+$/)
		{
			oconvCount++
			line.xml = ""
		}
		else
		{
			setParamsAndComments(line, 2) //sets the line.params and line.comment attributes
			oconvCount++
			def (from, to) = line.params.trim().split(" ").toList()
			line.xml = """<output from="${esc(from)}" to="${esc(to)}"${commAttr(line)}/>"""
		}
	}
	
	def lemmaPresent(line)
	{
		line.xml = ""
		log.warning("LEMMA_PRESENT not used in Hunspell 1.2. Instead, use the st: field on dictionary entries or affix rules.${EOL}\t ${line.text}")
	}
	
	def pseudoroot(line)
	{
		line.function = "needAffix"
		log.warning("PSEUDOROOT depecated. Use NEEDAFFIX option instead.${EOL}\t${line.text}")
		setting1Flag(line)
	}
	
	def compoundSyllable(line)
	{
		setParamsAndComments(line, 2) //sets the line.params and line.comment attributes
		def (max, vowels) = line.params.trim().split(" ").toList()
		line.xml = """<compoundSyllable max="${esc(max)}" vowels="${esc(vowels)}"${commAttr(line)}/>"""
	}
	
	def syllableNum(line)
	{
		setParamsAndComments(line, 1) //sets the line.params and line.comment attributes
		def flags = toExpandedFlagList(line.params, line.text)
		line.xml = """<syllableNum flags="${esc(flags)}"${commAttr(line)}/>"""
	}
	
	def prefix(line)
	{
		affix("prefix", line)
	}
	
	def suffix(line)
	{
		affix("suffix", line)
	}
	
	def affix(String type, line)
	{
		setParamsAndComments(line, 3) //Check for a 3 param command first. If it's more than 3, recheck below for 3,4,5
		def params = line.params.split(/[\s\t]/)
		
		//TODO: check for affixCount[type] == 0 ?
		if(params.size() == 3 && params[0] != lastAffixFlag && params[1] =~ /^(Y|N)$/ && params[2] =~ /^[0-9]+$/)
		{
			setParamsAndComments(line, 3) //maximum of 3 params
			line.xml += """<${type} flag="${esc(toExpandedFlagList(params[0], line.text))}" cross="${params[1]=='Y'?'true':'false'}"${commAttr(line)}>"""
			affixCount[type]++
			lastAffixFlag = params[0]
			
			//Insert a blank line with a different level before the current line
			//This will trigger the </prefix> or </suffix> from previous affixes if needed.
			line.insertBefore = newAnnotatedLine()
			line.insertBefore.level = ["hunspell","affixFile","affixes"]
		}
		else
		{
			setParamsAndComments(line, 3, 4, 5) //3 params, 4 params, and 5 params are all valid
			affixCount[type]++
			def (sameFlag, remove, affix, where, morph) = line.params.split(/[\s\t]/, 5).toList()
			if(sameFlag != lastAffixFlag)
			{
				log.warning("Flag ${sameFlag} doesn't match affix header ${lastAffixFlag} on line:\r\n" + line.text)
				line.xml = ""
			}
			else
			{
				def (add, flags) = affix.split("/").toList()
				//line.xml = """<chars>${line.params}</chars>"""
				line.xml = "<rule"
				if(remove && remove != "0"){line.xml += """ remove="${esc(remove)}\""""}
				if(add && add != "0"){line.xml += """ add="${esc(add)}\""""}
				if(where && where != "."){line.xml += """ where="${esc(where)}\""""}
				if(flags){line.xml += """ combineFlags="${esc(toExpandedFlagList(flags, line.text))}\""""}
				if(morph){line.xml += """ morph="${esc(expandMorphemeAlias(morph))}\""""}
				line.xml += "${commAttr(line)}/>"
			}
			lastAffixFlag = params[0]
		}
	}
}
