/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package src.game.actors ;

import src.util.Table;



public interface SkillsAndTraits {
  
  
  final public static int
    PERSONALITY = 0,
    PHYSICAL    = 1,
    CATEGORIC   = 2,
    ARTIFICIAL  = 3,
    SKILL       = 4,
    CONDITION   = 5 ;
  
  final public static int
    EFFORTLESS_DC  = -10,
    TRIVIAL_DC     = -5 ,
    SIMPLE_DC      =  0 ,
    ROUTINE_DC     =  5 ,
    MODERATE_DC    =  10,
    DIFFICULT_DC   =  15,
    STRENUOUS_DC   =  20,
    PUNISHING_DC   =  25,
    IMPOSSIBLE_DC  =  30 ;
  
  final static int
    FORM_NATURAL   = 0,
    FORM_PHYSICAL  = 1,
    FORM_SENSITIVE = 2,
    FORM_COGNITIVE = 3,
    FORM_PSYONIC   = 4,
    FORM_INSTINCT  = 5 ;
    
  
  

  //  Nanotech/Informatics?  Introspection?  Specific weapon skills?
  
  final public static Skill
    //
    //  TODO:  Simplify these to just 3 attributes?  Physique/senses/cognition?
    VIGOUR    = new Skill("Vigour"   , FORM_NATURAL, null),
    BRAWN     = new Skill("Brawn"    , FORM_NATURAL, null),
    REFLEX    = new Skill("Reflex"   , FORM_NATURAL, null),
    INSIGHT   = new Skill("Insight"  , FORM_NATURAL, null),
    INTELLECT = new Skill("Intellect", FORM_NATURAL, null),
    WILL      = new Skill("Will"     , FORM_NATURAL, null),
    
    ATTRIBUTES[] = Trait.skillsSoFar() ;
  
  final public static Skill
    //
    //  For the benefit of animals and non-human species-
    SCENTING       = new Skill("Scenting"      , FORM_INSTINCT, INSIGHT  ),
    LIMB_AND_MAW   = new Skill("Limb and Maw"  , FORM_INSTINCT, REFLEX   ),
    NESTING        = new Skill("Nesting"       , FORM_INSTINCT, INSIGHT  ),
    MIMESIS        = new Skill("Mimesis"       , FORM_INSTINCT, REFLEX   ),
    PHEREMONIST    = new Skill("Pheremonist"   , FORM_INSTINCT, WILL     ),
    IMMANENCE      = new Skill("Immanence"     , FORM_INSTINCT, INTELLECT),
    
    INSTINCT_SKILLS[] = Trait.skillsSoFar() ;
  
  final public static Skill
    //
    //  Artifice-related skills:
    ASSEMBLY       = new Skill("Assembly"      , FORM_COGNITIVE, INTELLECT),
    CHEMISTRY      = new Skill("Chemistry"     , FORM_COGNITIVE, INTELLECT),
    FIELD_THEORY   = new Skill("Field Theory"  , FORM_COGNITIVE, INTELLECT),
    ASTROGATION    = new Skill("Astrogation"   , FORM_COGNITIVE, INTELLECT),
    //
    //  Ecology-related skills:
    HANDICRAFTS    = new Skill("Handicrafts"   , FORM_COGNITIVE, INTELLECT),
    CULTIVATION    = new Skill("Cultivation"   , FORM_COGNITIVE, INTELLECT),
    XENOZOOLOGY    = new Skill("Xenozoology"   , FORM_COGNITIVE, INTELLECT),
    GEOPHYSICS     = new Skill("Geophysics"    , FORM_COGNITIVE, INTELLECT),
    //
    //  Physician-related skills:
    PHARMACY       = new Skill("Pharmacy"      , FORM_COGNITIVE, INTELLECT),
    ANATOMY        = new Skill("Anatomy"       , FORM_COGNITIVE, INTELLECT),
    GENE_CULTURE   = new Skill("Gene Culture"  , FORM_COGNITIVE, INTELLECT),
    PSYCHOANALYSIS = new Skill("Psychoanalysis", FORM_COGNITIVE, INTELLECT),
    //
    //  Research and governance-related.
    ANCIENT_LORE   = new Skill("Ancient Lore"  , FORM_COGNITIVE, INTELLECT),
    BATTLE_TACTICS = new Skill("Battle Tactics", FORM_COGNITIVE, INTELLECT),
    ACCOUNTING     = new Skill("Accounting"    , FORM_COGNITIVE, INTELLECT),
    
