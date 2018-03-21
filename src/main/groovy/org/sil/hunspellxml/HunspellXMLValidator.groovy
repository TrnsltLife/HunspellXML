package org.sil.hunspellxml

// validate a schema using relaxgNG
// 1. download jing from http://code.google.com/p/jing-trang/
// 2. copy the jing.jar into $GROOVY_HOME/lib

import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.*
import com.thaiopensource.validate.rng.SAXSchemaReader;
import com.thaiopensource.xml.sax.ErrorHandlerImpl;
import org.xml.sax.InputSource;
import groovy.ui.SystemOutputInterceptor
 
class HunspellXMLValidator
{	
	//Use relaxNG to validate the HunspellXML file.
	//The schema is built using this class, but the
	//schema can be exported as one of the options in
	//HunspellXMLConverter.
	
	public static final String SHORT = "short"
	public static final String LONG = "long"
	public static final String NUM = "num"
	public static final String UTF8 = "UTF-8"
	public static final flagTypes = [SHORT, LONG, NUM, UTF8]
	public static final EOL = "\r\n"
	
	Log log = new Log()
	
	ValidationDriver driver
	String characterSet
	String flagType
	String relaxngXML = ""
	List<String> xmlDocLineList
	
	def interceptor = new SystemOutputInterceptor({Integer i, String s->
		s = s.replaceAll(/\(unknown file\):/, "")
		s = s.trim()
		def lineNumber = s.replaceAll(/([0-9]+):.*/, '$1').trim()
		def lineText = ""
		if(lineNumber.isInteger())
		{
			lineNumber = Integer.parseInt(lineNumber)
			if(xmlDocLineList?.getAt(lineNumber))
			{
				lineText = EOL + xmlDocLineList[lineNumber]
			}
		}
		if(lineNumber)
		{
			log.error("Line " + s + lineText)
		}
		else
		{
			log.error("Line " + s);
		}
		false
	});
		
	public HunspellXMLValidator(String characterSet, String flagType, Log log)
	{
		this(characterSet, flagType)
		this.log = log
	}
	
	public HunspellXMLValidator(String characterSet, String flagType)
	{
		if(!flagTypes.contains(flagType))
		{
			flagType = "short"
		}
		
		this.characterSet = characterSet
		this.flagType = flagType
	}
	
	public configureValidator()
	{
		
		interceptor.start()
		relaxngXML = relaxngXMLTemplate
			.replaceAll(/\[\[FLAG\]\]/, flagValidation[flagType])
			.replaceAll(/\[\[FLAGTYPE\]\]/, flagType)
			.replaceAll(/\[\[COMPOUNDRULE\]\]/, compoundRulePattern[flagType])
			.replaceAll(/\[\[COMMENT\]\]/, commentTemplate)
			.replaceAll(/\[\[LANGCODE\]\]/, languageCodePattern)
			.replaceAll(/\[\[COMMENT_ATTR\]\]/, commentAttrTemplate)
		xmlDocLineList = relaxngXML.readLines() //for printing line in error log
			
		PropertyMapBuilder properties = new PropertyMapBuilder();
		properties.put(ValidateProperty.ERROR_HANDLER,  new ErrorHandlerImpl(System.out));
		SchemaReader sr = SAXSchemaReader.getInstance();

		def schemaStream = new InputSource(new ByteArrayInputStream(relaxngXML.getBytes("UTF-8")))
		driver = new ValidationDriver(properties.toPropertyMap(), sr);
		interceptor.stop()
		
		if(!driver.loadSchema(schemaStream))
		{
			log.error('HunspellXMLValidator: problem loading schema')
			return
		}
	}

	public boolean validate(String xmlDoc)
	{
		xmlDocLineList = xmlDoc.readLines()
		return validate(new ByteArrayInputStream(xmlDoc.getBytes(characterSet)))
	}
	
	def doCall(Integer i, String s)
	{
		log.error("doCall method " + i.toString() + ", " + s)
	}
	
	public boolean validate(InputStream xmlDoc)
	{
		configureValidator()
		log.info("Beginning validation...")
		try
		{
			if(!driver.validate( new InputSource(xmlDoc)))
			{
				log.error('HunspellXMLValidator: problem validating')
				return false
			}
			else
			{
				log.info('HunspellXMLValidator: successfully validated')
				return true
			}
		}
		catch(Exception e)
		{
			log.error('Error processing XML:')
			log.error(e.getMessage())
			return false
		}
	}
	
