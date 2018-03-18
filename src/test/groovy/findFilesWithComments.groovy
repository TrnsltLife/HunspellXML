//Find files in hunspell tests that have comments in them #
File dir = new File("D:/Dev/LangDev/Hunspell/hunspell-1.3.2/tests/")
List files = []
dir.eachFileMatch(groovy.io.FileType.FILES, ~/.*\.(aff|dic|good|wrong)/){file->
	def lines = file.readLines()
	for(line in lines)
	{
		if(line =~ /.+#.*/) //comment not at the beginning of the line
		{
			files << file
			break
		}
	}
}

new File("src/test/groovy/filesWithComments.groovy").withWriter("UTF-8"){writer->
	writer << "List filesWithComments = [\r\n\t" + 
		files.collect{'"' + it.getCanonicalPath().replaceAll("\\\\", '/') + '"'}.join(", \r\n\t") + 
		"\r\n];"
}