package org.sil.hunspellxml

class HunspellXMLCLI 
{
	public static final int ERR_NO_ARGS = 1
	public static final int ERR_INVALID_FLAGS = 2
	public static final int ERR_INVALID_FLAG_TYPE = 3
	public static final int ERR_NO_AFF_REQUIRES_DC_DF = 4
	public static final int ERR_TESTS_FAILED = 5
	
	public static void main(String[] args)
	{
		if(args.size() < 1)
		{
			printUsage()
			System.exit(ERR_NO_ARGS)
		}
		
		if(args[0] == "-h" || args[0] == "-?")
		{
			printUsage()
			System.exit(0)
		}
		
		def file = args[-1] //last argument
		
		def ftc = file.toLowerCase()
		
		
		if(ftc.endsWith(".xml"))
		{
			xmlToHunspell(args)
		}
		else if(ftc.endsWith(".aff") || ftc.endsWith(".dic") || ftc.endsWith(".good") || ftc.endsWith(".wrong") || ftc.endsWith(".dat"))
		{
			hunspellToXML(args)
		}
	}
	
	public static void xmlToHunspell(String[] args)
	{
		def file = args[-1] //last argument
		def options = new HashMap()
		options.putAll(HunspellXMLExporter.defaultOptions)
		def exportFlagMap = [hs:"hunspell",
			ts:"tests",
			th:"thesaurus",
			rm:"readme",
			lc:"license",
			ff:"firefox",
			lo:"libreOffice",
			op:"opera"]		
		boolean invalidFlags = false
		def exportOptions = []

		//process flags
		for(int i=0; i<args.size()-1; i++)
		{
			def arg = args[i]
			if(arg.startsWith("-"))
			{
				arg -= "-"
				if(exportFlagMap[arg])
				{
					exportOptions << arg
				}
				else if(arg == "h" || arg == "?")
				{
					printUsage()
					System.exit(0)
				}
				else if(arg.startsWith("l="))
				{
					//log level
					arg -= "l="
					arg = arg.toLowerCase()
					switch(arg)
					{
						case "debug": 	options.logLevel = Log.DEBUG; break;
						case "info": 	options.logLevel = Log.INFO; break;
						case "warning":
						case "warn": 	options.logLevel = Log.WARNING; break;
						case "error":
						case "err": 	options.logLevel = Log.ERROR; break;
						case "none": 	options.logLevel = Log.NONE; break;
						default: 		options.logLevel = Log.WARNING;
					}
				}
				else if(arg.startsWith("o="))
				{
					//output filename
					arg -= "o="
					if(arg.startsWith('"') && arg.endsWith('"'))
					{
						arg = arg.replaceAll(/^"/, "")
						arg = arg.replaceAll(/"$/, "")
					}
					//Get the absolute path to set the output directory relative to the current working directory.
					//If we don't make it absolute, HunspellXMLConverter outputs relative to the input .xml file.
					File f = new File(arg).getAbsoluteFile()
					options.hunspellFileName = f.getName()
					options.customPath = f.getParent()
				}
				else if(arg == "rng")
				{
					options.relaxNG = true
				}
				else if(arg == "rt")
				{
					options.runTests = true
				}
				else if(arg == "s")
				{
					options.suppressMetadata = true
					options.suppressAutoBlankLines = true
					options.suppressAutoComments = true
					options.suppressMyBlankLines = true
					options.suppressMyComments = true
				}
				else if(arg == "sd")
				{
					options.suppressMetadata = true
				}
				else if(arg == "sa")
				{
					options.suppressAutoBlankLines = true
					options.suppressAutoComments = true
				}
				else if(arg == "sm")
				{
					options.suppressMyBlankLines = true
					options.suppressMyComments = true
				}
				else if(arg == "sab")
				{
					options.suppressAutoBlankLines = true
				}
				else if(arg == "sac")
				{
					options.suppressAutoComments = true
				}
				else if(arg == "smb")
				{
					options.suppressMyBlankLines = true
				}
				else if(arg == "smc")
				{
					options.suppressMyComments = true
				}
				else
				{
					println("Invalid flag: -" + arg)
					invalidFlags = true
				}
			}
			else
			{
				println("Invalid flag: -" + arg)
				invalidFlags = true
			}
		}
		
		if(invalidFlags)
		{
			printUsage()
			System.exit(ERR_INVALID_FLAGS)
		}
		
		//Set all export options to false, except those specified on the command line
		//If no export options are specified, default to exporting all files per HunspellXMLExporter.defaultOptions
		if(exportOptions)
		{
			for(key in exportFlagMap.keySet())
			{
				options[exportFlagMap[key]] = false
			}
			for(key in exportOptions)
			{
				options[exportFlagMap[key]] = true
			}
		}
		
		if(new File(file).exists())
		{
			def hxc = new HunspellXMLConverter(new File(file), options)
			hxc.log.debug("Options set from command line: " + options)
			hxc.convert()
			if(hxc.exporter.options.runTests)
			{
				if(!hxc.exporter.options.goodPassed ||
				   !hxc.exporter.options.badPassed)
				{
					println("Tests failed")
					System.exit(ERR_TESTS_FAILED)
				}
				else
				{
					println("Tests passed")
				}
			}
		}
		else
		{
			println("File not found: " + file)
		}
	}
	
	public static void hunspellToXML(String[] args)
	{
		def file = args[-1] //last argument
		def options = new HashMap()
		options.putAll(HunspellConverter.defaultOptions)
		def processFlagMap = [aff:"skipAff",
			dat:"skipDat",
			dic:"skipDic",
			tst:"skipTests"]
		boolean invalidFlags = false
		def processOptions = []
		
		//process flags
		for(int i=0; i<args.size()-1; i++)
		{
			def arg = args[i]
			if(arg.startsWith("-"))
			{
				arg -= "-"
				if(processFlagMap[arg])
				{
					processOptions << arg
				}
				else if(arg.startsWith("dat="))
				{
					processOptions << "dat"
					arg -= "dat="
					options.datFile = arg
				}
				else if(arg.startsWith("dc="))
				{
					arg -= "dc="
					if(!["short", "long", "UTF-8", "num"].contains(arg))
					{
						println("Option -df must be one of [short, long, UTF-8, num].")
						System.exit(ERR_INVALID_FLAG_TYPE)
					}
					options.defaultCharSet = arg
				}
				else if(arg.startsWith("df="))
				{
					arg -= "df="
					options.defaultFlagType = arg
				}
				else if(arg.startsWith("dl="))
				{
					arg -= "dl="
					options.defaultLangCode = arg
				}
				else if(arg == "h" || arg == "?")
				{
					printUsage()
					System.exit(0)
				}
				else if(arg.startsWith("l="))
				{
					//log level
					arg -= "l="
					arg = arg.toLowerCase()
					switch(arg)
					{
						case "debug": 	options.logLevel = Log.DEBUG; break;
						case "info": 	options.logLevel = Log.INFO; break;
						case "warning":
						case "warn": 	options.logLevel = Log.WARNING; break;
						case "error":
						case "err": 	options.logLevel = Log.ERROR; break;
						case "none": 	options.logLevel = Log.NONE; break;
						default: 		options.logLevel = Log.WARNING;
					}
				}
				else if(arg.startsWith("o="))
				{
					//output filename
					arg -= "o="
					if(arg.startsWith('"') && arg.endsWith('"'))
					{
						arg = arg.replaceAll(/^"/, "")
						arg = arg.replaceAll(/"$/, "")
					}
					options.outputFileName = arg
				}
				else if(arg.startsWith("oc="))
				{
					arg -= "oc="
					options.charSet = arg
				}
				/*
				else if(arg.startsWith("p="))
				{
					//custom export path
					arg -= "p="
					if(arg.startsWith('"') && arg.endsWith('"'))
					{
						arg = arg.replaceAll(/^"/, "")
						arg = arg.replaceAll(/"$/, "")
					}
					options.customPath = arg
				}
				*/
				else if(arg == "s")
				{
					options.suppressMyBlankLines = true
					options.suppressMyComments = true
				}
				else if(arg == "sm")
				{
					options.suppressMyBlankLines = true
					options.suppressMyComments = true
				}
				else if(arg == "smb")
				{
					options.suppressMyBlankLines = true
				}
				else if(arg == "smc")
				{
					options.suppressMyComments = true
				}
				else
				{
					println("Invalid flag: -" + arg)
					invalidFlags = true
				}
			}
			else
			{
				println("Invalid flag: -" + arg)
				invalidFlags = true
			}
		}
		
		if(invalidFlags)
		{
			printUsage()
			System.exit(ERR_INVALID_FLAGS)
		}
		
		//Set skipAff, skipDic, and skipTests to true, if any process options were specified (-aff, -dic, -tst)
		//Set them back to false if they were specified on the command line.
		if(processOptions)
		{
			for(key in processFlagMap.keySet())
			{
				options[processFlagMap[key]] = true //Indicate which files to skip, e.g. skipAff=true, skipDic=true, or skipTests=true
			}
			for(key in processOptions)
			{
				options[processFlagMap[key]] = false //Don't skip the files specified on the command line with -aff, -dic, -tst, e.g. skipAff=false, etc.
			}
			
			//If skipAff is true, -dc and -df need to have been used
			if(options.skipAff && !options.skipDic)
			{
				if(!options.defaultCharSet || !options.defaultFlagType)
				{
					println("If -dic is specified but -aff is not specified, you must specify the -dc= and -df= options.")
					System.exit(ERR_NO_AFF_REQUIRES_DC_DF)
				}
			}
		}
		
		if(new File(file).exists())
		{
			
			def hc = new HunspellConverter(file, options)
			hc.log.debug("Options set from command line: " + options)
			hc.convert()
		}
		else
		{
			println("File not found: " + file)
		}
	}

	
	public static printUsage()
	{
println(
"""Usage:
*************************************
*Print this HunspellXML help message*
*************************************
HunspellXML -h
*or*
HunspellXML -?

*************************************
*Convert XML file to Hunspell format*
*************************************
HunspellXML [Options] [Output Suppression] [Export Options] hunspellXML_input_file.xml

     [Optional Flags]
-o=file_base       Base filename (no extension) for creating Hunspell dictionary, e.g. path/to/en_US
-l=level           Log level: none, error, warning, info, debug
-rng               Create RelaxNG schema for HunspellXML
-rt                Run dictionary tests

     [Output Suppression]
-s                 Suppress all extra output
-sa                Suppress automatic comments and blank lines
-sab               Suppress automatic blank lines
-sac               Suppress automatic comments
-sd                Suppress metadata output
-sm                Suppress my comments and blank lines
-smb               Suppress my blank lines
-smc               Suppress my comments

     [Export Options: If none are specified, all will be created.]
-hs                Create Hunspell dictionary files
-ts                Create Hunspell test files
-th                Create MyThes thesaurus files
-rm                Create Readme file
-lc                Create License file
-ff                Create Firefox dictionary plugin
-lo                Create LibreOffice dictionary plugin
-op                Create Opera dictionary plugin

*************************************
*Convert Hunspell format to XML file*
*************************************
HunspellXML [Optional Flags] [Output Suppression] [Processing Options] hunspell_input_file.aff
*or*
HunspellXML [Optional Flags] [Output Suppression] [Processing Options] hunspell_input_file.dic

     [Optional Flags]
-o=filename.xml    Filename for exporting HunspellXML file, e.g. path/to/en_US.xml
-oc=output-charset Convert output to this Hunspell character set
-dc=charset        Default charset for reading and exporting if none is 
                      specified in the .aff file.
-df=flag-type      Default flag type if none is specified in the .aff file.
                      [short, long, UTF-8, num]
-dl=lang_code      Default language code if none is specified in the .aff file, e.g. en_US, fr_FR
-l=level           Log level: none, error, warning, info, debug

     [Output Suppression]
-s                 Suppress all extra output
-sm                Suppress my comments and blank lines
-smb               Suppress my blank lines
-smc               Suppress my comments

     [Processing Options: If none are specified, all will be processed.]
-aff               Process the Hunspell .aff file
-dic               Process the Hunspell .dic file
-tst               Process the Hunspell .good and .wrong files
-dat               Process the MyThes .dat file
-dat=thes_file.dat Process the MyThes .dat file named thes_file.dat. This allows the
                      .dat file to start with a different name than the .aff/.dic files.
""")
	}

}
