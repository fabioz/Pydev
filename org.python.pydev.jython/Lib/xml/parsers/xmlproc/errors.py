
# This file contains the lists of error messages used by xmlproc

import string

# The interface to the outside world

error_lists={}  # The hash of errors

def add_error_list(language,list):
    error_lists[string.lower(language)]=list

def get_error_list(language):
    return error_lists[string.lower(language)]

def get_language_list():
    return error_lists.keys()

# Errors in English

english={

    # --- Warnings: 1000-1999
    1000: "Undeclared namespace prefix '%s'",
    1002: "Unsupported encoding '%s'",
    1003: "Obsolete namespace syntax",
    1005: "Unsupported character number '%d' in character reference",
    1006: "Element '%s' has attribute list, but no element declaration",
    1007: "Attribute '%s' defined more than once",
    1008: "Ambiguous content model",

    # --- Namespace warnings
    1900: "Namespace prefix names cannot contain ':'s.",
    1901: "Namespace URI cannot be empty",
    1902: "Namespace prefix not declared",
    1903: "Attribute names not unique after namespace processing",

    # --- Validity errors: 2000-2999
    2000: "Actual value of attribute '%s' does not match fixed value",
    2001: "Element '%s' not allowed here",
    2002: "Document root element '%s' does not match declared root element",
    2003: "Element '%s' not declared",
    2004: "Element '%s' ended, but not finished",
    2005: "Character data not allowed in the content of this element",
    2006: "Attribute '%s' not declared",
    2007: "ID '%s' appears more than once in document",
    2008: "Only unparsed entities allowed as the values of ENTITY attributes",
    2009: "Notation '%s' not declared",
    2010: "Required attribute '%s' not present",
    2011: "IDREF referred to non-existent ID '%s'",
    2012: "Element '%s' declared more than once",
    2013: "Only one ID attribute allowed on each element type",
    2014: "ID attributes cannot be #FIXED or defaulted",
    2015: "xml:space must be declared an enumeration type",
    2016: "xml:space must have exactly the values 'default' and 'preserve'",
    2017: "'%s' is not an allowed value for the '%s' attribute",
    2018: "Value of '%s' attribute must be a valid name",
    2019: "Value of '%s' attribute not a valid name token",
    2020: "Value of '%s' attribute not a valid name token sequence",
    2021: "Token '%s' in the value of the '%s' attribute is not a valid name",
    2022: "Notation attribute '%s' uses undeclared notation '%s'",
    2023: "Unparsed entity '%s' uses undeclared notation '%s'",

    # --- Well-formedness errors: 3000-3999
    # From xmlutils
    3000: "Couldn't open resource '%s'",
    3001: "Construct started, but never completed",
    3002: "Whitespace expected here",
    3003: "Didn't match '%s'",   ## FIXME: This must be redone
    3004: "One of %s or '%s' expected",
    3005: "'%s' expected",

    # From xmlproc.XMLCommonParser
    3006: "SYSTEM or PUBLIC expected",
    3007: "Text declaration must appear first in entity",
    3008: "XML declaration must appear first in document",
    3009: "Multiple text declarations in a single entity",
    3010: "Multiple XML declarations in a single document",
    3011: "XML version missing on XML declaration",
    3012: "Standalone declaration on text declaration not allowed",
    3045: "Processing instruction target names beginning with 'xml' are reserved",
    3046: "Unsupported XML version",
    
    # From xmlproc.XMLProcessor
    3013: "Illegal construct",
    3014: "Premature document end, element '%s' not closed",
    3015: "Premature document end, no root element",
    3016: "Attribute '%s' occurs twice",
    3017: "Elements not allowed outside root element",
    3018: "Illegal character number '%d' in character reference",
    3019: "Entity recursion detected",
    3020: "External entity references not allowed in attribute values",
    3021: "Undeclared entity '%s'",
    3022: "'<' not allowed in attribute values",
    3023: "End tag for '%s' seen, but '%s' expected",
    3024: "Element '%s' not open",
    3025: "']]>' must not occur in character data",
    3027: "Not a valid character number",
    3028: "Character references not allowed outside root element",
    3029: "Character data not allowed outside root element",
    3030: "Entity references not allowed outside root element",
    3031: "References to unparsed entities not allowed in element content",
    3032: "Multiple document type declarations",
    3033: "Document type declaration not allowed inside root element",
    3034: "Premature end of internal DTD subset",
    3042: "Element crossed entity boundary",

    # From xmlproc.DTDParser
    3035: "Parameter entities cannot be unparsed",
    3036: "Parameter entity references not allowed in internal subset declarations",
    3037: "External entity references not allowed in entity replacement text",
    3038: "Unknown parameter entity '%s'",
    3039: "Expected type or alternative list",
    3040: "Choice and sequence lists cannot be mixed",
    3041: "Conditional sections not allowed in internal subset",
    3043: "Conditional section not closed",
    3044: "Token '%s' defined more than once",
    # next: 3047
    
    # From regular expressions that were not matched
    3900: "Not a valid name",
    3901: "Not a valid version number (%s)",
    3902: "Not a valid encoding name",
    3903: "Not a valid comment",
    3905: "Not a valid hexadecimal number",
    3906: "Not a valid number",
    3907: "Not a valid parameter reference",
    3908: "Not a valid attribute type",
    3909: "Not a valid attribute default definition",
    3910: "Not a valid enumerated attribute value",
    3911: "Not a valid standalone declaration",
    
    # --- Internal errors: 4000-4999
    4000: "Internal error: Entity stack broken",
    4001: "Internal error: Entity reference expected.",
    4002: "Internal error: Unknown error number.",
    4003: "Internal error: External PE references not allowed in declarations",

    # --- XCatalog errors: 5000-5099
    5000: "Uknown XCatalog element: %s.",
    5001: "Required XCatalog attribute %s on %s missing.",
     
    # --- SOCatalog errors: 5100-5199
    5100: "Invalid or unsupported construct: %s.",
    }

