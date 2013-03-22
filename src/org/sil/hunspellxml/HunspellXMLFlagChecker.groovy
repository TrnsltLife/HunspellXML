package org.sil.hunspellxml

import java.util.List;

class HunspellXMLFlagChecker
{
	//Use this to validate use of flags, and warn user of errors
	//in their Hunspell dictionary. Some of the warnings are
	//undocumented behaviors of Hunspell.
	
	def complexPrefixes = false
	def wordFlags = []
	def affixFlagMap = [:]
	def affixTargetMap = [:]
	def combineFlags = []
	def specialFlags = []
	def specialFlagsMap = [:]
	def specialFlagsReverseMap = [:]
	def flagMap = [:]
	def checkMap = [:]
	def missingFlagPath = HunspellXMLFlagPath.createMissing()
	def terminalFlagPath = HunspellXMLFlagPath.createTerminal()
	
	String flagType
	
	Log log = new Log()
	
	public HunspellXMLFlagChecker(String flagType, Log log)
	{
		this.flagType = flagType
		this.log = log
	}
	
	public HunspellXMLFlagChecker(String flagType)
	{
		this.flagType = flagType
	}
	
	List flagsToList(String flags)
	{
		return flags.trim().split(/\s/).toList()
	}
	
	def addSpecialFlag(String flag, String hunspellName)
	{
		specialFlagsMap[flag] = hunspellName
		specialFlagsReverseMap[hunspellName] = flag
		specialFlags << flag
	}
	
	def addWordFlag(String word, flag)
	{
		def (normals, specials) = extractSpecialFlags(flag)
		for(normal in normals)
		{
			def flagPath = HunspellXMLFlagPath.createWord(word, normal, specials)
			assignSpecialFlags(flagPath, specials)
			def flagPathS = flagPath.toString()
			if(!flagMap.containsKey(flagPathS)){flagMap[flagPathS] = flagPath}
			if(!wordFlags.contains(flagPathS)){wordFlags << flagPathS}
		}
		if(!normals) //only special flags present
		{
			def flagPath = HunspellXMLFlagPath.createWord(word, "", specials)
			flagPath.special = true
			assignSpecialFlags(flagPath, specials)
			//log.info("word with no normal affix flags, only specials...: " + flagPath)
			def flagPathS = flagPath.toString()
			if(!flagMap.containsKey(flagPathS)){flagMap[flagPathS] = flagPath}
			if(!wordFlags.contains(flagPathS)){combineFlags << flagPathS}
			//add word continuation rule + terminalFlagPath to the checked routes, so this one can be checked
			//helps catch errors like a CIRCUMFIX flag in the word continuation rules
			addCheckedRoute([flagPath, terminalFlagPath])
		}
	}
	
	def addAffixFlag(String flag, String type, String cross)
	{
		def crossB = (cross == "true")
		//Check if this flag already exists. Report an error if it does.
		if(!affixFlagMap.containsKey(flag)){affixFlagMap[flag] = [type:type, cross:crossB]}
		else
		{
			log.error("The " + affixFlagMap[flag].type + " flag '$flag' with cross=" + affixFlagMap[flag].crossB + " already exists, but the parser " +
				"encountered an attempt to add a $type flag '$flag' with cross=$cross.")
		}

		def flagPath
		if(type == "prefix"){flagPath = HunspellXMLFlagPath.createPrefixTarget(flag, crossB)}
		else{flagPath = HunspellXMLFlagPath.createSuffixTarget(flag, crossB)}
		def flagPathS = flagPath.toString()
		if(!flagMap.containsKey(flagPathS)){flagMap[flagPathS] = flagPath}
		if(!affixTargetMap.containsKey(flag)){affixTargetMap[flag] = flagPath}
		
		return flagPath
	}
	
