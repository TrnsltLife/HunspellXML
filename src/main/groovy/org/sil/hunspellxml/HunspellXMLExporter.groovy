package org.sil.hunspellxml

import java.util.Map;

class HunspellXMLExporter
{
	//Export in-memory dictionary & thesaurus files to dictionary 
	//and thesaurus files and plugins in various formats
	
	Log log = new Log()
	
	String baseDir
	HunspellXMLData data
	static final String FS = File.separator
	
	String filename = ""
	
	String affFile = ""
	String dicFile = ""
	boolean exportThesaurus = false
	String datFile = ""
	String idxFile = ""
	String licenseFile = ""
	String readmeFile = ""
	String goodFile = ""
	String badFile = ""
	
	static final defaultOptions = Collections.unmodifiableMap([hunspell:true,
		tests:true,
		thesaurus:true,
		license:true,
		readme:true,
		libreOffice:true,
		firefox:true,
		opera:true,
		relaxNG:false,
		runTests:true,
		customPath:"",
		hunspellFileName:"",
		logLevel:Log.WARNING,
		sortDictionaryData:true,
		suppressMetadata:false,
		suppressAutoBlankLines:false,
		suppressMyBlankLines:false,
		suppressAutoComments:false,
		suppressMyComments:false,
		goodPassed:true,
		goodResults:[],
		badPassed:true,
		badResults:[]])
	
	def options = [:]
		
	public HunspellXMLExporter(HunspellXMLData data, Log log, Map opts)
	{
		this(data, log)
		this.log = log
		options.putAll(opts)
	}
		
	public HunspellXMLExporter(HunspellXMLData data, Log log)
	{
		this(data)
		this.log = log
	}

	public HunspellXMLExporter(HunspellXMLData data, Map opts)
	{
		this(data)
		options.putAll(opts)
	}

	public HunspellXMLExporter(HunspellXMLData data)
	{
		this.data = data
		options.putAll(HunspellXMLExporter.defaultOptions)
	}
	
	def export()
	{
		filename = configureFilename(data, options.hunspellFileName)
		baseDir = configureBaseDir(data, filename, options.customPath)
		
		log.info("Exporting to ${baseDir}")
		new File(baseDir).mkdirs()
		
		//Populate default values if these optional elements are empty
		if(!data.metadata.languageName){data.metadata.languageName = data.metadata.languageCode}
		if(!data.metadata.dictionaryName){data.metadata.dictionaryName = data.metadata.languageName}
		
		if(options.thesaurus)
		{
			createThesaurusFiles()
		}
		
		if(options.hunspell)
		{
			createAffixFile()
			createDictionaryFile()
			if(options.tests)
			{
				createTestFiles()
			}
		
			if(options.license)
			{
				createLicenseFile()
			}
			if(options.readme)
			{
				createReadmeFile()
			}
		}
		
		if(options.firefox)
		{
			if(!options.readme)
			{
				log.error("Can't create Firefox plugin without creating the readme file. Check your export options.")
			}
			else
			{
				createFirefoxPlugin()
			}
		}
		if(options.libreOffice)
		{
			if(!options.readme)
			{
				log.error("Can't create LibreOffice plugin without creating the readme file. Check your export options.")
			}
			else
			{
				createLibreOfficePlugin()
			}
		}
		if(options.opera)
		{
			if(!options.license)
			{
				log.error("Can't create Opera plugin without creating the license file. Check your export options.")
			}
			else
			{
				createOperaPlugin()
			}
		}

	}
	
	static String configureFilename(HunspellXMLData data, customFileName)
	{
		if(customFileName){return customFileName}
		if(data.metadata.filename){return data.metadata.filename}
		return data.metadata.languageCode
	}
	
	static String configureBaseDir(HunspellXMLData data, String filename, String customPath="")
	{
		def baseDir = ""
		if(!customPath && data.metadata.filepath)
		{
			customPath = data.metadata.filepath
		}
		/*
		if(customPath)
		{
			if(!(new File(customPath).isAbsolute())) //path is relative
			{
				baseDir = data.basePath.getCanonicalPath() + FS
			}
			baseDir += customPath
		}
		else
		{
			baseDir = data.basePath.getCanonicalPath() + FS + filename
		}
		*/
		if(customPath)
		{
			baseDir = customPath
		}
		else
		{
			baseDir = filename
		}
		baseDir = new File(baseDir).getCanonicalPath()
		return baseDir
	}
		