# Errors in Norwegian

norsk={

    # --- Warnings: 1000-1999
    1000: "Navneroms-prefikset '%s' er ikke deklarert",
    1002: "Tegn-kodingen '%s' er ikke støttet",
    1003: "Denne navnerom-syntaksen er foreldet",
    1005: "Tegn nummer '%d' i tegn-referansen er ikke støttet",
    1006: "Element '%s' har attributt-liste, men er ikke deklarert",
    1007: "Attributt '%s' deklarert flere ganger",
    1008: "Tvetydig innholds-modell",

    # --- Namespace warnings: 1900-1999
    1900: "Navnerommets prefiks-navn kan ikke inneholde kolon",
    1901: "Navnerommets URI kan ikke være tomt",
    1902: "Navnerommets prefiks er ikke deklarert",
    1903: "Attributt-navn ikke unike etter navneroms-prosessering",
    
    # --- Validity errors: 2000-2999
    2000: "Faktisk verdi til attributtet '%s' er ikke lik #FIXED-verdien",
    2001: "Elementet '%s' er ikke tillatt her",
    2002: "Dokumentets rot-element '%s' er ikke det samme som det deklarerte",
    2003: "Element-typen '%s' er ikke deklarert",
    2004: "Elementet '%s' avsluttet, men innholdet ikke ferdig",
    2005: "Tekst-data er ikke tillatt i dette elementets innhold",
    2006: "Attributtet '%s' er ikke deklarert",
    2007: "ID-en '%s' brukt mer enn en gang",
    2008: "Bare uparserte entiteter er tillatt som verdier til ENTITY-attributter",
    2009: "Notasjonen '%s' er ikke deklarert",
    2010: "Påkrevd attributt '%s' mangler",
    2011: "IDREF viste til ikke-eksisterende ID '%s'",
    2012: "Elementet '%s' deklarert mer enn en gang",
    2013: "Bare ett ID-attributt er tillatt pr element-type",
    2014: "ID-attributter kan ikke være #FIXED eller ha standard-verdier",
    2015: "xml:space må deklareres som en oppramstype",
    2016: "xml:space må ha verdiene 'default' og 'preserve'",
    2017: "'%s' er ikke en gyldig verdi for '%s'-attributtet",
    2018: "Verdien til '%s'-attributtet må være et gyldig navn",
    2019: "Verdien til '%s'-attributtet er ikke et gyldig NMTOKEN",
    2020: "Verdien til '%s'-attributtet er ikke et gyldig NMTOKENS",
    2021: "Symbolet '%s' i verdien til '%s'-attributtet er ikke et gyldig navn",
    2022: "Notasjons-attributtet '%s' bruker en notasjon '%s' som ikke er deklarert",
    2023: "Uparsert entitet '%s' bruker en notasjon '%s' som ikke er deklarert",

    # --- Well-formedness errors: 3000-3999
    # From xmlutils
    3000: "Kunne ikke åpne '%s'",
    3001: "For tidlig slutt på entiteten",
    3002: "Blanke forventet her",
    3003: "Matchet ikke '%s'",   ## FIXME: This must be redone
    3004: "En av %s eller '%s' forventet",
    3005: "'%s' forventet",

    # From xmlproc.XMLCommonParser
    3006: "SYSTEM eller PUBLIC forventet",
    3007: "Tekst-deklarasjonen må stå først i entiteten",
    3008: "XML-deklarasjonen må stå først i dokumentet",
    3009: "Flere tekst-deklarasjoner i samme entitet",
    3010: "Flere tekst-deklarasjoner i samme dokument",
    3011: "XML-versjonen mangler på XML-deklarasjonen",
    3012: "'Standalone'-deklarasjon på tekst-deklarasjon ikke tillatt",

    # From xmlproc.XMLProcessor
    3013: "Syntaksfeil",
    3014: "Dokumentet slutter for tidlig, elementet '%s' er ikke lukket",
    3015: "Dokumentet slutter for tidlig, rot-elementet mangler",
    3016: "Attributtet '%s' gjentatt",
    3017: "Kun ett rot-element er tillatt",
    3018: "Ulovlig tegn nummer '%d' i tegn-referanse",
    3019: "Entitets-rekursjon oppdaget",
    3020: "Eksterne entitets-referanser ikke tillatt i attributt-verdier",
    3021: "Entiteten '%s' er ikke deklarert",
    3022: "'<' er ikke tillatt i attributt-verdier",
    3023: "Slutt-tagg for '%s', men '%s' forventet",
    3024: "Elementet '%s' lukket, men ikke åpent",
    3025: "']]>' ikke tillatt i tekst-data",
    3027: "Ikke et gyldig tegn-nummer",
    3028: "Tegn-referanser ikke tillatt utenfor rot-elementet",
    3029: "Tekst-data ikke tillatt utenfor rot-elementet",
    3030: "Entitets-referanser ikke tillatt utenfor rot-elementet",
    3031: "Referanser til uparserte entiteter er ikke tillatt i element-innhold",
    3032: "Mer enn en dokument-type-deklarasjon",
    3033: "Dokument-type-deklarasjon kun tillatt før rot-elementet",
    3034: "Det interne DTD-subsettet slutter for tidlig",
    3042: "Element krysset entitets-grense",
    3045: "Processing instruction navn som begynner med 'xml' er reservert",
    3046: "Denne XML-versjonen er ikke støttet",

    # From xmlproc.DTDParser
    3035: "Parameter-entiteter kan ikke være uparserte",
    3036: "Parameter-entitets-referanser ikke tillatt inne i deklarasjoner i det interne DTD-subsettet",
    3037: "Eksterne entitets-referanser ikke tillatt i entitetsdeklarasjoner",
    3038: "Parameter-entiteten '%s' ikke deklarert",
    3039: "Forventet attributt-type eller liste av alternativer",
    3040: "Valg- og sekvens-lister kan ikke blandes",
    3041: "'Conditional sections' er ikke tillatt i det interne DTD-subsettet",
    3043: "'Conditional section' ikke lukket",
    3044: "Symbolet '%s' er definert mer enn en gang",

    # From regular expressions that were not matched
    3900: "Ikke et gyldig navn",
    3901: "Ikke et gyldig versjonsnummer (%s)",
    3902: "Ikke et gyldig tegnkodings-navn",
    3903: "Ikke en gyldig kommentar",
    3905: "Ikke et gyldig heksadesimalt tall",
    3906: "Ikke et gyldig tall",
    3907: "Ikke en gyldig parameter-entitets-referanse",
    3908: "Ikke en gyldig attributt-type",
    3909: "Ikke en gyldig attributt-standard-verdi",
    3910: "Ikke en gyldig verdi for opprams-attributter",
    3911: "Ikke en gyldig verdi for 'standalone'",
    
    # --- Internal errors: 4000-4999
    4000: "Intern feil: Entitets-stakken korrupt.",
    4001: "Intern feil: Entitets-referanse forventet.",
    4002: "Intern feil: Ukjent feilmelding.",
    4003: "Intern feil: Eksterne parameter-entiteter ikke tillatt i deklarasjoner",
    # --- XCatalog errors: 5000-5099
    5000: "Ukjent XCatalog-element: %s.",
    5001: "Påkrevd XCatalog-attributt %s på %s mangler.",
     
    # --- SOCatalog errors: 5100-5199
    5100: "Ugyldig eller ikke støttet konstruksjon: %s.",
    }

# Updating the error hash

add_error_list("en",english)
add_error_list("no",norsk)