	def addCombineFlags(HunspellXMLFlagPath parent, String flag, String type, String combineFlags)
	{
		def combineFlagsList = flagsToList(combineFlags)
		def (normals, specials) = extractSpecialFlags(combineFlagsList)
		for(normal in normals)
		{
			def flagPath
			if(type == "prefix"){flagPath = HunspellXMLFlagPath.createPrefixContinuation(normal, specials, parent.cross)}
			else{flagPath = HunspellXMLFlagPath.createSuffixContinuation(normal, specials, parent.cross)}
			assignSpecialFlags(flagPath, specials)
						
			parent.addBranch(flagPath)
			
			def flagPathS = flagPath.toString()
			if(!flagMap.containsKey(flagPathS)){flagMap[flagPathS] = flagPath}
			if(!combineFlags.contains(flagPathS)){combineFlags << flagPathS}
		}
		if(!normals)//only special flags present
		{
			def flagPath = HunspellXMLFlagPath.createSpecialContinuation(specials, parent.cross)
			assignSpecialFlags(flagPath, specials)
			
			parent.addBranch(flagPath)
			
			def flagPathS = flagPath.toString()
			if(!flagMap.containsKey(flagPathS)){flagMap[flagPathS] = flagPath}
			if(!combineFlags.contains(flagPathS)){combineFlags << flagPathS}
		}
	}
	
	def assignSpecialFlags(HunspellXMLFlagPath flagPath, List specials)
	{
		//Special rules to handle special flags for the continuation class
		if(specials.contains(specialFlagsReverseMap["NEEDAFFIX"]))
		{
			flagPath.needaffix = true
		}
		if(specials.contains(specialFlagsReverseMap["CIRCUMFIX"]))
		{
			flagPath.circumfix = true
		}
		if(specials.contains(specialFlagsReverseMap["FORBIDDENWORD"]))
		{
			flagPath.forbidden = true
		}
	}
	
	def mapFlagPaths()
	{
		for(flagPathKey in wordFlags)
		{
			def flagPath = flagMap[flagPathKey]
			traceFlagPaths(flagPath, 0, false, [])
		}
	}
	
	def traceFlagPaths(HunspellXMLFlagPath flagPath, int level, boolean target, List route)
	{
		if(level > 3){return}
		if(target)
		{
			if(!flagPath)
			{
				if(route.size() > 0 && route[-1].special)
				{
					//Don't add a missing path.
					flagPath = terminalFlagPath
					if(!flagMap.containsKey(flagPath.toString())){flagMap[flagPath.toString()] = flagPath}
				}
				else
				{
					flagPath = missingFlagPath
					if(!flagMap.containsKey(flagPath.toString())){flagMap[flagPath.toString()] = flagPath}
				}
			}
			def branches = flagPath.branches
			if(branches)
			{
				for(branch in flagPath.branches)
				{
					route << flagPath
					addCheckedRoute(route)
					//log.info(route.join(" => "))
					traceFlagPaths(branch, level+1, false, route)
					route.pop()
				}
			}
			else
			{
				route << flagPath
				addCheckedRoute(route)
				//log.info(route.join(" => "))
				route.pop()
			}
			return
		}
		else //continuation link
		{
			def targetPath = affixTargetMap[flagPath.flag]
			route << flagPath
			//log.info(route.join(" => "))
			traceFlagPaths(targetPath, level, true, route)
			route.pop()
			return
		}
	}
	
	def extractSpecialFlags(String flags)
	{
		def list = HunspellXMLUtils.hunspellFlagsToList(flagType, flags)
		return extractSpecialFlags(list)
	}
	
	def extractSpecialFlags(List list)
	{
		def specials = []
		for(flag in list)
		{
			if(specialFlags.contains(flag))
			{
				specials << flag
			}
		}
		for(flag in specials)
		{
			list.remove(flag)
		}
		list = list.sort{it.toLowerCase()}
		specials = specials.sort{it.toLowerCase()}
		return [list, specials]
	}

	def addCheckedRoute(List route)
	{
		//First test to see if we should keep this route.
		//If it ends in a flagPath representing a missing affix target,
		//or if it ends in a flagPath representing an affix target, 
		//continue to add it the map of checked routes 
		def flagPath = route[-1]
		if(flagPath.missing ||
			(flagPath && flagPath.target) ||
			(flagPath && flagPath.special)
		)
		{
			//But before adding it, check that it will conform to the NEEDAFFIX rules.
			//If all of its continuation rules have needaffix == true, don't add it yet.
			//It should be added later when there is a target that doesn't require additional affixes.
			//However, even if all the rules require affixes, if we've run out of affix levels (route.size() == 8)
			//then add the rule anyways so it can be reported as an error.
			def allNeedAffix = allRoutesNeedAffix(flagPath)
			def anyCircumfix = anyRoutesCircumfix(flagPath)
			if((!allNeedAffix && !anyCircumfix) || route.size() == 8)
			{
				if(!checkMap.containsKey(shortFormatRoute(route)))
				{
					checkMap[shortFormatRoute(route)] = [route:[*route], warnings:[]]
					//log.info(level + "|" + route)
					//log.info("Add Checked Route:")
					//log.info((route.size()/2) + "|" + shortFormatRoute(route))
				}
			}
		}
	}
	
