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
    ARTIFICIAL  = 2,
    CATEGORIC   = 3,
    SKILL       = 4,
    CONDITION   = 5 ;
  
  final public static int
    EFFORTLESS_DC  = -10,
    TRIVIAL_DC     = -5 ,
    SIMPLE_DC      =  0 ,
    ROUTINE_DC     =  5 ,
    TRICKY_DC      =  10,
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
    
    ALL_ATTRIBUTES[] = Trait.skillsSoFar() ;
  
  final public static Skill
    SCENTING     = new Skill("Scenting"    , FORM_INSTINCT, INSIGHT),
    LIMB_AND_MAW = new Skill("Limb and Maw", FORM_INSTINCT, REFLEX ),
    NESTING      = new Skill("Nesting"     , FORM_INSTINCT, INSIGHT),
    MIMESIS      = new Skill("Mimesis"     , FORM_INSTINCT, REFLEX ),
    
    ALL_INSTINCTS[] = Trait.skillsSoFar() ;
  
  final public static Skill
    CULTIVATION    = new Skill("Cultivation"   , FORM_COGNITIVE, INTELLECT),
    XENOBIOLOGY    = new Skill("Xenobiology"   , FORM_COGNITIVE, INTELLECT),
    GEOPHYSICS     = new Skill("Geophysics"    , FORM_COGNITIVE, INTELLECT),
    LIFE_SUPPORT   = new Skill("Life Support"  , FORM_COGNITIVE, INTELLECT),

    ASSEMBLY       = new Skill("Assembly"      , FORM_COGNITIVE, INTELLECT),
    FIELD_THEORY   = new Skill("Field Theory"  , FORM_COGNITIVE, INTELLECT),
    CHEMISTRY      = new Skill("Chemistry"     , FORM_COGNITIVE, INTELLECT),
    GENE_CULTURE   = new Skill("Gene Culture"  , FORM_COGNITIVE, INTELLECT),
    
    PHARMACY       = new Skill("Pharmacy"      , FORM_COGNITIVE, INTELLECT),
    ANATOMY        = new Skill("Anatomy"       , FORM_COGNITIVE, INTELLECT),
    LOGOS_MENSA    = new Skill("Logos Mensa"   , FORM_COGNITIVE, INTELLECT),
    ADMINISTRATION = new Skill("Administration", FORM_COGNITIVE, INTELLECT),
    
    WOOD_AND_DAUB  = new Skill("Wood and Daub" , FORM_COGNITIVE, WILL     ),
    PHEREMONIST    = new Skill("Pheremonist"   , FORM_COGNITIVE, WILL     ),
    SUBSTANTIATION = new Skill("Substantiation", FORM_COGNITIVE, WILL     ),
    ANCIENT_LORE   = new Skill("Ancient Lore"  , FORM_COGNITIVE, WILL     ),
    
    ALL_COGNITIVE[] = Trait.skillsSoFar() ;
  
  final public static Skill
    COMMAND           = new Skill("Command"          , FORM_SENSITIVE, INSIGHT),
    SUASION           = new Skill("Suasion"          , FORM_SENSITIVE, INSIGHT),
    COUNSEL           = new Skill("Counsel"          , FORM_SENSITIVE, INSIGHT),
    
    SURVEILLANCE      = new Skill("Surveillance"     , FORM_SENSITIVE, INSIGHT),
    STEALTH_AND_COVER = new Skill("Stealth and Cover", FORM_SENSITIVE, INSIGHT),
    BATTLE_TACTICS    = new Skill("Battle Tactics"   , FORM_SENSITIVE, INSIGHT),
    
    NATIVE_TABOO      = new Skill("Native Taboo"     , FORM_SENSITIVE, INSIGHT),
    COMMON_CUSTOM     = new Skill("Common Custom"    , FORM_SENSITIVE, INSIGHT),
    NOBLE_ETIQUETTE   = new Skill("Noble Etiquette"  , FORM_SENSITIVE, INSIGHT),
    
    GRAPHIC_MEDIA     = new Skill("Graphic Media"    , FORM_SENSITIVE, WILL   ),
    
    ALL_SENSITIVE[] = Trait.skillsSoFar() ;
  
  final public static Skill
    MARKSMANSHIP      = new Skill("Marksmanship"     , FORM_PHYSICAL, REFLEX),
    CLOSE_COMBAT      = new Skill("Close Combat"     , FORM_PHYSICAL, REFLEX),
    SHIELD_AND_ARMOUR = new Skill("Shield and Armour", FORM_PHYSICAL, REFLEX),
    
    ATHLETICS         = new Skill("Athletics"        , FORM_PHYSICAL, REFLEX),
    PILOTING          = new Skill("Piloting"         , FORM_PHYSICAL, REFLEX),
    ASTROGATION       = new Skill("Astrogation"      , FORM_PHYSICAL, REFLEX),
    
    CARNAL_PLEASURE   = new Skill("Carnal Pleasure"  , FORM_PHYSICAL, REFLEX),
    MUSIC_AND_SONG    = new Skill("Music and Song"   , FORM_PHYSICAL, REFLEX),
    DISGUISE          = new Skill("Disguise"         , FORM_PHYSICAL, REFLEX),
    
    HARD_LABOUR       = new Skill("Hard Labour"      , FORM_PHYSICAL, WILL  ),
    DOMESTIC_SERVICE  = new Skill("Domestic Service" , FORM_PHYSICAL, WILL  ),
    
    ALL_PHYSICAL[] = Trait.skillsSoFar() ;
  
  final public static Skill
    SUGGESTION   = new Skill("Suggestion"  , FORM_PSYONIC, WILL),
    SYNESTHESIA  = new Skill("Synesthesia" , FORM_PSYONIC, WILL),
    METABOLISM   = new Skill("Metabolism"  , FORM_PSYONIC, WILL),
    TRANSDUCTION = new Skill("Transduction", FORM_PSYONIC, WILL),
    PROJECTION   = new Skill("Projection"  , FORM_PSYONIC, WILL),
    PREMONITION  = new Skill("Premonition" , FORM_PSYONIC, WILL),
    
    ALL_PYSONIC[] = Trait.skillsSoFar() ;
  
  
  
  public static Trait
    
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
      "Iritable",
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
      "Cheerful",
      "Optimistic",
      "Hopeful",
      null,
      "Doubtful",
      "Pessimistic",
      "Depressed"
    ),
    INQUISITIVE = new Trait(PERSONALITY,
      "Hunger for Knowledge",
      "Inquisitive",
      "Curious",
      null,
      "Dull",
      "Disinterested",
      "No Imagination"
    ),
    //
    //  TODO:  Dispose of this and create a separate 'creativity' trait?
    HUNGRY = new Trait(PERSONALITY,
      "Gluttonous",
      "Hungry",
      "Big Appetite",
      null,
      "Frugal",
      "No Appetite",
      "Wasting Away"
    ),
    
    
    STUBBORN = new Trait(PERSONALITY,
      "Obstinate",
      "Stubborn",
      "Persistent",
      null,
      "Spontaneous",
      "Impulsive",
      "Fickle"
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
    ACQUISITIVE = new Trait(PERSONALITY,
      "Avaricious",
      "Acquisitive",
      "Thrifty",
      null,
      "Generous",
      "Extravagant",
      "Profligate"
    ),
    DUTIFUL = new Trait(PERSONALITY,
      "Obedient",
      "Dutiful",
      "Deferential",
      null,
      "Defiant",
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
    HONOURABLE = new Trait(PERSONALITY,
      "Unimpeachable",
      "Honourable",
      "Trusty",
      null,
      "Shady",
      "Duplicitous",
      "Manipulative"
    ),
    EMPATHIC = new Trait(PERSONALITY,
      "Martyr Complex",
      "Empathic",
      "Sympathetic",
      null,
      "Cruel",
      "Sadistic",
      "Monstrous"
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
    DEBAUCHED = new Trait(PERSONALITY,
      "Debauched",
      "Lusty",
      "Fun",
      null,
      "Moderate",
      "Abstinent",
      "Frigid"
    ),
    
    
    FEMININE = new Trait(PHYSICAL,
      "Pneumatic",
      "Curvaceous",
      "Feminine",
      null,
      "Manly",
      "Bearded",
      "Hirsute"
    ),
    ORIENTATION = new Trait(PHYSICAL,
      "Heterosexual",
      "Bisexual",
      "Homosexual"
    ),
    GENDER = new Trait(PHYSICAL,
      "Female",
      null,
      "Male"
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
      "Very Tall",
      "Tall",
      null,
      "Short",
      "Very Short",
      "Dwarfish"
    ),
    STOUT = new Trait(PHYSICAL,
      "Rotund",
      "Stocky",
      "Stout",
      null,
      "Lithe",
      "Lean",
      "Gaunt"
    ),
    
    
    DESERT_BLOOD = new Trait(PHYSICAL,
      "Desert Blood",
      null
    ),
    TUNDRA_BLOOD = new Trait(PHYSICAL,
      "Tundra Blood",
      null
    ),
    FOREST_BLOOD = new Trait(PHYSICAL,
      "Forest Blood",
      null
    ),
    WASTES_BLOOD = new Trait(PHYSICAL,
      "Wastes Blood",
      null
    ),
    MUTATION = new Trait(PHYSICAL,
      "Severe Mutation",
      "Major Mutation",
      "Slight Mutation",
      null
    ),
    GIFTED = new Trait(PHYSICAL,
      "Prodigiously Gifted",
      "Highly Gifted",
      "Gifted",
      null
    )
  ;

  final public static Trait
    ALL_TRAIT_TYPES[] = Trait.from(Trait.allSkills) ;
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