	def createAffixFile()
	{
		affFile = baseDir + FS + filename + ".aff"
		def file = new File(affFile)
		file.withWriter(data.metadata.javaEncoding){writer->
			writer << data.affFile
		}
		log.info("Affix file created.")
	}
	
	def createDictionaryFile()
	{
		dicFile = baseDir + FS + filename + ".dic"
		def file = new File(dicFile)
		file.withWriter(data.metadata.javaEncoding){writer->
			writer << data.dicFile
		}
		log.info("Dictionary file created.")
	}
	
	def createThesaurusFiles()
	{
		if(data.idxCount)
		{
			exportThesaurus = true
			
			datFile = baseDir + FS + "thes_" + filename + ".dat"
			def file = new File(datFile)
			file.withWriter(data.metadata.javaEncoding){writer->
				writer << data.datFile
			}
			
			idxFile = baseDir + FS + "thes_" + filename + ".idx"
			file = new File(idxFile)
			file.withWriter(data.metadata.javaEncoding){writer->
				writer << data.idxFile
			}
			log.info("Thesaurus files created.")
		}
	}
	
	def createTestFiles()
	{
		goodFile = baseDir + FS + filename + ".good" //"_good.txt"
		def file = new File(goodFile)
		file.withWriter(data.metadata.javaEncoding){writer->
			writer << data.goodTestFile
		}
		
		badFile = baseDir + FS + filename + ".wrong" //"_bad.txt"
		file = new File(badFile)
		file.withWriter(data.metadata.javaEncoding){writer->
			writer << data.badTestFile
		}
		log.info("Test files created.")
	}
	
	def createLicenseFile()
	{
		licenseFile = baseDir + FS + "license.txt"
		def file = new File(licenseFile)
		file.withWriter(data.metadata.javaEncoding){writer->
			writer << data.metadata.license
		}
		log.info("License file created.")
	}
	
