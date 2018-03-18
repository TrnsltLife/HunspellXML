Basic Suffix + Prefix Rule
==========================
* When a word or rule has multiple combineFlags continuation rules
* And when at least one is a prefix and one a suffix
* The word or rule may apply both **one** *prefix* and **one** *suffix*
* **A long as** the prefix and suffix rule have the **cross="true"** attribute set
* e.g. 

	drink/UNBL
	
	<suffix flag="BL" cross="true">
		<rule add="able"/>
	</suffix>
	<prefix flag="UN" cross="true">
		<rule add="un"/>
	</prefix>
	
Basic NeedAffix Rule
====================	
* NeedAffix flag only applies to the side (prefix/suffix) that can have 2 affixes. i.e. normally the suffix side can have NA and it will apply.

Other Rules
===========
* No matter what, two prefixes cannot be added except in <complexPrefixes/> mode
* No matter what, you can't cross over twice
* Activating two affixes at the same time (circumfix) counts as a crossover.
* So you can't do a circumfix followed by another prefix (or suffix)

				/-----> prefix
	X	WORD <
				\-----> suffix -----> suffix
				
if 2 suffix mode
  WORD[word] ----> P1(PX) ++++> S1(SX) ----> S2(S-)

if 2 prefix mode
  WORD[word] ----> S1(SX) ++++> P1(PX) ----> P2(P-)

