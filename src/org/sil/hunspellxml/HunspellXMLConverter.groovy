package org.sil.hunspellxml

class HunspellXMLConverter
{
	//Utility class to validate and parse a HunspellXML file,
	//and export the finished Hunspell dictionary and plugins
	
	File xmlFile
	Log log = new Log()
	def exportOptions = [:]
	def exporter
	def validator
	def parser
	
	public HunspellXMLConverter(xmlFile)
	{
		if(!xmlFile instanceof File)
		{
			this.xmlFile = new File(xmlFile)
		}
		else{this.xmlFile = xmlFile}
	}
	
	public HunspellXMLConverter(xmlFile, Map exportOptions)
	{
		if(!xmlFile instanceof File)
		{
			this.xmlFile = new File(xmlFile)
		}
		else{this.xmlFile = xmlFile}
		this.exportOptions = exportOptions
	}
	
	public HunspellXMLConverter(xmlFile, Log log, Map exportOptions=[:])
	{
		this(xmlFile)
		this.log = log
		this.exportOptions = exportOptions
	}
	
	def convert()
	{
		//Get the file
		//Read in the text with the system default charset
		log.info("Reading the HunspellXML...")
		def xmlDoc = xmlFile.text
		log.info("Read.")
		
		//Get the actual charset from the file
		def charSet = HunspellXMLUtils.extractCharacterSet(xmlDoc)
		log.info("Character Set: " + charSet)
		def javaCharSet = HunspellXMLUtils.javaCharacterSet(charSet)
		log.info("(Java Character Set: " + javaCharSet + ")")
		
		//Read the text in again with the right encoding
		xmlDoc = xmlFile.newReader(javaCharSet).text
		
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
			new File(path.toString() + File.separator + "hunspellXML-${flagType}-${javaCharSet}.rng").withWriter("UTF-8"){writer->
				writer << hxv.relaxngXML
			}
			log.info("Exported RelaxNG schema: " + path.toString() + File.separator + "hunspellXML-${flagType}-${javaCharSet}.rng")
		}
		
		//Validate the HunspellXML document
		if(hxv.validate(xmlDoc))
		{
			log.info("Parsing the HunspellXML file...")
			def hxp = new HunspellXMLParser(path, flagType, log)
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
	}
}