	def createReadmeFile()
	{
		readmeFile = baseDir + FS + "README_" + filename + ".txt"
		def file = new File(readmeFile)
		file.withWriter(data.metadata.javaEncoding){writer->
			writer << data.metadata.readme
		}
		log.info("Readme file created.")
	}
	
	
	def createLibreOfficePlugin()
	{
		def allParams = true
		def reqParams = ["languageCode", "languageName", "dictionaryName", "description", "readme"]
		for(param in reqParams)
		{
			if(!data.metadata.containsKey(param))
			{
				allParams = false
			}
		}
		if(!allParams)
		{
			throw new Exception("Not all required metadata parameters are present to create the LibreOffice plugin. Required parameters are: " + reqParams.join(", ") + ".")
		}
		
		def langCode = data.metadata.languageCode
		def langCodeOnly = langCode.split(/[-_]/).toList()[0]
		def langCodeDash = langCode.replaceAll(/_/, "-")
		def localeList = data.metadata.localeList
		if(!localeList)
		{
			localeList = langCodeDash
			if(!langCodeOnly == langCodeDash)
			{
				localeList += " " + langCodeOnly
			}
		}
		def localeListDash = localeList.replaceAll(/_/, "-")
		
		def tempDir = baseDir + FS + "temp_libre_" + langCode
		
		new File(tempDir).mkdirs()
		new File(tempDir + FS + "META-INF").mkdirs()
		new File(tempDir + FS + "dictionaries").mkdirs()
		
		FileUtils.copy(dicFile, tempDir + FS + "dictionaries" + FS + langCode + ".dic")
		FileUtils.copy(affFile, tempDir + FS + "dictionaries" + FS + langCode + ".aff")
		FileUtils.copy(readmeFile, tempDir + FS + "dictionaries" + FS + "README_" + langCode + ".txt")
		
		if(exportThesaurus)
		{
			FileUtils.copy(datFile, tempDir + FS + "dictionaries" + FS + "thes_" + langCode + ".dat")
			FileUtils.copy(idxFile, tempDir + FS + "dictionaries" + FS + "thes_" + langCode + ".idx")
			FileUtils.copy(readmeFile, tempDir + FS + "dictionaries" + FS + "README_thes_" + langCode + ".txt")
		}
		
		//Output package-description.txt
		new File(tempDir + FS + "package-description.txt").withWriter("UTF-8"){writer->
			writer << data.metadata.dictionaryName + "\r\n\r\n" + data.metadata.description
		}
			
//Output description.xml
new File(tempDir + FS + "description.xml").withWriter("UTF-8"){writer->
writer << """<?xml version="1.0" encoding="UTF-8"?>
<description xmlns="http://openoffice.org/extensions/description/2006" xmlns:d="http://openoffice.org/extensions/description/2006"  xmlns:xlink="http://www.w3.org/1999/xlink">
<version value="2010.10.22" />
<identifier value="org.libreoffice.${langCode}.hunspell.dictionaries" />
<display-name>
<name lang="${langCode}">${data.metadata.dictionaryName}</name>
</display-name>
<platform value="all" />
<dependencies>
<OpenOffice.org-minimal-version value="3.0" d:name="OpenOffice.org 3.0" />
</dependencies>
</description>
"""
			}
			
			
//Output dictionaries.xcu
new File(tempDir + FS + "dictionaries.xcu").withWriter("UTF-8"){writer->
writer << """<?xml version="1.0" encoding="UTF-8"?>
<oor:component-data xmlns:oor="http://openoffice.org/2001/registry" xmlns:xs="http://www.w3.org/2001/XMLSchema" oor:name="Linguistic" oor:package="org.openoffice.Office">
 <node oor:name="ServiceManager">
	<node oor:name="Dictionaries">
		<node oor:name="HunSpellDic_${langCode}" oor:op="fuse">
			<prop oor:name="Locations" oor:type="oor:string-list">
				<value>%origin%/dictionaries/${langCode}.aff %origin%/dictionaries/${langCode}.dic</value>
			</prop>
			<prop oor:name="Format" oor:type="xs:string">
				<value>DICT_SPELL</value>
			</prop>
			<prop oor:name="Locales" oor:type="oor:string-list">
				<value>${localeListDash}</value>
			</prop>
		</node>
		"""
if(exportThesaurus)
{
writer << """
        <node oor:name="ThesDic_${langCode}}" oor:op="fuse">
            <prop oor:name="Locations" oor:type="oor:string-list">
                <value>%origin%/dictionaries/thes_${langCode}.dat %origin%/dictionaries/thes_${langCode}.idx</value>
            </prop>
            <prop oor:name="Format" oor:type="xs:string">
                <value>DICT_THES</value>
            </prop>
            <prop oor:name="Locales" oor:type="oor:string-list">
                <value>${localeListDash}</value>
            </prop>
        </node>
"""
}
writer << """	</node>
 </node>
</oor:component-data>
"""
}
			
			
//Output manifest.xml
new File(tempDir + FS + "META-INF/manifest.xml").withWriter("UTF-8"){writer->
writer << """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE manifest:manifest PUBLIC "-//OpenOffice.org//DTD Manifest 1.0//EN" "Manifest.dtd">
<manifest:manifest xmlns:manifest="http://openoffice.org/2001/manifest">
	<manifest:file-entry manifest:media-type="application/vnd.sun.star.configuration-data" manifest:full-path="dictionaries.xcu"/>
	<manifest:file-entry manifest:full-path="package-description.txt" manifest:media-type="application/vnd.sun.star.package-bundle-description"/>
</manifest:manifest>
"""
}

			new File(baseDir + FS + "LibreOffice").mkdirs()
			FileUtils.zip(tempDir, baseDir + FS + "LibreOffice" + FS + "dict-" + langCodeDash + ".oxt")
			FileUtils.deleteDirectory(tempDir)

			log.info("LibreOffice plugin file created.")
	}
	
	
	
