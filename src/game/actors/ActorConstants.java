/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package src.game.actors ;



public interface ActorConstants {
  
  
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
  
  
  
  final public static Skill
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
    SCENTING       = new Skill("Scenting"      , FORM_INSTINCT, INSIGHT),
    LIMB_AND_MAW   = new Skill("Limb and Maw"  , FORM_INSTINCT, REFLEX ),
    NESTING        = new Skill("Nesting"       , FORM_INSTINCT, INSIGHT),
    MIMESIS        = new Skill("Mimesis"       , FORM_INSTINCT, REFLEX ),
    PHEREMONIST    = new Skill("Pheremonist"   , FORM_INSTINCT, WILL   ),
    SUBSTANTIATION = new Skill("Substantiation", FORM_INSTINCT, WILL   ),
    
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
    ADMINISTRATION = new Skill("Administration", FORM_COGNITIVE, INTELLECT),
    
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
    //  Forms of artistic expression-
    EROTIC_DANCE      = new Skill("Erotic Dance"   , FORM_SENSITIVE, REFLEX ),
    DISGUISE          = new Skill("Disguise"       , FORM_SENSITIVE, REFLEX ),
    MUSIC_AND_SONG    = new Skill("Music and Song" , FORM_SENSITIVE, INSIGHT),
    GRAPHIC_DESIGN    = new Skill("Graphic Design" , FORM_SENSITIVE, INSIGHT),
    
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
    HOUSEKEEPING      = new Skill("Housekeeping"     , FORM_PHYSICAL, WILL  ),
    
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
      "Irritable",
      null,
      "Calm",
      "Gentle",
      "Pacifist"
    ),
    LOVING = new Trait(PERSONALITY,
      "Faithful",
      "Loving",
      "Warm",
      null,
      "Cold",
      "Detached",
      "Callous"
    ),
    OPTIMISTIC = new Trait(PERSONALITY,
      "Blithe",
      "Optimistic",
      "Cheerful",
      null,
      "Doubtful",
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
      "Respectful",
      null,
      "Assured",
      "Rebellious",
      "Anarchist"
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
      "On Edge"
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
      "Extreme Ecophile",
      "Ecophile",
      "Naturalist",
      null,
      "Urbanist",
      "Industrialist",
      "Radical Industrialist"
    ),
    ACQUISITIVE = new Trait(PERSONALITY,
      "Avaricious",
      "Thrifty",
      "Prudent",
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
      "Tough",
      "Hard",
      "Cruel"
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
      "Feminine",
      null,
      "Masculine",
      "Hairy",
      "Hirsute"
    ),
    HANDSOME = new Trait(PHYSICAL,
      "Beautiful",
      "Handsome",
      "Pretty",
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
      "Desert Blood",
      null
    ),
    TUNDRA_BLOOD = new Trait(CATEGORIC,
      "Tundra Blood",
      null
    ),
    FOREST_BLOOD = new Trait(CATEGORIC,
      "Forest Blood",
      null
    ),
    WASTES_BLOOD = new Trait(CATEGORIC,
      "Wastes Blood",
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
    CATEGORIC_TRAITS[] = Trait.traitsSoFar(),
    
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
      null
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
    
    
    //
    //  TODO:  These need to apply their effects.
    ILLNESS = new Condition(
      "Terminal Illness",
      "Serious Illness",
      "Mild Illness",
       null
    ),
    CANCER = new Condition(
      "Terminal Cancer",
      "Advanced Cancer",
      "Early Cancer",
      null
    ),
    SPICE_ADDICTION = new Condition(
      "Complete Addiction",
      "Heavy Addiction",
      "Mild Addiction",
      null
    ),
    RAGE_INFECTION = new Condition(
      "Infection Frenzy",
      "Infection Fever",
      "Infection Onset",
      null
    ),
    ALBEDAN_STRAIN = new Condition(
      "Albedan Strain",
      "Albedan Strain",
      "Albedan Strain",
      null
    ),
    SILVER_PLAGUE = new Condition(
      "Silver Plague",
      "Silver Plague",
      "Silver Plague",
      null
    ),
    
    CONDITIONS[] = Trait.traitsSoFar(),
    DISEASES[] = { ILLNESS, CANCER, SPICE_ADDICTION }
  ;
  
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






