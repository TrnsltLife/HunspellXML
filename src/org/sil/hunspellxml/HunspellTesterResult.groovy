package org.sil.hunspellxml

class HunspellTesterResult
{
	def misspelled = false
	def word
	def suggest
	def stem
	def morph
	
	public HunspellTesterResult(String word)
	{
		this.word = word
	}
}