	def allRoutesNeedAffix(HunspellXMLFlagPath flagPath)
	{
		def allNeedAffix = true
		if(!flagPath.branches){allNeedAffix = false}
		else
		{
			for(branch in flagPath.branches)
			{
				if(!branch.needaffix)
				{
					allNeedAffix = false
					break
				}
			}
		}
		return allNeedAffix
	}
	
	def checkRoutes()
	{
		for(entry in checkMap)
		{
			def route = entry.value.route
			def errors = entry.value.errors
			def warnings = entry.value.warnings
			checkRoute(route, warnings)
			if(warnings)
			{
				log.warning(shortFormatRoute(route) + "\r\n{\r\n\t" + warnings.join("\r\n\t") + "\r\n}\r\n")
			}
		}
	}
	
	def checkRoute(List route, List warnings)
	{
		checkAffixLimit(route, warnings)
		checkAffixCrossover(route, warnings)
		checkCircumfix(route, warnings)
		checkCrossProduct(route, warnings)
		checkForbidden(route, warnings)
		checkMissingAffix(route, warnings)
		checkNeedAffixOrder(route, warnings)
		checkNeedAffix(route, warnings)
	}
	
	def checkAffixLimit(List route, List warnings)
	{
		if(route.size() >= 8 && !route[7].terminal)
		{
			warnings << "Affix Limit Error: This affixation path contains too many affixes (4 or more). Only 3 affixes are allowed."
		}
		def prefixes = 0
		def suffixes = 0
		for(flagPath in route)
		{
			if(flagPath.prefix){prefixes++}
			else if(flagPath.suffix){suffixes++}
		}
		if(!complexPrefixes)
		{
			if(prefixes > 1)
			{
				warnings << "Prefix Limit Error: This affixation path contains too many prefixes ($prefixes). Only 1 prefix is allowed unless the COMPLEXPREFIXES option is set."
			}
			if(suffixes > 2)
			{
				warnings << "Suffix Limit Error: This affixation path contains too many suffixes ($suffixes). Only 2 suffixes are allowed unless the COMPLEXPREFIXES option is set, in which case the maximum limit is one suffix."
			}
		}
		else if(complexPrefixes)
		{
			if(suffixes > 1)
			{
				warnings << "Suffix Limit Error: This affixation path contains too many suffixes ($suffixes). Only 1 suffix is allowed when the COMPLEXPREFIXES option is set."
			}
			if(prefixes > 2)
			{
				warnings << "Prefix Limit Error: This affixation path contains too many prefixes ($prefixes). Only 2 prefixes are allowed when the COMPLEXPREFIXES option is set."
			}
		}
	}
	
	def checkAffixCrossover(List route, List warnings)
	{
		if(route.size() >= 6)
		{
			//If the affix pattern is: prefix -> suffix -> prefix 
			//or: suffix -> prefix -> suffix
			if((route[1].prefix && route[3].suffix && route[5].prefix) ||
				(route[1].suffix && route[3].prefix && route[5].suffix))
			{
				warnings << "Affix Crossover Error: Affixation paths that cross over twice (suffix->prefix->suffix or prefix->suffix->prefix) are invalid in Hunspell dictionaries. The second crossover will not be properly handled. Instead, use prefix->suffix->suffix, suffix->suffix->prefix, suffix->prefix->prefix, or prefix->prefix->suffix."
			}
		}
	}	
	
	def checkCrossProduct(List route, List warnings)
	{
		if(route.size() >= 6)
		{
			if(!checkCrossProduct(route[3], route[5]))
			{
				warnings << "Cross Product Error: The second and third affixes (a " + route[3].type + " and a " + route[5].type + ") should both have their 'cross' attribute set to 'true' or they will not be able to properly combine."
			}
			if(!checkCrossProduct(route[1], route[3]))
			{
				warnings << "Cross Product Error: The first and second affixes (a " + route[1].type + " and a " + route[3].type + ") should both have their 'cross' attribute set to 'true' or they will not be able to properly combine."
			}
		}
	}
	
