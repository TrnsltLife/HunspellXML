package org.sil.hunspellxml

class HunspellXMLConverter
{
	//Utility class to validate and parse a HunspellXML file,
	//and export the finished Hunspell dictionary and plugins
	
	File xmlFile
	Log log = new Log(Log.INFO)
	def exportOptions = [:]
	def exporter
	def validator
	def parser
	def tester
	static final String FS = File.separator
	
	public HunspellXMLConverter(xmlFile)
	{
		if(!xmlFile instanceof File)
		{
			this.xmlFile = new File(xmlFile)
		}
		else{this.xmlFile = xmlFile}
		this.exportOptions = HunspellXMLExporter.defaultOptions
		if(exportOptions.containsKey("logLevel")) {log.logLevel = exportOptions.logLevel}
	}
	
	public HunspellXMLConverter(xmlFile, Map exportOptions)
	{
		if(!xmlFile instanceof File)
		{
			this.xmlFile = new File(xmlFile)
		}
		else{this.xmlFile = xmlFile}
		this.exportOptions = exportOptions
		if(exportOptions.containsKey("logLevel")) {log.logLevel = exportOptions.logLevel}
	}
	
	public HunspellXMLConverter(xmlFile, Log log, Map exportOptions=HunspellXMLExporter.defaultOptions)
	{
		this(xmlFile)
		this.log = log
		this.exportOptions = exportOptions
		if(exportOptions.containsKey("logLevel")) {log.logLevel = exportOptions.logLevel}
	}
	
	def convert()
	{
		//Get the file
		//Read in the text with the system default charset
		log.info("Reading the HunspellXML...")
		def xmlDoc = xmlFile.getText("ISO-8859-1")
		log.info("Read.")
		
		//Get the actual charset from the file
		def charSet = HunspellXMLUtils.extractCharacterSet(xmlDoc)
		log.info("Character Set: " + charSet)
		def javaCharSet = HunspellXMLUtils.javaCharacterSet(charSet)
		log.info("(Java Character Set: " + javaCharSet + ")")
		
		//Read the text in again with the right encoding
		xmlDoc = xmlFile.newReader(javaCharSet).text
		
		//Check for UTF-8 BOM, and if it exists, remove it.
		if(javaCharSet == "UTF-8" && xmlDoc.startsWith("\uFEFF"))
		{
			xmlDoc -= "\uFEFF"
		}
		
		//Find the path of the file. This will be the root for the exported data.
		def path = xmlFile.getParentFile()
		
		//Extract the flagType from the document
		def flagType = HunspellXMLUtils.extractFlagType(xmlDoc)
		log.info("Hunspell flag type: " + flagType)
		
		//Create the validator
		log.info("Validating the HunspellXML file...")
		def hxv = new HunspellXMLValidator(javaCharSet, flagType, log)
		
		//Export the relaxNG schema for this character set and flag combination
		hxv.configureValidator()
		if(exportOptions.relaxNG)
		{
			new File(path.toString() + FS + "hunspellXML-${flagType}-${javaCharSet}.rng").withWriter("UTF-8"){writer->
				writer << hxv.relaxngXML
			}
			log.info("Exported RelaxNG schema: " + path.toString() + FS + "hunspellXML-${flagType}-${javaCharSet}.rng")
		}
		
		//Validate the HunspellXML document and export files
		if(hxv.validate(xmlDoc))
		{
			log.info("Parsing the HunspellXML file...")
			def hxp = new HunspellXMLParser(path, flagType, log, exportOptions)
			hxp.parseText(xmlDoc)
			parser = hxp
			
			log.info("Exporting the XML file...")
			def hxe = new HunspellXMLExporter(hxp.data, log, exportOptions)
			hxe.export()
			exporter = hxe
			log.info("Finished!")
			log.info("Find your files in: " + hxe.baseDir)
		}
		validator = hxv
		
		//Test the test files
		if(exportOptions.runTests && exporter?.dicFile)
		{
			tester = new HunspellTester(exporter.dicFile, javaCharSet, /*destroy*/ true)
			tester.checkTestFiles(exporter)
		}
	}
}
