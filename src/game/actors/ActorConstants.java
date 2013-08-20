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
    
    ATTRIBUTES[] = Trait.skillsSoFar() ;
  
  final public static Skill
    SCENTING     = new Skill("Scenting"    , FORM_INSTINCT, INSIGHT),
    LIMB_AND_MAW = new Skill("Limb and Maw", FORM_INSTINCT, REFLEX ),
    NESTING      = new Skill("Nesting"     , FORM_INSTINCT, INSIGHT),
    MIMESIS      = new Skill("Mimesis"     , FORM_INSTINCT, REFLEX ),
    
    INSTINCT_SKILLS[] = Trait.skillsSoFar() ;
  
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
    
    COGNITIVE_SKILLS[] = Trait.skillsSoFar() ;
  
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
    
    SENSITIVE_SKILLS[] = Trait.skillsSoFar() ;
  
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
    STRESS = new Condition(
      "Terrible Morale",
      "Bad Morale",
      "Weak Morale",
      null,
      "Good Morale",
      "Strong Morale",
      "Superb Morale"
    ),
    
    
    HUNGER = new Condition(
      "Near Starvation",
      "Gnawing Hunger",
      "Hungry",
      null
    ),
    MALNOURISHMENT = new Condition(
      "Badly Malnourished",
      "Malnourished",
      "Slightly Malnourished",
      null
    ),
    
    
    POISONED = new Condition(
      "Fatally Poisoned",
      "Badly Poisoned",
      "Poisoned",
      null
    ),
    
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
    
    SPYCE_ADDICTION = new Condition(
      "Complete Addiction",
      "Heavy Addiction",
      "Mild Addiction",
      null
    ),
    /*
    RAGE_INFECTION = new Condition(
      "Infection Frenzy",
      "Infection Atavism",
      "Infection Onset",
      null
    ),
    
    PART_CYBORG = new Condition(
      "Part Cyborg"
    ),
    //*/
    CONDITIONS[] = Trait.traitsSoFar()
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



