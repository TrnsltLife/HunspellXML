package org.sil.hunspellxml

import java.io.File;

class HunspellXMLData
{
	//In-memory representation of dictionary & thesaurus files
	
	File basePath
	def currentSection = "hunspell"
	
	def metadata = [version:"0.0",
		license:"All rights reserved.",
		creator:"Anonymous",
		contributors:[],
		dictionaryName:"",
		filepath:"",
		filename:"",
		readme:"None",
		webpage:"None",
		description:"None",
		shortDescription:"None",
		firefoxVersion:[min:"0.0", max:"100.0"],
		thunderbirdVersion:[min:"0.0", max:"100.0"]
	]
	
	//Hunspell Dictionary
	def affFile = new StringBuffer()
	def dicFile = new StringBuffer()
	def dicList = []
	def dicCount = 0
	
	//MyThes Thesaurus
	def datFile = new StringBuffer()
	def idxFile = new StringBuffer()
	def idxCount = 0
	def idxOffset = 0
	def idxList = []
	
	//Test Files
	def goodTestFile = new StringBuffer()
	def badTestFile = new StringBuffer()

	
	public HunspellXMLData()
	{
	}
}