	public String getSchema()
	{
		return relaxngXML
	}
	
	public static final flagValidation = [
"short":
'''<data type="string">
	<param name="length">1</param>
	<param name="pattern">[!-\u00ff]</param>
</data>''',

"long":
'''<data type="string">
	<param name="length">2</param>
	<param name="pattern">[!-\u00ff]{2,2}</param>
</data>''',

"num":
'''<data type="integer ">
	<param name="minInclusive">1</param>
	<param name="maxInclusive">65000</param>
</data>''',

"UTF-8":
'''<data type="string">
	<param name="length">1</param>
</data>'''
]

	
	public static final compoundRulePattern = [
"short":
"([!-\u00ff][\\?\\*]?)+",

"long":
"(\\\\([!-\u00ff]{2,2}\\\\)[\\?\\*]?)+", //repeated groups of 2 ASCII characters inside parentheses followed by optional ? or *

"num":
"(\\\\((65000|6[0-4][0-9]{3,3}|[1-5][0-9]{4,4}|[1-9][0-9]{0,3})\\\\)[\\?\\*]?)+", //repeated groups of numbers up to 65000 inside parentheses followed by optional ? or *

"UTF-8":
"([^\\s][\\?\\*]?)+"
]
	

public static final commentTemplate = '''
<!-- comment -->
<zeroOrMore>
	<element name="br"><empty/></element>
</zeroOrMore>
<zeroOrMore>
	<element name="comment"><text/></element>
</zeroOrMore>
'''

public static final commentAttrTemplate = '''
<optional>
	<attribute name="comment"><data type="string"/></attribute>
</optional>
'''

public static final languageCodePattern ='''<data type="string">
	<param name="minLength">2</param>
	<param name="maxLength">6</param>
	<param name="pattern">[a-z]{2,3}(_[A-Z]{2,2})?</param>
</data>'''

//Pattern to match a valid Hunspell word entry, e.g.:
//compound\/word\/parts/AFFIXS	Morphological/Data
public static final dictionaryWordTemplate = /^([^\s\t\/\\]|(\\\/))+(\/[^\s\t]+)?(\t.*)?$/
		
	
public static final relaxngXMLTemplate = '''
<!-- hunspell -->
<element name="hunspell" xmlns="http://relaxng.org/ns/structure/1.0" datatypeLibrary="http://www.w3.org/2001/XMLSchema-datatypes">
	<!-- suppress -->
	<optional>
	<element name="suppress">
		<optional><attribute name="autoBlankLines"><choice><value>true</value><value>false</value></choice></attribute></optional>
		<optional><attribute name="autoComments"><choice><value>true</value><value>false</value></choice></attribute></optional>
		<optional><attribute name="metadata"><choice><value>true</value><value>false</value></choice></attribute></optional>
		<optional><attribute name="myBlankLines"><choice><value>true</value><value>false</value></choice></attribute></optional>
		<optional><attribute name="myComments"><choice><value>true</value><value>false</value></choice></attribute></optional>
	</element>
	</optional>

	<interleave>
		<!-- metadata (REQUIRED) -->
		<optional>
		<element name="metadata">
		<interleave>
			[[COMMENT]]
			
			<!-- languageName -->
			<optional><element name="languageName"><text/></element></optional>
			
			<!-- localeList -->
			<optional>
			<element name="localeList">
				<list>
					<oneOrMore>
					[[LANGCODE]]
					</oneOrMore>
				</list>
			</element>
			</optional>
			
			<!-- version -->
			<optional><element name="version"><text/></element></optional>
			
			<!-- license -->
			<optional><element name="license"><text/></element></optional>
			
			<!-- creator -->
			<optional><element name="creator"><text/></element></optional>

			<!-- contributors -->
			<optional>
			<element name="contributors">
				<zeroOrMore>
					<element name="name"><text/></element>
				</zeroOrMore>
			</element>
			</optional>
			
			<!-- dictionaryName -->
			<optional><element name="dictionaryName"><text/></element></optional>
			
			<!-- filepath -->
			<optional><element name="filepath"><text/></element></optional>
			
			<!-- filename -->
			<optional><element name="filename"><text/></element></optional>
			
			<!-- readme -->
			<optional><element name="readme"><text/></element></optional>

			<!-- webpage -->
			<optional><element name="webpage"><text/></element></optional>
			
			<!-- description -->
			<optional><element name="description"><text/></element></optional>

			<!-- shortDescription -->
			<optional><element name="shortDescription"><text/></element></optional>
						
			<!-- firefoxVersion -->
			<optional>
			<element name="firefoxVersion">
				<attribute name="min"><data type="string"/></attribute>
				<attribute name="max"><data type="string"/></attribute>
			</element>
			</optional>
			
			<!-- thunderbirdVersion -->
			<optional>
			<element name="thunderbirdVersion">
				<attribute name="min"><data type="string"/></attribute>
				<attribute name="max"><data type="string"/></attribute>
			</element>
			</optional>
			
			<!-- custom -->
			<optional>
			<element name="customAttributes">
			<interleave>
				[[COMMENT]]
				<zeroOrMore>
					<element name="attribute">
						<attribute name="name"/>
						<choice>
							<text/>
							<empty/>
						</choice>
					</element>
				</zeroOrMore>
			</interleave>
			</element>
			</optional>
		</interleave>
		</element>
		</optional>

		<!-- affixFile -->
		<element name="affixFile">
		<interleave>
			[[COMMENT]]
		
			<!-- settings -->
			<element name="settings">
			<interleave>
				[[COMMENT]]
					
				<!-- languageCode (REQUIRED) -->
				<element name="languageCode">[[LANGCODE]][[COMMENT_ATTR]]</element>
				
				<!-- characterSet (REQUIRED) -->
				<element name="characterSet">
					<choice>
						<value>UTF-8</value>
						<value>ISO8859-1</value>
						<value>ISO8859-2</value>
						<value>ISO8859-3</value>
						<value>ISO8859-4</value>
						<value>ISO8859-5</value>
						<value>ISO8859-6</value>
						<value>ISO8859-7</value>
						<value>ISO8859-8</value>
						<value>ISO8859-9</value>
						<value>ISO8859-10</value>
						<value>ISO8859-13</value>
						<value>ISO8859-14</value>
						<value>ISO8859-15</value>
						<value>ISO-8859-1</value>
						<value>ISO-8859-2</value>
						<value>ISO-8859-3</value>
						<value>ISO-8859-4</value>
						<value>ISO-8859-5</value>
						<value>ISO-8859-6</value>
						<value>ISO-8859-7</value>
						<value>ISO-8859-8</value>
						<value>ISO-8859-9</value>
						<value>ISO-8859-10</value>
						<value>ISO-8859-13</value>
						<value>ISO-8859-14</value>
						<value>ISO-8859-15</value>
						<value>KOI8-R</value>
						<value>KOI8-U</value>
						<value>microsoft-cp1251</value>
						<value>ISCII-DEVANAGARI</value>
					</choice>
					[[COMMENT_ATTR]]
				</element>

				<!-- flagType (REQUIRED) -->
				<element name="flagType">
					<choice>
						<value>[[FLAGTYPE]]</value>
					</choice>
					[[COMMENT_ATTR]]
				</element>

				<!-- version -->
				<optional>
				<element name="version"><text/>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- wordChars -->
				<optional>
				<element name="wordChars">
					<list>
						<oneOrMore>
						<data type="string">
							<param name="length">1</param>
						</data>
						</oneOrMore>
					</list>
					[[COMMENT_ATTR]]
				</element>
				</optional>
				
				<!-- ignore -->
				<optional>
				<element name="ignore">
					<list>
						<oneOrMore>
						<data type="string">
							<param name="length">1</param>
						</data>
						</oneOrMore>
					</list>
					[[COMMENT_ATTR]]
				</element>
				</optional>				

				<!-- circumfix -->
				<optional>
				<element name="circumfix"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- forbiddenWord -->
				<optional>
				<element name="forbiddenWord"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- keepCase -->
				<optional>
				<element name="keepCase"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>

				<!-- needAffix -->
				<optional>
				<element name="needAffix"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- substandard -->
				<optional>
				<element name="substandard"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>
				
				
				<!-- checkSharpS -->
				<optional>
				<element name="checkSharpS"><empty/>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- complexPrefixes -->
				<optional>
				<element name="complexPrefixes"><empty/>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- fullStrip -->
				<optional>
				<element name="fullStrip"><empty/>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- aliasFlags -->
				<!-- if set, program will alias all of your flags for you -->
				<optional>
				<element name="aliasFlags"><empty/>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- aliasMorphemes -->
				<!-- if set, program will alias all of your morphemes for you -->
				<optional>
				<element name="aliasMorphemes"><empty/>[[COMMENT_ATTR]]</element>
				</optional>
				
			</interleave>
			</element>
			<!-- end of settings -->
			
			
			
			<!-- convertInput -->
			<optional>
				<element name="convertInput">
					[[COMMENT_ATTR]]
					[[COMMENT]]
					<zeroOrMore>
						<!-- input -->
						<element name="input">
							<attribute name="from"><data type="string"/></attribute>
							<attribute name="to"><data type="string"/></attribute>
							[[COMMENT_ATTR]]
						</element>
					</zeroOrMore>
					[[COMMENT]]
				</element>
			</optional>
			
			
			
			<!-- convertOutput -->
			<optional>
				<element name="convertOutput">
					[[COMMENT_ATTR]]
					[[COMMENT]]
					<!-- output -->
					<zeroOrMore>
						<element name="output">
							<attribute name="from"><data type="string"/></attribute>
							<attribute name="to"><data type="string"/></attribute>
							[[COMMENT_ATTR]]
						</element>
					</zeroOrMore>
					[[COMMENT]]
				</element>
			</optional>
			
			
			
			<!-- suggestions -->
			<optional>
			<element name="suggestions">
			<interleave>
				[[COMMENT_ATTR]]
				[[COMMENT]]
				
				<!-- tryChars -->
				<optional>
				<element name="tryChars">
					<optional>
					<attribute name="capitalize">
						<choice>
							<value>true</value>
							<value>false</value>
						</choice>
					</attribute>
					</optional>
					<list>
					<oneOrMore>
						<data type="string">
							<param name="length">1</param>
						</data>
					</oneOrMore>
					</list>
					[[COMMENT_ATTR]]
				</element>
				</optional>
				
				<!-- keyboard -->
				<optional>
					<element name="keyboard">
						[[COMMENT_ATTR]]
						<choice>
							<text/>
							<attribute name="layout">
								<choice>
									<value>QWERTY</value>
									<value>AZERTY</value>
									<value>Dvorak</value>
								</choice>
							</attribute>
						</choice>
					</element>
				</optional>
				
				<!-- phone -->
				<optional>
					<element name="phone">
						[[COMMENT_ATTR]]
						<interleave>
							<!-- rule -->
							<zeroOrMore>
								<!-- Currently, no pattern validation is done. -->
								<element name="rule">[[COMMENT_ATTR]]<text/></element>
							</zeroOrMore>
						</interleave>
					</element>
				</optional>
				
				<!-- replacements -->
				<optional>
				<element name="replacements">
					[[COMMENT_ATTR]]
					<!-- replace -->
					<zeroOrMore>
						<element name="replace">
							<attribute name="from"><data type="string"/></attribute>
							<attribute name="to"><data type="string"/></attribute>
							<optional>
								<attribute name="reverse">
									<choice>
										<value>true</value>
										<value>false</value>
									</choice>
								</attribute>
							</optional>
							[[COMMENT_ATTR]]
						</element>
					</zeroOrMore>
				</element>
				</optional>
				
				<!-- mappings -->
				<optional>
				<element name="mappings">
					[[COMMENT_ATTR]]
					<!-- map -->
					<zeroOrMore>
					<element name="map">
						[[COMMENT_ATTR]]
						<list>
							<oneOrMore>
								<data type="string"/>
							</oneOrMore>
						</list>
					</element>
					</zeroOrMore>
				</element>
				</optional>
				
				<!-- noNGramSuggestions -->
				<optional>
				<element name="noNGramSuggestions"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>
			
				<!-- noSuggestions -->
				<optional>
				<element name="noSuggestions"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- warn -->
				<optional>
				<element name="warn"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- forbidWarn -->
				<optional>
				<element name="forbidWarn"><empty/>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- maxCompoundSuggestions -->
				<optional>
				<element name="maxCompoundSuggestions">
					<data type="integer">
						<param name="minInclusive">0</param>
					</data>
					[[COMMENT_ATTR]]
				</element>
				</optional>
				
				<!-- maxNGramSuggestions -->
				<optional>
				<element name="maxNGramSuggestions">
					<data type="integer">
						<param name="minInclusive">0</param>
					</data>
					[[COMMENT_ATTR]]
				</element>
				</optional>
				
				<!-- maxDifference -->
				<optional>
				<element name="maxDifference">
					<data type="integer">
						<param name="minInclusive">0</param>
						<param name="maxInclusive">10</param>
					</data>
					[[COMMENT_ATTR]]
				</element>
				</optional>

				<!-- onlyMaxDifference -->
				<optional>
				<element name="onlyMaxDifference"><empty/>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- noSplitSuggestions -->
				<optional>
				<element name="noSplitSuggestions"><empty/>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- suggestionsWithDots -->
				<optional>
				<element name="suggestionsWithDots"><empty/>[[COMMENT_ATTR]]</element>
				</optional>
			</interleave>
			</element>
			</optional>
			<!-- end of suggestions -->
			
			
			
			
			
			<!-- compounds -->
			<optional>
			<element name="compounds">
			<interleave>
				[[COMMENT]]
			
				<!-- breakChars -->
				<optional>
				<element name="breakChars">
					[[COMMENT_ATTR]]
					<!-- chars -->
					<choice>
						<attribute name="off"><value>true</value></attribute>
						<zeroOrMore>
							<element name="chars">[[COMMENT_ATTR]]<text/></element>
						</zeroOrMore>
					</choice>
				</element>
				</optional>
			
				<!-- compoundRules -->
				<optional>
				<element name="compoundRules">
					[[COMMENT_ATTR]]
					<!-- rule -->
					<zeroOrMore>
					<element name="rule">
						<data type="string">
							<param name="pattern">[[COMPOUNDRULE]]</param>
						</data>
						[[COMMENT_ATTR]]
					</element>
					</zeroOrMore>
				</element>
				</optional>
				
				<!-- compoundPatterns -->
				<optional>
				<element name="compoundPatterns">
					[[COMMENT_ATTR]]
					<!-- pattern -->
					<zeroOrMore>
					<element name="pattern">
						<attribute name="startChars"><data type="string"/></attribute>
						<attribute name="endChars"><data type="string"/></attribute>

						<optional>
						<attribute name="startFlags">
							<list><oneOrMore>[[FLAG]]</oneOrMore></list>
						</attribute>
						</optional>
						
						<optional>
						<attribute name="endFlags">
							<list><oneOrMore>[[FLAG]]</oneOrMore></list>
						</attribute>
						</optional>
						
						<optional><attribute name="replacement"><data type="string"/></attribute></optional>
						[[COMMENT_ATTR]]
					</element>
					</zeroOrMore>
				</element>
				</optional>
			
				<!-- compoundMin -->
				<optional>
				<element name="compoundMin">
					[[COMMENT_ATTR]]
					<data type="integer">
						<param name="minInclusive">0</param>
					</data>
				</element>
				</optional>
				
				<!-- compoundWordMax -->
				<optional>
				<element name="compoundWordMax">
					[[COMMENT_ATTR]]
					<data type="integer">
						<param name="minInclusive">0</param>
					</data>
				</element>
				</optional>
				
				<!-- compoundSyllable -->
				<optional>
				<element name="compoundSyllable">
					[[COMMENT_ATTR]]
					<attribute name="max">
						<data type="integer">
							<param name="minInclusive">0</param>
						</data>
					</attribute>
					<attribute name="vowels">
						<list>
							<oneOrMore>
							<data type="string"/>
							</oneOrMore>
						</list>
					</attribute>
				</element>
				</optional>
				
				<!-- syllableNum -->
				<optional>
				<element name="syllableNum">
					[[COMMENT_ATTR]]
					<attribute name="flags">
					<list>
					<oneOrMore>[[FLAG]]</oneOrMore>
					</list>
					</attribute>
				</element>
				</optional>
			
				<!-- compound -->
				<optional>
				<element name="compound"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>
			
				<!-- compoundBegin -->
				<optional>
				<element name="compoundBegin"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>
			
				<!-- compoundMiddle -->
				<optional>
				<element name="compoundMiddle"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>
			
				<!-- compoundEnd -->
				<optional>
				<element name="compoundEnd"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>
			
				<!-- onlyInCompound -->
				<optional>
				<element name="onlyInCompound"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>
			
				<!-- compoundPermit -->
				<optional>
				<element name="compoundPermit"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>
			
				<!-- compoundForbid -->
				<optional>
				<element name="compoundForbid"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>
			
				<!-- compoundRoot -->
				<optional>
				<element name="compoundRoot"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- forceUpperCase -->
				<optional>
				<element name="forceUpperCase"><attribute name="flag">[[FLAG]]</attribute>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- checkCompoundDuplicates -->
				<optional>
				<element name="checkCompoundDuplicates"><empty/>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- checkCompoundReplacements -->
				<optional>
				<element name="checkCompoundReplacements"><empty/>[[COMMENT_ATTR]]</element>
				</optional>
			
				<!-- checkCompoundCase -->
				<optional>
				<element name="checkCompoundCase"><empty/>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- checkCompoundTriple -->
				<optional>
				<element name="checkCompoundTriple"><empty/>[[COMMENT_ATTR]]</element>
				</optional>
				
				<!-- simplifiedTriple -->
				<optional>
				<element name="simplifiedTriple"><empty/>[[COMMENT_ATTR]]</element>
				</optional>
			
				<!-- compoundMoreSuffixes -->
				<optional>
				<element name="compoundMoreSuffixes"><empty/>[[COMMENT_ATTR]]</element>
				</optional>

			</interleave>
			</element>
			</optional>
			<!-- end of compounds -->
			
			
			
			
			
			<!-- affixes -->
			<grammar>
				<start>
				<optional>
				<element name="affixes">
				<interleave>
					[[COMMENT]]
					
					<zeroOrMore>
					<element name="prefix">
						[[COMMENT_ATTR]]
						<attribute name="flag">[[FLAG]]</attribute>
						<optional>
						<attribute name="cross"><choice><value>true</value><value>false</value></choice></attribute>
						</optional>
						<choice>
							<group>
							<interleave>
								<element name="multiply">
									[[COMMENT_ATTR]]
									<oneOrMore>
										<element name="group">
											[[COMMENT_ATTR]]
											<oneOrMore>
												<ref name="rule"/>
											</oneOrMore>
										</element>
									</oneOrMore>
								</element>
								<zeroOrMore>
									<ref name="tests"/>
								</zeroOrMore>
							</interleave>
							</group>
							<group>
							<interleave>
								<oneOrMore>
									<ref name="rule"/>
								</oneOrMore>
								<zeroOrMore>
									<ref name="tests"/>
								</zeroOrMore>
							</interleave>
							</group>
						</choice>
					<text/>
					</element>
					</zeroOrMore>
					
					<zeroOrMore>
					<element name="suffix">
						[[COMMENT_ATTR]]
						<attribute name="flag">[[FLAG]]</attribute>
						<optional>
						<attribute name="cross"><choice><value>true</value><value>false</value></choice></attribute>
						</optional>
						<choice>
							<group>
							<interleave>
								<element name="multiply">
									[[COMMENT_ATTR]]
									<oneOrMore>
										<element name="group">
											[[COMMENT_ATTR]]
											<oneOrMore>
												<ref name="rule"/>
											</oneOrMore>
										</element>
									</oneOrMore>
								</element>
								<zeroOrMore>
									<ref name="tests"/>
								</zeroOrMore>
							</interleave>
							</group>
							<group>
							<interleave>
								<oneOrMore>
									<ref name="rule"/>
								</oneOrMore>
								<zeroOrMore>
									<ref name="tests"/>
								</zeroOrMore>
							</interleave>
							</group>
						</choice>
					</element>
					</zeroOrMore>
				</interleave>
				</element>
				</optional>
				</start>
				
				<!-- rule -->
				<define name="rule">
					<element name="rule">
						[[COMMENT_ATTR]]
						<optional><attribute name="add"><text/></attribute></optional>
						<optional><attribute name="remove"><text/></attribute></optional>
						<optional><attribute name="where"><text/></attribute></optional>
						<optional>
						<attribute name="morph">
							<choice>
								<list>
									<oneOrMore>
										<!-- handles morphological info like st:word_stem -->
										<data type="string">
											<param name="pattern">[a-z][a-z]:[^\\s]+</param>
										</data>
									</oneOrMore>
								</list>
								<!-- Any other string -->
								<data type="string"/>
							</choice>
						</attribute>
						</optional>
						<optional>
							<attribute name="combineFlags">
								<list>
									<oneOrMore>[[FLAG]]</oneOrMore>
								</list>
							</attribute>
						</optional>
					</element>
				</define>
				
				<!-- tests -->
				<define name="tests">
					<element name="tests">
						[[COMMENT_ATTR]]
						<interleave>
							<zeroOrMore>
								<optional><element name="good"><text/></element></optional>
								<optional><element name="bad"><text/></element></optional>
							</zeroOrMore>
						</interleave>
					</element>
				</define>
				
			</grammar>
			<!-- end of affixes -->

		</interleave>
		</element>
		<!-- end of affixFile -->
		
		<!-- tests -->
		<optional>
		<element name="tests">
			[[COMMENT_ATTR]]
			<interleave>
				<zeroOrMore>
					<optional><element name="good"><text/></element></optional>
					<optional><element name="bad"><text/></element></optional>
				</zeroOrMore>
			</interleave>
		</element>
		</optional>
		<!-- end of tests -->
		
		<!-- dictionaryFile -->
		<element name="dictionaryFile">
		[[COMMENT_ATTR]]
		<interleave>
			<!-- words -->
			<zeroOrMore>
				<element name="words">
					<optional>
					<attribute name="flags">
						<list><oneOrMore>[[FLAG]]</oneOrMore></list>
					</attribute>
					</optional>
					<optional>
					<attribute name="morph"><data type="string"/></attribute>
					</optional>
					<choice>
						<text/>

						<!-- <w>word</w> -->
						<zeroOrMore>
							<element name="w">
								<optional>
								<attribute name="flags">
									<list><oneOrMore>[[FLAG]]</oneOrMore></list>
								</attribute>
								</optional>
								<optional>
								<attribute name="morph"><data type="string"/></attribute>
								</optional>
								<text/>
							</element>
						</zeroOrMore>
					</choice>
				</element>
			</zeroOrMore>
			
			<!-- include -->
			<zeroOrMore>
				<element name="include">
					<attribute name="file"><data type="string"/></attribute>
					<optional>
						<attribute name="flags">
							<list><oneOrMore>[[FLAG]]</oneOrMore></list>
						</attribute>
					</optional>
					<optional>
						<attribute name="morph"><data type="string"/></attribute>
					</optional>
				</element>
			</zeroOrMore>
		</interleave>
		</element>
		<!-- end of dictionaryFile -->
		
		<!-- thesaurusFile -->
		<optional>
		<element name="thesaurusFile">
		<interleave>
			<!-- entry -->
			<zeroOrMore>
				<element name="entry">
					<attribute name="word"><data type="string"/></attribute>
					<zeroOrMore>
						<!-- synonyms -->
						<element name="synonyms">
							<attribute name="info"><data type="string"/></attribute>
							<choice>
								<!-- <s>synonym</s> -->
								<oneOrMore>
									<element name="s"><text/></element>
								</oneOrMore>

								<text/>
							</choice>
						</element>
					</zeroOrMore>
				</element>
			</zeroOrMore>
			
			<!-- entries -->
			<zeroOrMore>
				<element name="entries">
					<text/>
				</element>
			</zeroOrMore>
		
			<!-- include -->
			<zeroOrMore>
				<element name="include">
					<attribute name="file"><data type="string"/></attribute>
				</element>
			</zeroOrMore>
		</interleave>
		</element>
		</optional>
		<!-- end of thesaurusFile -->
	</interleave>
</element>
<!-- end of hunspell -->
'''.toString()
	
	
}