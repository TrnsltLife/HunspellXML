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

//wordFlagsMorph
cli("-hs -ts -rt -o=${out}/wordFlagsMorph ${dir}/wordFlagsMorph.xml")
//del(["$out/wordFlagsMorph.aff", "$out/wordFlagsMorph.dic", "$out/wordFlagsMorph.good", "$out/wordFlagsMorph.wrong"])

//wordSlash
cli("-o=${out}/wordSlash.xml ${dir}/wordSlash.dic")
//del(["$out/wordSlash.aff", "$out/wordSlash.dic", "$out/wordSlash.good", "$out/wordSlash.wrong"])

//morphSpaceSlash
cli("-o=${out}/morphSpaceSlash.xml ${dir}/morphSpaceSlash.dic")
cli("-hs -ts -o=${out}/morphSpaceSlash ${out}/morphSpaceSlash.xml")