    COGNITIVE_SKILLS[] = Trait.skillsSoFar() ;
  
  final public static Skill
    //
    //  Methods of persuasion-
    COMMAND           = new Skill("Command"        , FORM_SENSITIVE, INSIGHT),
    SUASION           = new Skill("Suasion"        , FORM_SENSITIVE, INSIGHT),
    COUNSEL           = new Skill("Counsel"        , FORM_SENSITIVE, INSIGHT),
    //
    //  Knowing the language and culture-
    NATIVE_TABOO      = new Skill("Native Taboo"   , FORM_SENSITIVE, INSIGHT),
    COMMON_CUSTOM     = new Skill("Common Custom"  , FORM_SENSITIVE, INSIGHT),
    NOBLE_ETIQUETTE   = new Skill("Noble Etiquette", FORM_SENSITIVE, INSIGHT),
    //
    //  Forms of artistic expression/entertainment-
    EROTICS           = new Skill("Erotics"        , FORM_SENSITIVE, REFLEX ),
    MASQUERADE        = new Skill("Masquerade"     , FORM_SENSITIVE, REFLEX ),
    MUSIC_AND_SONG    = new Skill("Music and Song" , FORM_SENSITIVE, INSIGHT),
    GRAPHIC_MEDIA     = new Skill("Graphic Media"  , FORM_SENSITIVE, INSIGHT),
    
    SENSITIVE_SKILLS[] = Trait.skillsSoFar() ;
  
  final public static Skill
    //
    //  Direct combat skills-
    FORMATION_COMBAT  = new Skill("Formation Combat" , FORM_PHYSICAL, WILL  ),
    MARKSMANSHIP      = new Skill("Marksmanship"     , FORM_PHYSICAL, REFLEX),
    HAND_TO_HAND      = new Skill("Hand to Hand"     , FORM_PHYSICAL, REFLEX),
    SHIELD_AND_ARMOUR = new Skill("Shield and Armour", FORM_PHYSICAL, REFLEX),
    //
    //  Exploration and mobility-
    ATHLETICS         = new Skill("Athletics"        , FORM_PHYSICAL, WILL  ),
    PILOTING          = new Skill("Piloting"         , FORM_PHYSICAL, REFLEX),
    SURVEILLANCE      = new Skill("Surveillance"     , FORM_PHYSICAL, REFLEX),
    STEALTH_AND_COVER = new Skill("Stealth and Cover", FORM_PHYSICAL, REFLEX),
    //
    //  General elbow grease-
    HARD_LABOUR       = new Skill("Hard Labour"      , FORM_PHYSICAL, WILL  ),
    DOMESTICS         = new Skill("Domestics"        , FORM_PHYSICAL, WILL  ),
    
    PHYSICAL_SKILLS[] = Trait.skillsSoFar() ;
  
  final public static Skill
    SUGGESTION   = new Skill("Suggestion"  , FORM_PSYONIC, WILL),
    SYNESTHESIA  = new Skill("Synesthesia" , FORM_PSYONIC, WILL),
    METABOLISM   = new Skill("Metabolism"  , FORM_PSYONIC, WILL),
    TRANSDUCTION = new Skill("Transduction", FORM_PSYONIC, WILL),
    PROJECTION   = new Skill("Projection"  , FORM_PSYONIC, WILL),
    PREMONITION  = new Skill("Premonition" , FORM_PSYONIC, WILL),
    
    PYSONIC_SKILLS[] = Trait.skillsSoFar() ;
  
  
  
  public static Trait
    
    //
    //  These are the listings of personality traits.  These can be modified
    //  over time based on experience, peer pressure or conditioning.  Genetic
    //  factors also influence their expression.  (TODO:  Implement that.)
    //
    //  I've divided these into 3 main categories-
    //    Basic Impulses (emotional drives or physical needs)
    //    Meta-Decisional (modify the general process of plan-selection)
    //    Cultural/Ethical (overall social values)
    
