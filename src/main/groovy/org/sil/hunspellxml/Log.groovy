package org.sil.hunspellxml

class Log
{
	public static final int DEBUG = 1
	public static final int INFO = 2
	public static final int WARNING = 3
	public static final int ERROR = 4
	public static final int NONE = 5
	
	def logLevel = INFO
	
	public Log()
	{
		logLevel = INFO
	}
	
	public Log(int level)
	{
		setLogLevel(level)
	}
	
	public int getLogLevel() {return logLevel}
	public void setLogLevel(int level)
	{
		logLevel = level
		if(logLevel < DEBUG) {logLevel = DEBUG}
		if(logLevel > ERROR) {logLevel = ERROR}
	}
	
	def debugLog = new StringBuilder()
	def debugLogHook = {}
	def void debug(message)
	{
		if(logLevel <= DEBUG)
		{
			debugLog << message
			println("DEBUG: " + message)
			debugLogHook.call(message)
		}
	}
	
	def infoLog = new StringBuilder()
	def infoLogHook = {}
	def void info(message)
	{
		if(logLevel <= INFO)
		{
			infoLog << message
			println("INFO: " + message)
			infoLogHook.call(message)
		}
	}
	
	def warningLog = new StringBuilder()
	def warningLogHook = {}
	def void warning(message)
	{
		if(logLevel <= WARNING)
		{
			warningLog << message
			println("WARNING: " + message)
			warningLogHook.call(message)
		}
	}
	
	def errorLog = new StringBuilder()
	def errorLogHook = {}
	def void error(message)
	{
		if(logLevel <= ERROR)
		{
			errorLog << message
			println("ERROR: " + message)
			errorLogHook.call(message)
		}
	}
}