	def checkCrossProduct(HunspellXMLFlagPath a, HunspellXMLFlagPath b)
	{
		if(b.missing){return true} //not a cross-product problem
		if(b.terminal){return true} //not a cross-product problem
		if(a.suffix == b.prefix)
		{
			//one is a suffix and one is a prefix
			if(a.cross && b.cross)
			{
				//cross-product is allowed
				return true
			}
			else
			{
				//cross-product needs to be enabled but it is not set on both positions
				return false
			}
		}
		else
		{
			//no cross-product needed
			return true
		}
	}
	
	def checkNeedAffixOrder(List route, List warnings)
	{
		//Given the following slots (parentheses indicate optionality):
		//Prefix1 (Prefix2) Word Suffix1 (Suffix2)
		//The only valid patterns for 3 affixes with NEEDAFFIX set for each step are:
		//1. Prefix1 -> Suffix1 -> Suffix2
		//2. Suffix -> Prefix2 -> Prefix1 with COMPLEXPREFIXES turned on
		
		//Route mapping with NEEDAFFIX on each step only matters if there are three steps
		if(route.size() >= 6)
		{
			//Only need to check if each continuation flag has NEEDAFFIX set on it.
			if(route[0].needaffix && route[2].needaffix && route[4].needaffix)
			{
				if(!complexPrefixes)
				{
					if(route[1].prefix && route[3].suffix && route[5].suffix){/*good*/}
					else
					{
						warnings << "Affix Order Error: Currently in Hunspell, if there are three affixes specified and each has the NEEDAFFIX flag attached, the only valid affixation path for reaching one prefix and two suffixes is: prefix -> suffix -> suffix. The current affixation path (${route[1].type} -> ${route[3].type} -> ${route[5].type}) does not match this pattern, and thus will not perform as expected."
					}
				}
				else
				{
					if(route[1].suffix && route[3].prefix && route[5].prefix){/*good*/}
					else
					{
						warnings << "Affix Order Error: Currently in Hunspell, if there are three affixes specified and each has the NEEDAFFIX flag attached, the only valid affixation path for reaching two prefixes and one suffix (with the COMPLEXPREFIXES option enabled) is: suffix -> prefix -> prefix. The current affixation path (${route[1].type} -> ${route[3].type} -> ${route[5].type}) does not match this pattern, and thus will not perform as expected."
					}
				}
			}
		}
	}
	
	def checkNeedAffix(List route, List warnings)
	{
		//Check the last valid affix (i.e. route[5], route[3], or route[1])
		//and verify that it does not have its needaffix flag set.
		def flagPath
		if(route.size() >= 6){flagPath = route[5]}
		else if(route.size() >= 4){flagPath = route[3]}
		else if(route.size() >= 2){flagPath = route[1]}
		
		//Check if all the continuation paths from the final affix are NEEDAFFIX
		def allNeedAffix = allRoutesNeedAffix(flagPath)
		if(!flagPath.branches){allNeedAffix = false}

		if(allNeedAffix)
		{
			warnings << "Need Affix Error: All of the continuation paths from the final " + (route.size() >= 6 ? "valid " : "") + "affix -- " + shortFormatRoute([flagPath]).trim() + " -- have the NEEDAFFIX flag attached, but they do not lead to another affix. This means this affixation path is invalid and will not work properly." +
						(route.size() >= 6 ? " Since this affixation path already has the maximum number of three affixes, this affixation path can never be properly completed unless you remove the NEEDAFFIX flag from the final affix.": "")
		}
	}
	
