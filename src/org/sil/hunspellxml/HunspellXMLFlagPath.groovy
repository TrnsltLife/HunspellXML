package org.sil.hunspellxml

class HunspellXMLFlagPath
{
	//Utility class for representing Hunspell affixes and affix continuation rules
	
	public static final String MISSING = "missing"
	public static final String WORD = "word"
	public static final String PREFIXTARGET = "prefix"
	public static final String SUFFIXTARGET = "suffix"
	public static final String PREFIXCONTINUATION = "prefixContinuation"
	public static final String SUFFIXCONTINUATION = "suffixContinuation"
	public static final String SPECIALCONTINUATION = "specialContinuation"
	public static final String TERMINALTARGET = "terminal"
	
	String word
	String flag
	List<HunspellXMLFlagPath> branches = []
	String type
	List<String> specialFlags = []
	boolean missing = false
	boolean continuation = false
	boolean target = false
	boolean prefix = false
	boolean suffix = false
	boolean cross = false
	boolean special = false
	boolean terminal = false
	boolean needaffix = false
	boolean circumfix = false
	boolean forbidden = false
	
	public HunspellXMLFlagPath(String flag)
	{
		this.flag = flag
	}
	
	public static createMissing()
	{
		def flagPath = new HunspellXMLFlagPath("")
		flagPath.type = MISSING
		flagPath.missing = true
		flagPath.target = true
		return flagPath
	}
	
	public static createWord(String word, String flag, List specialFlags)
	{
		def flagPath = new HunspellXMLFlagPath(flag)
		flagPath.word = word
		flagPath.type = WORD
		flagPath.continuation = true
		flagPath.specialFlags = specialFlags
		return flagPath
	}
	
	public static createPrefixTarget(String flag, boolean cross)
	{
		def flagPath = new HunspellXMLFlagPath(flag)
		flagPath.type = PREFIXTARGET
		flagPath.target = true
		flagPath.prefix = true
		flagPath.cross = cross
		return flagPath
	}
	
	public static createSuffixTarget(String flag, boolean cross)
	{
		def flagPath = new HunspellXMLFlagPath(flag)
		flagPath.type = SUFFIXTARGET
		flagPath.target = true
		flagPath.suffix = true
		flagPath.cross = cross
		return flagPath
	}
	
	public static createPrefixContinuation(String flag, List specialFlags, boolean cross)
	{
		def flagPath = new HunspellXMLFlagPath(flag)
		flagPath.type = PREFIXCONTINUATION
		flagPath.continuation = true
		flagPath.specialFlags = specialFlags
		flagPath.cross = cross
		return flagPath
	}
	
	public static createSuffixContinuation(String flag, List specialFlags, boolean cross)
	{
		def flagPath = new HunspellXMLFlagPath(flag)
		flagPath.type = SUFFIXCONTINUATION
		flagPath.continuation = true
		flagPath.specialFlags = specialFlags
		flagPath.cross = cross
		return flagPath
	}
	
	public static createSpecialContinuation(List specialFlags, boolean cross)
	{
		def flagPath = new HunspellXMLFlagPath("")
		flagPath.type = SPECIALCONTINUATION
		flagPath.continuation = true
		flagPath.specialFlags = specialFlags
		flagPath.cross = cross
		flagPath.special = true
		return flagPath
	}
	
	public static createTerminal()
	{
		def flagPath = new HunspellXMLFlagPath("")
		flagPath.type = TERMINALTARGET
		flagPath.target = true
		flagPath.terminal = true
		return flagPath
	}
	
	public void addBranch(HunspellXMLFlagPath flagPath)
	{
		branches << flagPath
	}

	public boolean equals(b)
	{
		def a = this
		return (a.flag == b.flag) && (a.type == b.type) && (a.cross == b.cross) && (a.specialFlags == b.specialFlags) &&
			(a.needaffix == b.needaffix) && (a.circumfix == b.circumfix) && (a.forbidden == b.forbidden)
	}
	
	public String toString()
	{
		return "$type($flag, X:$cross, $specialFlags, NA:$needaffix, CF:$circumfix, FB:$forbidden)"
	}
}
