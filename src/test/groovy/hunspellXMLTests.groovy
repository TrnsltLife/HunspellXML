import org.sil.hunspellxml.HunspellXMLCLI

def dir = "src/test/groovy/data/hunspellXMLTests"

def cli(command)
{
	println("HunspellXML " + command)
	HunspellXMLCLI.main(command.split(" "))
}
def del(files) {files.each{file-> new File(file).delete()}}
	
cli("-hs -ts -rt -o=${dir}/words ${dir}/words.xml")
del(["$dir/words.aff", "$dir/words.dic", "$dir/words.good", "$dir/words.wrong"])

