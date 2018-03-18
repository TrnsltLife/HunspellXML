package org.sil.hunspellxml

import java.util.List;

class HunspellXMLUtils
{
	
	public static extractFlagType(String xmlDoc)
	{
		def start = xmlDoc.indexOf("<flagType")
		start = xmlDoc.indexOf(">", start) //find the > that ends <flagType comment="...">
		if(start >= 0)
		{
			def end = xmlDoc.indexOf("</flagType>", start)
			if(end >= 0)
			{
				//return xmlDoc[(start+10)..(end-1)].trim()
				return xmlDoc[(start+1)..(end-1)].trim()
			}
		}
		return "short"
	}
	
	public static extractCharacterSet(String xmlDoc)
	{
		def start = xmlDoc.indexOf("<characterSet")
		start = xmlDoc.indexOf(">", start) //find the > that ends <characterSet comment="...">
		if(start >= 0)
		{
			def end = xmlDoc.indexOf("</characterSet>")
			if(end >= 0)
			{
				//return xmlDoc[(start+14)..(end-1)].trim()
				return xmlDoc[(start+1)..(end-1)].trim()
			}
		}
		return "UTF-8"
	}
	
	public static String extractHunspellCharacterSet(String affFile, String defaultCharSet="UTF-8")
	{
		def text = (new File(affFile)).getText("UTF-8")
		if(text.startsWith("\uFEFF")){text -= "\uFEFF"} //remove UTF-8 BOM
		def lines = text.split("\r?\n").toList()
		for(line in lines)
		{
			if(line.startsWith("SET"))
			{
				line = line.replaceAll(/^SET\s+/, "")
				//Get the first segment, in case there is a comment on this line
				//SET UTF-8 #The most compatible across systems
				//would become
				//["UTF-8", "#The most compatible across systems"]
				//And we'll keep just "UTF-8"
				def segments = line.split(/\s/, 2).toList()
				line = segments[0]
				return line.trim()
			}
			else if(line.startsWith("FLAG UTF-8"))
			{
				//When the FLAG is set to UTF-8, it forces the character set to be UTF-8
				return "UTF-8"
			}
		}
		return defaultCharSet
	}
	
	public static javaCharacterSet(String characterSet)
	{
		if(characterSet == "ISO8859-11"){return "x-iso-8859-11"}
		else if(characterSet.startsWith("ISO8859")){return characterSet.replaceAll(/^(ISO8859)/, "ISO-8859")}
		else if(characterSet == "microsoft-cp1251"){return "windows-1251"}
		else{return characterSet}
	}
	
	public static myThesCharacterSet(String characterSet)
	{
		if(characterSet == "microsoft-cp1251"){return "CP-1251"}
		else{return characterSet}
	}
	
	static List hunspellFlagsToList(Log log, String flagType, String flags)
	{
		flags = flags.trim()
		if(flagType == "short" || flagType == "UTF-8")
		{
			def list = []
			for(def i=0; i<flags.size(); i++)
			{
				list << flags[i]
			}
			return list
		}
		else if(flagType == "long")
		{
			def list = []
			if(flags.size() % 2 != 0)
			{
				log.error("Hunspell flag list ${flags} is invalid for flagType: ${flagType}")
			}
			for(def i=0; i+1<flags.size(); i+=2)
			{
				list << flags[i..i+1]
			}
			return list
		}
		else if(flagType == "num")
		{
			def list = flags.split(/,/).toList()
			return list
		}
	}
}