    //
    //  BASIC IMPULSES-
    NERVOUS = new Trait(PERSONALITY,
      "Cowardly",
      "Nervous",
      "Cautious",
      null,
      "Brave",
      "Fearless",
      "Reckless"
    ),
    AGGRESSIVE = new Trait(PERSONALITY,
      "Vengeful",
      "Aggressive",
      "Defensive",
      null,
      "Calm",
      "Gentle",
      "Pacifist"
    ),
    FRIENDLY = new Trait(PERSONALITY,
      "Fawning",
      "Complimentary",
      "Friendly",
      null,
      "Reserved",
      "Critical",
      "Caustic"
    ),
    OPTIMISTIC = new Trait(PERSONALITY,
      "Blithe",
      "Optimistic",
      "Cheerful",
      null,
      "Skeptical",
      "Pessimistic",
      "Morose"
    ),
    DEBAUCHED = new Trait(PERSONALITY,
      "Debauched",
      "Lusty",
      "Fun",
      null,
      "Temperate",
      "Abstinent",
      "Frigid"
    ),
    APPETITE = new Trait(PERSONALITY,
      "Gluttonous",
      "Big Appetite",
      "Gourmand",
      null,
      "Frugal",
      "Small Appetite",
      "No Appetite"
    ),
    
    //
    //  META-DECISIONAL-
    STUBBORN = new Trait(PERSONALITY,
      "Obstinate",
      "Stubborn",
      "Persistent",
      null,
      "Spontaneous",
      "Impulsive",
      "Fickle"
    ),
    INQUISITIVE = new Trait(PERSONALITY,
      "Insatiably Curious",
      "Inquisitive",
      "Curious",
      null,
      "Stolid",
      "Disinterested",
      "Dull"
    ),
    SOCIABLE = new Trait(PERSONALITY,
      "Gregarious",
      "Sociable",
      "Open",
      null,
      "Private",
      "Solitary",
      "Withdrawn"
    ),
    DUTIFUL = new Trait(PERSONALITY,
      "Obedient",
      "Dutiful",
      "Respectful of Betters",
      null,
      "Independant",
      "Rebellious",
      "Anarchic"
    ),
    IMPASSIVE = new Trait(PERSONALITY,
      "Emotionless",
      "Impassive",
      "Rational",
      null,
      "Passionate",
      "Excitable",
      "Manic"
    ),
    INDOLENT = new Trait(PERSONALITY,
      "Lethargic",
      "Indolent",
      "Relaxed",
      null,
      "Busy",
      "Restless",
      "Workaholic"
    ),
    
    //
    //  CULTURAL/ETHICAL-
    TRADITIONAL = new Trait(PERSONALITY,
      "Hidebound",
      "Traditional",
      "Old-fashioned",
      null,
      "Reformist",
      "Radical",
      "Subversive"
    ),
    NATURALIST = new Trait(PERSONALITY,
      "Gone Feral",
      "Ecophile",
      "Naturalist",
      null,
      "Urbanist",
      "Industrialist",
      "Antiseptic"
    ),
    ACQUISITIVE = new Trait(PERSONALITY,
      "Avaricious",
      "Acquisitive",
      "Thrifty",
      null,
      "Generous",
      "Extravagant",
      "Profligate"
    ),
    AMBITIOUS = new Trait(PERSONALITY,
      "Narcissist",
      "Ambitious",
      "Proud",
      null,
      "Modest",
      "Humble",
      "Complacent"
    ),
    HONOURABLE = new Trait(PERSONALITY,
      "Unimpeachable",
      "Honourable",
      "Trustworthy",
      null,
      "Sly",
      "Dishonest",
      "Manipulative"
    ),
    EMPATHIC = new Trait(PERSONALITY,
      "Martyr Complex",
      "Compassionate",
      "Sympathetic",
      null,
      "Hard",
      "Cruel",
      "Sadistic"
    ),
    PERSONALITY_TRAITS[] = Trait.traitsSoFar(),
    
    
    //
    //  These are the listings for physical traits.  Physical traits are
    //  determined at birth and cannot be modified (except perhaps surgically),
    //  but do wax and wane based on aging, in a fashion similar to basic
    //  attributes.  TODO:  Implement that.
    
