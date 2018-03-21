import org.sil.hunspellxml.HunspellXMLCLI

def dir = "src/test/groovy/data/hunspellXMLTests"
def out = "src/test/groovy/data/hunspellXMLTests/out"

def cli(command)
{
	println("HunspellXML " + command)
	HunspellXMLCLI.main(command.split(" "))
}
def del(files) {files.each{file-> new File(file).delete()}}

//cop_EG
cli("-o=${out}/cop_EG.xml -l=debug ${dir}/cop_EG.dic")
//del(["$dir/cop_EG.xml"])

//version
cli("-o=${out}/version.xml ${dir}/version.dic")
//del(["$dir/version.xml"])

//wordsFlagsMorph
cli("-hs -ts -rt -o=${out}/wordsFlagsMorph ${dir}/wordsFlagsMorph.xml")
//del(["$out/wordsFlagsMorph.aff", "$out/wordsFlagsMorph.dic", "$out/wordsFlagsMorph.good", "$out/wordsFlagsMorph.wrong"])

