import org.sil.hunspellxml.HunspellXMLCLI

def dir = "src/test/groovy/data/hunspellXMLTests"

def cli(command)
{
	println("HunspellXML " + command)
	HunspellXMLCLI.main(command.split(" "))
}
def del(files) {files.each{file-> new File(file).delete()}}
	
cli("-hs -ts -rt -o=${dir}/wordsFlagsMorph ${dir}/wordsFlagsMorph.xml")
del(["$dir/wordsFlagsMorph.aff", "$dir/wordsFlagsMorph.dic", "$dir/wordsFlagsMorph.good", "$dir/wordsFlagsMorph.wrong"])