    FEMININE = new Trait(PHYSICAL,
      "Busty",
      "Curvy",
      "Gamine",
      null,
      "Boyish",
      "Bearded",
      "Hirsute"
    ),
    HANDSOME = new Trait(PHYSICAL,
      "Stunning",
      "Beautiful",
      "Handsome",
      null,
      "Plain",
      "Ugly",
      "Hideous"
    ),
    TALL = new Trait(PHYSICAL,
      "Towering",
      "Looming",
      "Tall",
      null,
      "Short",
      "Small",
      "Diminutive"
    ),
    STOUT = new Trait(PHYSICAL,
      "Rotund",
      "Stout",
      "Sturdy",
      null,
      "Lithe",
      "Lean",
      "Gaunt"
    ),
    GIFTED = new Trait(PHYSICAL,
      "Prodigiously Gifted",
      "Highly Gifted",
      "Gifted",
      null
    ),
    PHYSICAL_TRAITS[] = Trait.traitsSoFar(),
    
    //
    //  Categoric traits are qualitative physical traits unaffected by aging.
    ORIENTATION = new Trait(CATEGORIC,
      "Heterosexual",
      "Bisexual",
      "Homosexual",
      null
    ),
    GENDER = new Trait(CATEGORIC,
      "Female",
      null,
      "Male"
    ),
    DESERT_BLOOD = new Trait(CATEGORIC,
      "Desert Blood", // "Desertborn", "Dark"
      null
    ),
    TUNDRA_BLOOD = new Trait(CATEGORIC,
      "Tundra Blood", // "Tundraborn", "Sallow"
      null
    ),
    FOREST_BLOOD = new Trait(CATEGORIC,
      "Forest Blood", //  "Forestborn", "Tan"
      null
    ),
    WASTES_BLOOD = new Trait(CATEGORIC,
      "Wastes Blood", //  "Wastesborn", "Pale"
      null
    ),
    BLOOD_TRAITS[] = {
      DESERT_BLOOD, TUNDRA_BLOOD, FOREST_BLOOD, WASTES_BLOOD
    },
    MUTATION = new Trait(CATEGORIC,
      "Severe Mutation",
      "Major Mutation",
      "Slight Mutation",
      null
    ),
    CATEGORIC_TRAITS[] = Trait.traitsSoFar() ;
  

  
  final static int
    SHORT_LATENCY  = 1,
    MEDIUM_LATENCY = 10,
    LONG_LATENCY   = 100,
    
    NO_SPREAD    = 0,
    SLOW_SPREAD  = 2,
    RAPID_SPREAD = 5,
    
    MINIMAL_VIRULENCE   = 5,
    MILD_VIRULENCE      = 10,
    MODERATE_VIRULENCE  = 15,
    HIGH_VIRULENCE      = 20,
    EXTREME_VIRULENCE   = 25,
    INCURABLE_VIRULENCE = 30 ;
  
  final public static Condition
    
