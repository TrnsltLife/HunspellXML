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
	
	Log log = new Log()
	
	ValidationDriver driver
	String characterSet
	String flagType
	String relaxngXML = ""
	
	def interceptor = new SystemOutputInterceptor({ log.error(it); false});
		
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
		return validate(new ByteArrayInputStream(xmlDoc.getBytes(characterSet)))
	}
	
	public boolean validate(InputStream xmlDoc)
	{
		configureValidator()
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
			log.error(e.toString())
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
		<optional><attribute name="blankLines"><choice><value>true</value><value>false</value></choice></attribute></optional>
		<optional><attribute name="comments"><choice><value>true</value><value>false</value></choice></attribute></optional>
		<optional><attribute name="metadata"><choice><value>true</value><value>false</value></choice></attribute></optional>
		<optional><attribute name="myBlankLines"><choice><value>true</value><value>false</value></choice></attribute></optional>
		<optional><attribute name="myComments"><choice><value>true</value><value>false</value></choice></attribute></optional>
	</element>
	</optional>

	<interleave>
		[[COMMENT]]
		
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
				<element name="languageCode">[[LANGCODE]]</element>
				
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
						<value>KOI8-R</value>
						<value>KOI8-U</value>
						<value>microsoft-cp1251</value>
						<value>ISCII-DEVANAGARI</value>
					</choice>
				</element>

			
				<!-- flagType (REQUIRED) -->
				<element name="flagType">
					<choice>
						<value>[[FLAGTYPE]]</value>
					</choice>
				</element>
				
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
				</element>
				</optional>				

				<!-- circumfix -->
				<optional>
				<element name="circumfix"><attribute name="flag">[[FLAG]]</attribute></element>
				</optional>
				
				<!-- forbiddenWord -->
				<optional>
				<element name="forbiddenWord"><attribute name="flag">[[FLAG]]</attribute></element>
				</optional>
				
				<!-- keepCase -->
				<optional>
				<element name="keepCase"><attribute name="flag">[[FLAG]]</attribute></element>
				</optional>

				<!-- needAffix -->
				<optional>
				<element name="needAffix"><attribute name="flag">[[FLAG]]</attribute></element>
				</optional>
				
				<!-- substandard -->
				<optional>
				<element name="substandard"><attribute name="flag">[[FLAG]]</attribute></element>
				</optional>
				
				
				<!-- checkSharpS -->
				<optional>
				<element name="checkSharpS"><empty/></element>
				</optional>
				
				<!-- complexPrefixes -->
				<optional>
				<element name="complexPrefixes"><empty/></element>
				</optional>
				
				<!-- fullStrip -->
				<optional>
				<element name="fullStrip"><empty/></element>
				</optional>
				
				<!-- aliasFlags -->
				<!-- if set, program will alias all of your flags for you -->
				<optional>
				<element name="aliasFlags"><empty/></element>
				</optional>
				
				<!-- aliasMorphemes -->
				<!-- if set, program will alias all of your morphemes for you -->
				<optional>
				<element name="aliasMorphemes"><empty/></element>
				</optional>
				
			</interleave>
			</element>
			<!-- end of settings -->
			
			
			
			<!-- convert -->
			<optional>
			<element name="convert">
				[[COMMENT]]
				<zeroOrMore>
					<element name="input">
						<attribute name="from"><data type="string"/></attribute>
						<attribute name="to"><data type="string"/></attribute>
					</element>
				</zeroOrMore>
				[[COMMENT]]
				<zeroOrMore>
					<element name="output">
						<attribute name="from"><data type="string"/></attribute>
						<attribute name="to"><data type="string"/></attribute>
				</element>
				</zeroOrMore>
				[[COMMENT]]
			</element>
			</optional>
			
			
			
			<!-- suggestions -->
			<optional>
			<element name="suggestions">
			<interleave>
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
				</element>
				</optional>
				
				<!-- keyboard -->
				<optional>
					<element name="keyboard">
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
					<interleave>
						<!-- rule -->
						<zeroOrMore>
							<!-- Currently, no pattern validation is done. -->
							<element name="rule"><text/></element>
						</zeroOrMore>
					</interleave>
					</element>
				</optional>
				
				<!-- replacements -->
				<optional>
				<element name="replacements">
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
						</element>
					</zeroOrMore>
				</element>
				</optional>
				
				<!-- mappings -->
				<optional>
				<element name="mappings">
					<!-- map -->
					<zeroOrMore>
					<element name="map">
						<list>
							<oneOrMore>
								<data type="string"/>
							</oneOrMore>
						</list>
					</element>
					</zeroOrMore>
				</element>
				</optional>
				
			
				<!-- noSuggestions -->
				<optional>
				<element name="noSuggestions"><attribute name="flag">[[FLAG]]</attribute></element>
				</optional>
				
				<!-- warn -->
				<optional>
				<element name="warn"><attribute name="flag">[[FLAG]]</attribute></element>
				</optional>
				
				<!-- forbidWarn -->
				<optional>
				<element name="forbidWarn"><empty/></element>
				</optional>
				
				<!-- maxCompoundSuggestions -->
				<optional>
				<element name="maxCompoundSuggestions">
					<data type="integer">
						<param name="minInclusive">0</param>
					</data>
				</element>
				</optional>
				
				<!-- maxNGramSuggestions -->
				<optional>
				<element name="maxNGramSuggestions">
					<data type="integer">
						<param name="minInclusive">0</param>
					</data>
				</element>
				</optional>
				
				<!-- maxDifference -->
				<optional>
				<element name="maxDifference">
					<data type="integer">
						<param name="minInclusive">0</param>
						<param name="maxInclusive">10</param>
					</data>
				</element>
				</optional>

				<!-- onlyMaxDifference -->
				<optional>
				<element name="onlyMaxDifference"><empty/></element>
				</optional>
				
				<!-- noSplitSuggestions -->
				<optional>
				<element name="noSplitSuggestions"><empty/></element>
				</optional>
				
				<!-- suggestionsWithDots -->
				<optional>
				<element name="suggestionsWithDots"><empty/></element>
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
					<choice>
						<attribute name="off"><value>true</value></attribute>
						<list>
							<oneOrMore>
							<data type="string"/>
							</oneOrMore>
						</list>
					</choice>
				</element>
				</optional>
			
				<!-- compoundRules -->
				<optional>
				<element name="compoundRules">				
					<!-- rule -->
					<zeroOrMore>
					<element name="rule">
						<data type="string">
							<param name="pattern">[[COMPOUNDRULE]]</param>
						</data>
					</element>
					</zeroOrMore>
				</element>
				</optional>
				
				<!-- compoundPatterns -->
				<optional>
				<element name="compoundPatterns">
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
					</element>
					</zeroOrMore>
				</element>
				</optional>
			
				<!-- compoundMin -->
				<optional>
				<element name="compoundMin">
					<data type="integer">
						<param name="minInclusive">0</param>
					</data>
				</element>
				</optional>
				
				<!-- compoundWordMax -->
				<optional>
				<element name="compoundWordMax">
					<data type="integer">
						<param name="minInclusive">0</param>
					</data>
				</element>
				</optional>
				
				<!-- compoundSyllables -->
				<optional>
				<element name="compoundSyllables">
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
					<attribute name="flags">
					<list>
					<oneOrMore>[[FLAG]]</oneOrMore>
					</list>
					</attribute>
				</element>
				</optional>
			
				<!-- compound -->
				<optional>
				<element name="compound"><attribute name="flag">[[FLAG]]</attribute></element>
				</optional>
			
				<!-- compoundBegin -->
				<optional>
				<element name="compoundBegin"><attribute name="flag">[[FLAG]]</attribute></element>
				</optional>
			
				<!-- compoundMiddle -->
				<optional>
				<element name="compoundMiddle"><attribute name="flag">[[FLAG]]</attribute></element>
				</optional>
			
				<!-- compoundLast -->
				<optional>
				<element name="compoundLast"><attribute name="flag">[[FLAG]]</attribute></element>
				</optional>
			
				<!-- onlyInCompound -->
				<optional>
				<element name="onlyInCompound"><attribute name="flag">[[FLAG]]</attribute></element>
				</optional>
			
				<!-- compoundPermit -->
				<optional>
				<element name="compoundPermit"><attribute name="flag">[[FLAG]]</attribute></element>
				</optional>
			
				<!-- compoundForbid -->
				<optional>
				<element name="compoundForbid"><attribute name="flag">[[FLAG]]</attribute></element>
				</optional>
			
				<!-- compoundRoot -->
				<optional>
				<element name="compoundRoot"><attribute name="flag">[[FLAG]]</attribute></element>
				</optional>
				
				<!-- forceUpperCase -->
				<optional>
				<element name="forceUpperCase"><attribute name="flag">[[FLAG]]</attribute></element>
				</optional>
				
				<!-- checkCompoundDuplicates -->
				<optional>
				<element name="checkCompoundDuplicates"><empty/></element>
				</optional>
				
				<!-- checkCompoundReplacements -->
				<optional>
				<element name="checkCompoundReplacements"><empty/></element>
				</optional>
			
				<!-- checkCompoundCase -->
				<optional>
				<element name="checkCompoundCase"><empty/></element>
				</optional>
				
				<!-- checkCompoundTriple -->
				<optional>
				<element name="checkCompoundTriple"><empty/></element>
				</optional>
				
				<!-- simplifiedTriple -->
				<optional>
				<element name="simplifiedTriple"><empty/></element>
				</optional>
			
				<!-- compoundMoreSuffixes -->
				<optional>
				<element name="compoundMoreSuffixes"><empty/></element>
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
						<attribute name="flag">[[FLAG]]</attribute>
						<optional>
						<attribute name="cross"><choice><value>true</value><value>false</value></choice></attribute>
						</optional>
						<choice>
							<group>
							<interleave>
								<element name="multiply">
									<oneOrMore>
										<element name="group">
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
						<attribute name="flag">[[FLAG]]</attribute>
						<optional>
						<attribute name="cross"><choice><value>true</value><value>false</value></choice></attribute>
						</optional>
						<choice>
							<group>
							<interleave>
								<element name="multiply">
									<oneOrMore>
										<element name="group">
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
						<optional><attribute name="add"><text/></attribute></optional>
						<optional><attribute name="remove"><text/></attribute></optional>
						<optional><attribute name="where"><text/></attribute></optional>
						<optional>
						<attribute name="morph">
							<list>
								<oneOrMore>
									<!-- handles morphological info like st:word_stem -->
									<!-- TODO figure out what [prefix]+ and +SUFFIX info is and handle it -->
									<data type="string">
										<param name="pattern">[a-z][a-z]:[^\\s]+</param>
									</data>
								</oneOrMore>
							</list>
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
					<text/>
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
							<text/>
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