	def createFirefoxPlugin()
	{
		def allParams = true
		def reqParams = ["languageCode", "languageName", "dictionaryName", "readme", "version", "creator", "contributors", "webpage", "shortDescription"]
		def optParams = ["firefoxVersion.min","firefoxVersion.max", "thunderbirdVersion.min", "thunderbirdVersion.max"]
		for(param in reqParams)
		{
			if(!data.metadata.containsKey(param))
			{
				allParams = false
			}
		}
		if(!allParams)
		{
			throw new Exception("Not all required metadata parameters are present to create the Firefox plugin. Required parameters are: " + reqParams.join(", ") + ".")
		}
		
		def langCode = data.metadata.languageCode
		def langCodeOnly = langCode.split(/[-_]/).toList()[0]
		def langCodeDash = langCode.replaceAll(/_/, "-")
		

			def tempDir = baseDir + FS + "temp_firefox_" + langCode
			
			new File(tempDir).mkdirs()
			new File(tempDir + FS + "dictionaries").mkdirs()
			
			FileUtils.copy(dicFile, tempDir + FS + "dictionaries" + FS + langCodeDash + ".dic")
			FileUtils.copy(affFile, tempDir + FS + "dictionaries" + FS + langCodeDash + ".aff")
			FileUtils.copy(readmeFile, tempDir + FS + "README-" + langCodeDash + ".txt")
			
			//Output .js file
			new File(tempDir + FS + "install.js").withWriter("UTF-8"){writer->
writer << """var err = initInstall("${data.metadata.dictionaryName}", "${langCodeDash}@dictionaries.addons.mozilla.org", "1.0");
if (err != SUCCESS)
    cancelInstall();

var fProgram = getFolder("Program");
err = addDirectory("", "${langCodeDash}@dictionaries.addons.mozilla.org",
		   "dictionaries", fProgram, "dictionaries", true);
if (err != SUCCESS)
    cancelInstall();

performInstall();
"""
			}
			
			
			
			//Output .rdf file
			new File(tempDir + FS + "install.rdf").withWriter("UTF-8"){writer->
				writer << """<?xml version="1.0"?>

<RDF xmlns="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
     xmlns:em="http://www.mozilla.org/2004/em-rdf#">
  <Description about="urn:mozilla:install-manifest">
    <em:id>${langCodeDash}@dictionaries.addons.mozilla.org</em:id>
	<em:unpack>true</em:unpack>
    <em:version>${data.metadata.version}</em:version>
    <em:creator>${data.metadata.creator}</em:creator>
"""
				
				data.metadata.contributors.each{contributor->
					writer << """    <em:contributor>${contributor}</em:contributor>\r\n"""
				}

				writer << """    <em:homepageURL>${data.metadata.webpage}</em:homepageURL>
	<!-- Firefox -->
    <em:targetApplication>
      <Description>
        <em:id>{ec8030f7-c20a-464f-9b0e-13a3a9e97384}</em:id>
	    <em:minVersion>${data.metadata.firefoxVersion.min ?: "0.0"}</em:minVersion>
	    <em:maxVersion>${data.metadata.firefoxVersion.max ?: "100.0"}</em:maxVersion>
      </Description>
    </em:targetApplication>
    <!-- Thunderbird -->
    <em:targetApplication>
      <Description>
        <em:id>{3550f703-e582-4d05-9a08-453d09bdfdc6}</em:id>
	    <em:minVersion>${data.metadata.thunderbirdVersion.min ?: "0.0"}</em:minVersion>
	    <em:maxVersion>${data.metadata.thunderbirdVersion.max ?: "100.0"}</em:maxVersion>
      </Description>
    </em:targetApplication>
    <em:name>${data.metadata.dictionaryName}</em:name>
    <em:description>${data.metadata.shortDescriptions}</em:description>
  </Description>
</RDF>
"""
			}

			new File(baseDir + FS + "Firefox").mkdirs()
			def zipFile = baseDir + FS + "Firefox" + FS + data.metadata.languageName.replaceAll(/ /, "_") + "-" + data.metadata.version + "-fx+tb.xpi"
			FileUtils.zip(tempDir, zipFile)
			FileUtils.deleteDirectory(tempDir)
			
			log.info("Firefox plugin file created.")
	}
	
	
	
	
	def createOperaPlugin()
	{
		def allParams = true
		def reqParams = ["languageCode", "languageName", "version", "license"]
		for(param in reqParams)
		{
			if(!data.metadata.containsKey(param))
			{
				allParams = false
			}
		}
		if(!allParams)
		{
			throw new Exception("Not all required metadata parameters are present to create the Opera plugin. Required parameters are: " + reqParams.join(", ") + ".")
		}
		
		def langCode = data.metadata.languageCode
		def langCodeOnly = langCode.split(/[-_]/).toList()[0]
		def langCodeDash = langCode.replaceAll(/_/, "-")
		

			def tempDir = baseDir + FS + "temp_opera_" + langCode
			
			new File(tempDir).mkdirs()

			FileUtils.copy(dicFile, tempDir + FS + langCode + ".dic")
			FileUtils.copy(affFile, tempDir + FS + langCode + ".aff")
			FileUtils.copy(licenseFile, tempDir + FS + "license.txt")
			
			//Output .ini file
			new File(tempDir + FS + "${langCode}.ini").withWriter("UTF-8"){writer->
writer << """Opera Preferences version 2.1
; Do not edit this file while Opera is running
; This file is stored in UTF-8 encoding

[Dictionary]
Version=${data.metadata.version}
Name=${data.metadata.languageName}
"""
			}


			new File(baseDir + FS + "Opera").mkdirs()
			FileUtils.zip(tempDir, baseDir + FS + "Opera" + FS + langCode + ".zip")
			FileUtils.deleteDirectory(tempDir)
			
			log.info("Opera plugin file created.")
	}
}