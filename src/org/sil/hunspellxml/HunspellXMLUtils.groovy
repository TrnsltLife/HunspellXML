package org.sil.hunspellxml

import java.util.List;

class HunspellXMLUtils
{
	
	public static extractFlagType(String xmlDoc)
	{
		def start = xmlDoc.indexOf("<flagType>")
		if(start >= 0)
		{
			def end = xmlDoc.indexOf("</flagType>")
			if(end >= 0)
			{
				return xmlDoc[(start+10)..(end-1)].trim()
			}
		}
		return "short"
	}
	
	public static extractCharacterSet(String xmlDoc)
	{
		def start = xmlDoc.indexOf("<characterSet>")
		if(start >= 0)
		{
			def end = xmlDoc.indexOf("</characterSet>")
			if(end >= 0)
			{
				return xmlDoc[(start+14)..(end-1)].trim()
			}
		}
		return "UTF-8"
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
	
	static List hunspellFlagsToList(String flagType, String flags)
	{
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
			for(def i=0; i<flags.size(); i+=2)
			{
				list << flags[i..i+1]
			}
			return list
		}
		else if(flagType == "num")
		{
			return flags.split(/,/).toList()
		}
	}
}
