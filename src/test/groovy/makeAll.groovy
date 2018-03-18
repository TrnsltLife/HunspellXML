import org.sil.hunspellxml.*;  

File xmlFile = new File("D:/Dev/LangDev/Hunspell/HunspellXML/ln_test/ln_hunspell.xml")
def hxc = new HunspellXMLConverter(xmlFile)
hxc.convert()