	def checkMissingAffix(List route, List warnings)
	{
		def targetPath
		def contPath
		if(route.size() >= 6)
		{
			targetPath = route[5]
			contPath = route[4]
		}
		else if(route.size() >= 4)
		{
			targetPath = route[3]
			contPath = route[2]
		}
		else if(route.size() >= 2)
		{
			targetPath = route[1]
			contPath = route[0]
		}
		if(targetPath && contPath)
		{
			if(targetPath.missing)
			{
				warnings << "Missing Affix Error: There is no affix definition for the continuation flag " + contPath.flag + "."
			}
		}
	}
	

	
	def checkCircumfix(List route, List warnings)
	{
		def anyCircumfixes = false
		def continuationCount = 0
		def circumfixCount = 0
		for(r in route)
		{
			if(r.continuation)
			{
				continuationCount++
				if(r.circumfix)
				{
					circumfixCount++
					anyCircumfixes = true
				}
			}
		}
		if(anyCircumfixes)
		{
			if(circumfixCount < 2 || 
				circumfixCount > 2 || 
				(circumfixCount == 2 && continuationCount < 3) ||
				(circumfixCount == 2 && continuationCount >= 3 && (!route[2].circumfix || !route[4].circumfix)))
			{
				warnings << "Circumfix Path Error: The circumfix flag can only be used in the first and second affixes' continuation rules. (It must be used on both). It may not be used on the third affix's continuation rules."
			}
			if(circumfixCount >= 2 && route.size() >= 6 && !route[5].terminal)
			{
				warnings << "Circumfix Path Error: Affixation paths containing the circumfix flag can only contain 2 affixes. A third affix is always invalid."
			}
			if(route[0].circumfix)
			{
				warnings << "Circumfix Path Error: The circumfix flag is never valid in the dictionary words' continuation rules."
			}
			
			if(circumfixCount >= 2 && continuationCount >= 3 && route[2].circumfix && route[4].circumfix)
			{
				if(complexPrefixes && route[1].suffix && route[2].circumfix && route[3].prefix)
				{
					warnings << "Circumfix Order Error: When using the circumfix flag with COMPLEXPREFIXES enabled (2-prefix mode), the first affix must be a prefix and the second affix must be a suffix."
				}
				else if(!complexPrefixes && route[1].prefix && route[2].circumfix && route[3].suffix)
				{
					warnings << "Circumfix Order Error: When using the circumfix flag with COMPLEXPREFIXES disabled (2-suffix mode), the first affix must be a suffix and the second affix must be a prefix."
				}
			}
		}
	}
	
	def anyRoutesCircumfix(HunspellXMLFlagPath flagPath)
	{
		//log.info("anyRoutesCircumfix(" + shortFormatRoute(flagPath) + ")")
		def anyCircumfix = []
		if(!flagPath.branches){anyCircumfix = []}
		else
		{
			for(branch in flagPath.branches)
			{
				if(branch.circumfix)
				{
					anyCircumfix << branch
					//log.info("\t" + shortFormatRoute(branch))
				}
			}
		}
		return anyCircumfix
	}
	
	def checkForbidden(List route, List warnings)
	{
		def uselessContinuations = false
		def contPath
		if(route.size() >= 6)
		{
			contPath = route[4]
			uselessContinuations |= contPath.forbidden
		}
		else if(route.size() >= 4)
		{
			contPath = route[2]
			uselessContinuations |= contPath.forbidden
		}
		else if(route.size() >= 2)
		{
			contPath = route[0]
			uselessContinuations |= contPath.forbidden
		}
		if(uselessContinuations)
		{
			warnings << "Forbidden Word Warning: This affixation path contains affixes after a FORBIDDENWORD flag. Any affixes after the first FORBIDDENWORD flag are unreachable and will be considered misspelled."
		}
	}
	
	
	def shortFormatRoute(route)
	{
		StringBuffer sb = new StringBuffer()
		def lastFlagPath
		for(flagPath in route)
		{
			if(flagPath.continuation)
			{
				if(flagPath.type == HunspellXMLFlagPath.WORD)
				{
					sb << "WORD[" + flagPath.word + "}"
				}
				sb << "--"
				if(flagPath.specialFlags){sb << "{" + flagPath.specialFlags.join(",") + "}"}
				sb << "-->  "
			}
			else //target
			{
				sb << flagPath.flag
				if(flagPath.missing){sb << "MISSING(" + (lastFlagPath?.flag?:"") + ")"}
				else if(flagPath.terminal){sb << "END"}
				else
				{
					sb << "("
					if(flagPath.prefix){sb << "P"}
					else if(flagPath.suffix){sb << "S"}
					if(flagPath.cross){sb << "X"}
					else{sb << "-"}
					sb << ")"
				}
				sb << "  "
			}
			lastFlagPath = flagPath
		}
		return sb.toString()
	}
}
