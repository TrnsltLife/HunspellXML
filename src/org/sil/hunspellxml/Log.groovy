package org.sil.hunspellxml

class Log
{
	def infoLog = new StringBuilder()
	def infoLogHook = {}
	def void info(message)
	{
		infoLog << message
		println(message)
		infoLogHook.call(message)
	}
	
	def warningLog = new StringBuilder()
	def warningLogHook = {}
	def void warning(message)
	{
		warningLog << message
		println("WARNING: " + message)
		warningLogHook.call(message)
	}
	
	def errorLog = new StringBuilder()
	def errorLogHook = {}
	def void error(message)
	{
		errorLog << message
		println("ERROR: " + message)
		errorLogHook.call(message)
	}
}