    //
    //  Finally, listings for various conditions that might beset the actor-
    INJURY = new Condition(
      "Critical Injury",
      "Serious Injury",
      "Slight Injury",
      null
    ),
    FATIGUE = new Condition(
      "Extreme Fatigue",
      "Heavy Fatigue",
      "Mild Fatigue",
      null
    ),
    MORALE = new Condition(
      "Fantastic Mood",
      "Great Mood",
      "Fair Mood",
      null,
      "Poor Mood",
      "Awful Mood",
      "Wretched Mood"
    ),
    
    
    HUNGER = new Condition(
      "Near Starvation",
      "Hungry",
      "Peckish",
      null,
      "Full"
    ),
    MALNOURISHMENT = new Condition(
      "Malnourished",
      null
    ),
    POISONED = new Condition(
      "Fatally Poisoned",
      "Badly Poisoned",
      "Poisoned",
      null
    ),
    
    
    ILLNESS = new Condition(
      SHORT_LATENCY, MINIMAL_VIRULENCE, RAPID_SPREAD, Table.make(
        VIGOUR, -3, BRAWN, -3
      ),
      "Terminal Illness",
      "Serious Illness",
      "Mild Illness",
       null
       //  "Illness Immune"
    ),
    SOMA_HAZE = new Condition(
      SHORT_LATENCY, MINIMAL_VIRULENCE, NO_SPREAD, Table.make(
        REFLEX, -3, INTELLECT, -1, INSIGHT, 1
      ),
      "Soma Haze",
      "Soma Haze",
      "Soma Haze",
      null
      //  "Haze Immune"
    ),
    SPICE_ADDICTION = new Condition(
      LONG_LATENCY, MILD_VIRULENCE, NO_SPREAD, Table.make(
        VIGOUR, -10, BRAWN, -10, WILL, -5, INTELLECT, -5
      ),
      "Complete Spice Addiction",
      "Heavy Spice Addiction",
      "Mild Spice Addiction",
      null
      //  "Addiction Immune"
    ),
    CANCER = new Condition(
      LONG_LATENCY, MODERATE_VIRULENCE, NO_SPREAD, Table.make(
        VIGOUR, -20, BRAWN, -10
      ),
      "Terminal Cancer",
      "Advanced Cancer",
      "Early Cancer",
      null
      //  "Cancer Immune"
    ),
    RAGE_INFECTION = new Condition(
      SHORT_LATENCY, HIGH_VIRULENCE, RAPID_SPREAD, Table.make(
        VIGOUR, 5, BRAWN, 5, AGGRESSIVE, 5, INTELLECT, -15
      ),
      "Rage Frenzy",
      "Rage Fever",
      "Rage Onset",
      null
      //  "Rage Immune"
    ),
    HIREX_PARASITE = new Condition(
      MEDIUM_LATENCY, HIGH_VIRULENCE, SLOW_SPREAD, Table.make(
        INTELLECT, -5, REFLEX, -5, INSIGHT, -5, BRAWN, -5, HANDSOME, -5
      ),
      "Hirex Infestation",
      "Hirex Parasite",
      "Hirex Gestation",
      null
      //  "Hirex Immune"
    ),
    ALBEDAN_STRAIN = new Condition(
      MEDIUM_LATENCY, EXTREME_VIRULENCE, SLOW_SPREAD, Table.make(
        DEBAUCHED, 2, VIGOUR, 5, INSIGHT, 5, REFLEX, -5
      ),
      "Albedan Strain",
      "Albedan Strain",
      "Albedan Strain",
      null
      //  "Strain Immune"
    ),
    SILVERQUICK = new Condition(
      SHORT_LATENCY, INCURABLE_VIRULENCE, RAPID_SPREAD, Table.make(
        IMPASSIVE, 5, VIGOUR, -20, BRAWN, -20
      ),
      "Silverquick Ague",
      "Silverquick Scale",
      "Silverquick Taint",
      null
      //  "Silverquick Immune"
    ),
    MOEBIUS_PHASE = new Condition(
      LONG_LATENCY, INCURABLE_VIRULENCE, NO_SPREAD, Table.make(
        REFLEX, -20, BRAWN, -20
      ),
      "Moebius Sublimation",
      "Moebius Phase",
      "Moebius Displacement",
      null
      //  "Phase Immune"
    ),
    
    SPONTANEOUS_DISEASE[] = {
      ILLNESS, CANCER, HIREX_PARASITE
    },
    DISEASES[] = {
      ILLNESS, SOMA_HAZE, SPICE_ADDICTION,
      CANCER, RAGE_INFECTION, HIREX_PARASITE,
      ALBEDAN_STRAIN, SILVERQUICK, MOEBIUS_PHASE
    } ;
  final public static Trait CONDITIONS[] = Trait.traitsSoFar() ;
  
  final public static Trait
    ALL_TRAIT_TYPES[] = Trait.from(Trait.allTraits) ;
}





/*
  //  Logicians, Spacers, Initiates, Shapers, Collective and Symbiotes-
  //    Supercognitive, Primary/Secondary/Tertiary, Cyborg, Melded, Symbiote
  //  There are some extra traits lying around-
  //    Infected, Hypersensitive/Ultraphysical, Longevity.
  //  Each of the the major monster categories also has an identifying trait-
  //    Humanoid, Insectile, Silicate, Artilect, Browser and Predator.
  //  The three non-humanoid species also have a dedicated life-cycle-
  //    Sessile/Changeling/Blossom Node, Larva/Worker/Soldier/Queen, Jovian.
//*/






