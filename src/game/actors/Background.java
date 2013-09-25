/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.game.campaign.System ;
import src.util.* ;


//
//  TODO:  Backgrounds need to include their own descriptions.


public class Background implements BuildConstants {
  
  
  
  final public static Float
    ALWAYS    =  1.0f,
    OFTEN     =  0.6f,
    SOMETIMES =  0.3f,
    RARELY    = -0.7f,
    NEVER     = -1.0f ;
  final public static Integer
    LEARNING  = 0,
    NOVICE    = 5,
    PRACTICED = 10,
    EXPERT    = 15,
    MASTER    = 20 ;
  
  final public static int
    SLAVE_CLASS  =  0,
    LOWER_CLASS  =  1,
    MIDDLE_CLASS =  2,
    UPPER_CLASS  =  3,
    RULER_CLASS  =  4 ;
  final public static int
    GUILD_MILITANT = 0,
    GUILD_MERCHANT = 1,
    GUILD_AESTHETE = 2,
    GUILD_ARTIFICER = 3,
    GUILD_ECOLOGIST = 4,
    GUILD_PHYSICIAN = 5,
    NOT_A_GUILD = -1 ;
  
  final public static int HIRE_COSTS[] = {
    150, 250, 500, 1000, -1  //Represents 10 days' salary.  Can't hire nobles.
  } ;
  
  
  final protected static Object
    MAKES = new Object(),
    NEEDS = new Object() ;
  
  final static String COSTUME_DIR = "media/Actors/human/" ;
  
  private static int nextID = 0 ;
  final public int ID = nextID++ ;
  private static Batch <Background> all = new Batch() ;
  
  
  final public static Background
    //
    //  Natives can only be recruited locally, not from offworld.
    NATIVE_BIRTH = new Background(
      "Native Birth", "native_skin.gif", null, -1, NOT_A_GUILD,
      NOVICE, NATIVE_TABOO, LEARNING, HANDICRAFTS, MARKSMANSHIP, XENOZOOLOGY
    ),
    //
    //  The following are available to most actors as part of their careers-
    HIVES_BIRTH = new Background(
      "Hives Birth", "artificer_skin.gif", null, SLAVE_CLASS, NOT_A_GUILD,
      NOVICE, COMMON_CUSTOM, LEARNING, NATIVE_TABOO, HAND_TO_HAND, CHEMISTRY
    ),
    PYON_BIRTH = new Background(
      "Pyon Birth", "pyon_skin.gif", null, LOWER_CLASS, NOT_A_GUILD,
      NOVICE, COMMON_CUSTOM, LEARNING, HARD_LABOUR, DOMESTICS, ASSEMBLY
    ),
    FREE_BIRTH = new Background(
      "Free Birth", "citizen_skin.gif", null, MIDDLE_CLASS, NOT_A_GUILD,
      NOVICE, COMMON_CUSTOM, LEARNING, SUASION, MARKSMANSHIP, PILOTING
    ),
    GUILDER_BIRTH = new Background(
      "Guilder Birth", "vendor_skin.gif", null, UPPER_CLASS, NOT_A_GUILD,
      NOVICE, COMMON_CUSTOM, LEARNING, NOBLE_ETIQUETTE, ADMINISTRATION, COUNSEL
    ),
    //
    //  Highborn are not available as normally-generated citizens, only as
    //  visiting NPCs or members of your household.
    HIGH_BIRTH = new Background(
      "High Birth", "highborn_male_skin.gif", null, RULER_CLASS,
      NOVICE, NOBLE_ETIQUETTE, LEARNING, COMMAND, HAND_TO_HAND, ANCIENT_LORE
    ),
    
    OPEN_CLASSES[] = { HIVES_BIRTH, PYON_BIRTH, FREE_BIRTH, GUILDER_BIRTH } ;
  
  
  
  
  final public static Background
    
    EXCAVATOR = new Background(
      "Excavator", "pyon_skin.gif", null,
      LOWER_CLASS, GUILD_ARTIFICER,
      EXPERT, HARD_LABOUR, NOVICE, GEOPHYSICS, ASSEMBLY, CHEMISTRY,
      OFTEN, STUBBORN, RARELY, NERVOUS, HANDSOME
    ),
    
    TECHNICIAN = new Background(
      "Technician", "artificer_skin.gif", "artificer_portrait.png",
      LOWER_CLASS, GUILD_ARTIFICER,
      PRACTICED, ASSEMBLY, HARD_LABOUR, NOVICE, FIELD_THEORY,
      SOMETIMES, DUTIFUL, RARELY, INDOLENT
    ),
    
    CORE_TECHNICIAN = new Background(
      "Core Technician", "citizen_skin.gif", "artificer_portrait.png",
      MIDDLE_CLASS, GUILD_ARTIFICER,
      EXPERT, FIELD_THEORY, PRACTICED, CHEMISTRY, ASSEMBLY,
      NOVICE, SHIELD_AND_ARMOUR,
      OFTEN, DUTIFUL, SOMETIMES, NERVOUS,
      SEALSUIT
    ),
    
    ARTIFICER = new Background(
      "Artificer", "artificer_skin.gif", "artificer_portrait.png",
      UPPER_CLASS, GUILD_ARTIFICER,
      EXPERT, ASSEMBLY, PRACTICED, FIELD_THEORY, SHIELD_AND_ARMOUR,
      NOVICE, ANCIENT_LORE, CHEMISTRY,
      SOMETIMES, INQUISITIVE, RARELY, NATURALIST
    ),
    
    ARTIFICER_CIRCLES[] = { EXCAVATOR, TECHNICIAN, CORE_TECHNICIAN, ARTIFICER }
  ;

  final public static Background
    
    MINDER = new Background(
      "Minder", "citizen_skin.gif", null,
      LOWER_CLASS, GUILD_PHYSICIAN,
      PRACTICED, DOMESTICS, SUASION, NOVICE, ANATOMY, PHARMACY, COUNSEL,
      OFTEN, EMPATHIC, SOMETIMES, STUBBORN
    ),
    
    VAT_BREEDER = new Background(
      "Vat Breeder", "citizen_skin.gif", null,
      MIDDLE_CLASS, GUILD_PHYSICIAN,
      PRACTICED, GENE_CULTURE, PHARMACY, CHEMISTRY,
      RARELY, DEBAUCHED, INDOLENT
    ),
    
    ARCHIVIST = new Background(
      "Archivist", "citizen_skin.gif", null,
      MIDDLE_CLASS, GUILD_PHYSICIAN,
      EXPERT, ADMINISTRATION, ANCIENT_LORE, NOVICE, COUNSEL, ASSEMBLY,
      ALWAYS, INQUISITIVE, SOMETIMES, NERVOUS, IMPASSIVE
    ),
    
    //  TODO:  Also include the Psychoanalyst
    /*
    MONITOR = new Background(
      "Monitor", "citizen_skin.gif", null,
      MIDDLE_CLASS,
      EXPERT, COUNSEL, PRACTICED, SUASION, COMMAND, PSYCHOANALYSIS,
      NOVICE, COMMON_CUSTOM, ADMINISTRATION,
      OFTEN, EMPATHIC, TRADITIONAL, RARELY, DEBAUCHED
    ),
    //*/
    
    PHYSICIAN = new Background(
      "Physician", "physician_skin.gif", "physician_portrait.png",
      UPPER_CLASS, GUILD_PHYSICIAN,
      EXPERT, ANATOMY, PHARMACY,
      PRACTICED, GENE_CULTURE, PSYCHOANALYSIS, COUNSEL, SUASION,
      OFTEN, INQUISITIVE, SOMETIMES, HONOURABLE, IMPASSIVE, RARELY, DEBAUCHED
    ),
    
    PHYSICIAN_CIRCLES[] = { MINDER, VAT_BREEDER, ARCHIVIST, PHYSICIAN }
  ;
  
  final public static Background
    
    FIELD_HAND = new Background(
      "Field Hand", "pyon_skin.gif", null,
      LOWER_CLASS, GUILD_ECOLOGIST,
      PRACTICED, CULTIVATION, HARD_LABOUR, NOVICE, DOMESTICS,
      OFTEN, SOCIABLE, SOMETIMES, NATURALIST, RARELY, AMBITIOUS
    ),
    
    FORMER_ENGINEER = new Background(
      "Former Engineer", "ecologist_skin.gif", null,
      MIDDLE_CLASS, GUILD_ECOLOGIST,
      PRACTICED, GEOPHYSICS, ASSEMBLY, HARD_LABOUR,
      NOVICE, GENE_CULTURE, SURVEILLANCE,
      RARELY, SOCIABLE, OFTEN, OPTIMISTIC,
      CAMOUFLAGE
    ),
    
    EXPLORER = new Background(
      "Explorer", "ecologist_skin.gif", "ecologist_portrait.png",
      MIDDLE_CLASS, GUILD_ECOLOGIST,
      EXPERT, XENOZOOLOGY, SURVEILLANCE, STEALTH_AND_COVER,
      PRACTICED, MARKSMANSHIP, NATIVE_TABOO,
      NOVICE, BATTLE_TACTICS, HAND_TO_HAND,
      RARELY, NERVOUS, INDOLENT, OFTEN, NATURALIST,
      STUN_PISTOL, CAMOUFLAGE
    ),
    
    ECOLOGIST = new Background(
      "Ecologist", "ecologist_skin.gif", "ecologist_portrait.png",
      UPPER_CLASS, GUILD_ECOLOGIST,
      EXPERT, CULTIVATION, PRACTICED, GENE_CULTURE, XENOZOOLOGY,
      PRACTICED, GEOPHYSICS, CHEMISTRY,
      ALWAYS, NATURALIST, SOMETIMES, EMPATHIC, INQUISITIVE
    ),
    
    ECOLOGIST_CIRCLES[] = { FIELD_HAND, FORMER_ENGINEER, EXPLORER, ECOLOGIST }
  ;
  
  final public static Background
    
    SUPPLY_CORPS = new Background(
      "Supply Corps", "pyon_skin.gif", null,
      LOWER_CLASS, GUILD_MERCHANT,
      NOVICE, PILOTING, HARD_LABOUR,
      OFTEN, INDOLENT, RARELY, AMBITIOUS
    ),
    
    SOMA_VENDOR = new Background(
      "Soma Vendor", "vendor_skin.gif", null,
      MIDDLE_CLASS, GUILD_MERCHANT,
      PRACTICED, COUNSEL, SUASION, NOVICE, DOMESTICS, CHEMISTRY,
      ADMINISTRATION,
      SOMETIMES, ACQUISITIVE
    ),
    
    STOCK_VENDOR = new Background(
      "Stock Vendor", "vendor_skin.gif", "vendor_portrait.png",
      MIDDLE_CLASS, GUILD_MERCHANT,
      PRACTICED, ADMINISTRATION, DOMESTICS, NOVICE, SUASION, HARD_LABOUR
    ),
    
    AUDITOR = new Background(
      "Auditor", "vendor_skin.gif", "vendor_portrait.png",
      UPPER_CLASS, GUILD_MERCHANT,
      EXPERT, COUNSEL, ADMINISTRATION, PRACTICED, COMMAND, ANCIENT_LORE,
      ALWAYS, STUBBORN, OFTEN, DUTIFUL,
      SOMETIMES, AMBITIOUS, IMPASSIVE, RARELY, DEBAUCHED
    ),
    
    VENDOR_CIRCLES[] = { SUPPLY_CORPS, SOMA_VENDOR, STOCK_VENDOR, AUDITOR }
  ;
  
  final public static Background
    
    VOLUNTEER = new Background(
      "Volunteer", "militant_skin.gif", "militant_portrait.png",
      LOWER_CLASS, GUILD_MILITANT,
      PRACTICED, HAND_TO_HAND, MARKSMANSHIP,
      NOVICE, SURVEILLANCE, ASSEMBLY, HARD_LABOUR, SHIELD_AND_ARMOUR,
      SOMETIMES, DUTIFUL, AGGRESSIVE, RARELY, NERVOUS, FEMININE,
      TASE_STAFF, PHASE_BLASTER, PARTIAL_ARMOUR
    ),
    
    RESERVIST = new Background(
      "Reservist", "artificer_skin.gif", "militant_portrait.png",
      MIDDLE_CLASS, GUILD_MILITANT,
      PRACTICED, HARD_LABOUR, ASSEMBLY,
      NOVICE, ANATOMY, PHARMACY, MARKSMANSHIP,
      RARELY, SOCIABLE, SOMETIMES, HONOURABLE,
      STUN_PISTOL, SHIELD_BELT
    ),
    
    RUNNER = new Background(
      "Runner", "runner_skin.gif", "vendor_portrait.png",
      MIDDLE_CLASS, GUILD_MILITANT,
      EXPERT, PILOTING, MARKSMANSHIP, STEALTH_AND_COVER,
      PRACTICED, SUASION, SURVEILLANCE, MASQUERADE,
      OFTEN, ACQUISITIVE, SOMETIMES, NERVOUS, RARELY, HONOURABLE,
      PHASE_BLASTER, CAMOUFLAGE
    ),
    
    VETERAN = new Background(
      "Veteran", "militant_skin.gif", "militant_portrait.png",
      UPPER_CLASS, GUILD_MILITANT,
      EXPERT, HAND_TO_HAND, MARKSMANSHIP, PRACTICED, SURVEILLANCE,
      FORMATION_COMBAT, COMMAND, SHIELD_AND_ARMOUR, BATTLE_TACTICS,
      OFTEN, DUTIFUL, SOMETIMES, STUBBORN, AMBITIOUS, NEVER, NERVOUS,
      TASE_STAFF, PHASE_BLASTER, BODY_ARMOUR
    ),
    
    MILITARY_CIRCLES[] = { VOLUNTEER, RESERVIST, RUNNER, VETERAN }
  ;
  
  final public static Background
    
    PERFORMER = new Background(
      "Performer", "aesthete_female_skin.gif", "aesthete_portrait.png",
      LOWER_CLASS, GUILD_AESTHETE,
      PRACTICED, MUSIC_AND_SONG, NOVICE, EROTICS, MASQUERADE,
      OFTEN, HANDSOME, RARELY, STOUT, SOMETIMES, EMPATHIC, DEBAUCHED,
      FINERY
    ) {
      final Texture male_skin = costumeFor("aesthete_male_skin.gif") ;
      public Texture costumeFor(Actor actor) {
        return actor.traits.female() ? costume : male_skin ;
      }
    },
    
    FABRICATOR = new Background(
      "Fabricator", "citizen_skin.gif", null,
      MIDDLE_CLASS, GUILD_AESTHETE,
      PRACTICED, CHEMISTRY, GRAPHIC_MEDIA, NOVICE, HARD_LABOUR, HANDICRAFTS,
      SOMETIMES, STUBBORN, NERVOUS
    ),
    
    SCULPTOR = new Background(
      "Sculptor", "aesthete_male_skin.gif", null,
      UPPER_CLASS, GUILD_AESTHETE,
      EXPERT, GRAPHIC_MEDIA, PRACTICED, HANDICRAFTS, NOVICE, ANATOMY,
      RARELY, STUBBORN, IMPASSIVE, OFTEN, DEBAUCHED
    ),
    
    PROPAGANDIST = new Background(
      "Propagandist", "citizen_skin.gif", null,
      UPPER_CLASS, GUILD_AESTHETE,
      EXPERT, GRAPHIC_MEDIA, SUASION,
      PRACTICED, MUSIC_AND_SONG, ADMINISTRATION,
      NOVICE, COUNSEL, PSYCHOANALYSIS,
      RARELY, HONOURABLE, STUBBORN, OFTEN, AMBITIOUS
    ),
    
    AESTHETE_CIRCLES[] = { PERFORMER, FABRICATOR, SCULPTOR, PROPAGANDIST }
  ;
  
  
  final public static Background
    //
    //  Scavengers represent the unemployed/homeless/penniless who want to
    //  leave your settlement, but can't.
    SCAVENGER = new Background(
      "Scavenger", "native_skin.gif", null,
      SLAVE_CLASS, NOT_A_GUILD,
      NOVICE, STEALTH_AND_COVER, LEARNING, HANDICRAFTS,
      OFTEN, NERVOUS, ACQUISITIVE, RARELY, INDOLENT
    ),
    //
    //  Mechanics and captains keep your dropships in working order.
    SHIP_MECHANIC = new Background(
      "Ship Mechanic", null, null,
      LOWER_CLASS, NOT_A_GUILD,
      PRACTICED, ASSEMBLY, HARD_LABOUR,
      NOVICE, FIELD_THEORY, SHIELD_AND_ARMOUR
    ),
    SHIP_CAPTAIN = new Background(
      "Ship Captain", null, null,
      MIDDLE_CLASS, NOT_A_GUILD,
      EXPERT, PILOTING, MARKSMANSHIP, PRACTICED, COMMAND, SUASION,
      NOVICE, ASTROGATION, BATTLE_TACTICS, COMMON_CUSTOM,
      STUN_PISTOL, PARTIAL_ARMOUR
    ),
    //
    //  Companions won't generally stay put, but might visit your settlement if
    //  the place is nice.
    COMPANION = new Background(
      "Companion", "aesthete_female_skin.gif", "aesthete_portrait.png",
      UPPER_CLASS, NOT_A_GUILD,
      EXPERT, EROTICS, COUNSEL, SUASION, MASQUERADE, NOBLE_ETIQUETTE,
      PRACTICED, DOMESTICS, MUSIC_AND_SONG, COMMAND, HAND_TO_HAND,
      ALWAYS, HANDSOME, OFTEN, FEMININE, EMPATHIC, TALL, RARELY, STOUT,
      FINERY
    ) {
    final Texture male_skin = costumeFor("aesthete_male_skin.gif") ;
      public Texture costumeFor(Actor actor) {
        return actor.traits.female() ? costume : male_skin ;
      }
    },
    
    MIGRANT_CIRCLES[] = { SCAVENGER, SHIP_MECHANIC, SHIP_CAPTAIN, COMPANION } ;
  
  
  final public static Background
    //
    //  You'll always get a few of these in a given native village-
    GATHERER = new Background(
      "Gatherer", "native_skin.gif", null,
      LOWER_CLASS, NOT_A_GUILD,
      EXPERT, HANDICRAFTS, PRACTICED, DOMESTICS, CULTIVATION, HARD_LABOUR,
      NATIVE_TABOO,
      RARELY, INDOLENT, OFTEN, SOCIABLE
    ),
    HUNTER = new Background(
      "Hunter", "native_skin.gif", null,
      LOWER_CLASS, NOT_A_GUILD,
      EXPERT, SURVEILLANCE, STEALTH_AND_COVER,
      PRACTICED, MARKSMANSHIP, XENOZOOLOGY, ATHLETICS,
      NOVICE, HAND_TO_HAND, HANDICRAFTS, MASQUERADE,
      OFTEN, NATURALIST
    ),
    CHIEFTAIN = new Background(
      "Chieftain", "native_skin.gif", null,
      UPPER_CLASS, NOT_A_GUILD,
      EXPERT, NATIVE_TABOO, COMMAND, SUASION, MARKSMANSHIP,
      PRACTICED, HAND_TO_HAND, BATTLE_TACTICS,
      RARELY, NERVOUS, OFTEN, TRADITIONAL
    ),
    //
    //  Depending on tribal culture, you might also get the following-
    MUTANT_PARIAH = new Background(
      "Mutant Pariah", "native_skin.gif", null,
      LOWER_CLASS, NOT_A_GUILD,
      PRACTICED, MUSIC_AND_SONG, NATIVE_TABOO, MASQUERADE,
      NOVICE, EROTICS, HANDICRAFTS,
      LEARNING, TRANSDUCTION, SYNESTHESIA, METABOLISM,
      ALWAYS, GIFTED, OFTEN, MUTATION, RARELY, FRIENDLY, HANDSOME
    ),
    CARGO_CULTIST = new Background(
      "Cargo Cultist", "native_skin.gif", null,
      MIDDLE_CLASS, NOT_A_GUILD,
      EXPERT, HANDICRAFTS, PRACTICED, DOMESTICS,
      NOVICE, ANCIENT_LORE, ASSEMBLY, COMMON_CUSTOM,
      ALWAYS, ACQUISITIVE, OPTIMISTIC, RARELY, NATURALIST
    ),
    SHAMAN = new Background(
      "Shaman", "native_skin.gif", null,
      UPPER_CLASS, NOT_A_GUILD,
      EXPERT, NATIVE_TABOO, COUNSEL, PRACTICED, CULTIVATION,
      NOVICE, PHARMACY, ANATOMY, ANCIENT_LORE, MUSIC_AND_SONG,
      ALWAYS, TRADITIONAL, OFTEN, DUTIFUL, NATURALIST
    ),
    
    //  TODO:  Implement a soFar() method for these
    NATIVE_CIRCLES[] = {
      GATHERER, HUNTER, CHIEFTAIN,
      MUTANT_PARIAH, CARGO_CULTIST, SHAMAN
    } ;
  
  
  final public static Background
    //
    //  Aristocratic titles are for the benefit of the player-character:
    KNIGHTED = new Background(
      "Knighted", "highborn_male_skin.gif", null,
      RULER_CLASS, NOT_A_GUILD,
      PRACTICED, HAND_TO_HAND, BATTLE_TACTICS, SHIELD_AND_ARMOUR, COMMAND,
      NOBLE_ETIQUETTE, NOVICE, ADMINISTRATION, ANCIENT_LORE, COMMON_CUSTOM,
      LEARNING, SUGGESTION, PREMONITION, PROJECTION,
      SOMETIMES, GIFTED, OFTEN, TRADITIONAL, RARELY, NERVOUS
    ) {
      final Texture female_skin = costumeFor("highborn_male_skin.gif") ;
      public String nameFor(Actor actor) {
        return actor.traits.male() ? "Knighted Lord" : "Knighted Lady" ;
      }
      public Texture costumeFor(Actor actor) {
        return actor.traits.male() ? costume : female_skin ;
      }
    },
    BARON    = null,
    COUNT    = null,
    DUKE     = null,
    //  TODO:  What about Prince/Lord Solar and Emperor/Empress?
    RULING_POSITIONS[] = { KNIGHTED, COUNT, BARON, DUKE },
    //
    //  Your family, servants, bodyguards and captives-
    CONSORT = new Background(
      "Consort", "highborn_female_skin.gif", null,
      RULER_CLASS, NOT_A_GUILD,
      PRACTICED, COMMAND, SUASION, NOVICE, EROTICS, MASQUERADE, DOMESTICS,
      RARELY, IMPASSIVE, STUBBORN, OFTEN, AMBITIOUS, ACQUISITIVE,
      SOMETIMES, FRIENDLY
    ) {
      final Texture male_skin = costumeFor("highborn_female_skin.gif") ;
      public String nameFor(Actor actor) {
        final boolean male = actor.traits.male() ;
        final Background rank = actor.base().ruler().vocation() ;
        if (rank == KNIGHTED) return male ? "Lord Consort" : "Lady Consort" ;
        if (rank == COUNT) return male ? "Count Consort" : "Countess Consort" ;
        if (rank == BARON) return male ? "Baron Consort" : "Baroness Consort" ;
        if (rank == DUKE ) return male ? "Duke Consort"  : "Duchess Consort"  ;
        return name ;
      }
      public Texture costumeFor(Actor actor) {
        return actor.traits.female() ? costume : male_skin ;
      }
    },
    STEWARD = new Background(
      "Steward", "citizen_skin.gif", null,
      UPPER_CLASS, NOT_A_GUILD,
      EXPERT, DOMESTICS, PRACTICED, PHARMACY, ANATOMY, COUNSEL,
      GENE_CULTURE, NOVICE, NOBLE_ETIQUETTE,
      ALWAYS, DUTIFUL, OFTEN, TRADITIONAL, NEVER, AGGRESSIVE
    ),
    HONOUR_GUARD = null,
    HOSTAGE      = null,
    
    COURT_CIRCLES[] = {},
    //
    //  Ministers confer the benefits of a portion of their skills on the
    //  planet as a whole (including stuff off the main map.)
    MINISTER_FOR_ACCOUNTS    = null,
    MINISTER_FOR_PROPAGANDA  = null,
    MINISTER_FOR_HEALTH      = null,
    MASTER_OF_ASSASSINS      = null,
    WARMASTER                = null,
    PLANETOLOGIST            = null,
    //
    //  These positions are for the benefit of citizens elected at the Counsel
    //  Chamber.  TODO:  Add these later.
    PREFECT = new Background(
      "Prefect", "physician_skin.gif", null,
      RULER_CLASS, NOT_A_GUILD,
      PRACTICED, COUNSEL, SUASION, ADMINISTRATION, COMMON_CUSTOM,
      NOVICE, NOBLE_ETIQUETTE, PSYCHOANALYSIS, BATTLE_TACTICS, COMMAND,
      OFTEN, SOCIABLE, AMBITIOUS, SOMETIMES, ACQUISITIVE
    ),
    GOVERNOR = null,
    SENATOR  = null,
    CONSUL   = null,
    
    GOVERNMENT_CIRCLES[] = {}
    //
    //  TODO:  Do you need positions for leadership within the Strains?
  ;
  
  
  final public static int
    INTENSE_GRAVITY = -2,
    STRONG_GRAVITY  = -1,
    NORMAL_GRAVITY  =  0,
    MILD_GRAVITY    =  1,
    NOMINAL_GRAVITY =  2 ;
  
  final public static System
    //
    //  TODO:  List ruling houses and common names.
    //         Introduce Calivor and Theta Rho.
    //         Give each system a brief text description.
    PLANET_ASRA_NOVI = new System(
      "Asra Novi", null, 0, 1,
      DESERT_BLOOD, MILD_GRAVITY,
      MAKES, SOMA, PLASTICS, DECOR, SPICE,
      NEEDS, WATER, SERVICE_CONSORTS, DATALINKS,
      FREE_BIRTH,
      OFTEN, ECOLOGIST_CIRCLES, SOMETIMES, COURT_CIRCLES, AESTHETE_CIRCLES
      //  House Suhail
    ),
    PLANET_PAREM_V = new System(
      "Parem V", null, 1, 1,
      WASTES_BLOOD, NORMAL_GRAVITY,
      MAKES, PARTS, DATALINKS, SERVICE_CAPTIVES,
      NEEDS, PETROCARBS, DECOR, RARITIES,
      HIVES_BIRTH, PYON_BIRTH,
      OFTEN, ARTIFICER_CIRCLES, SOMETIMES, COURT_CIRCLES,
      RARELY, ECOLOGIST_CIRCLES
      //  House Procyon
    ),
    PLANET_HALIBAN = new System(
      "Haliban", null, 0, 0,
      FOREST_BLOOD, STRONG_GRAVITY,
      MAKES, CARBS, GREENS, PETROCARBS,
      NEEDS, SERVICE_ARMAMENT, PARTS, MEDICINE,
      GUILDER_BIRTH, FREE_BIRTH,
      OFTEN, MILITARY_CIRCLES, SOMETIMES, PHYSICIAN_CIRCLES,
      RARELY, VENDOR_CIRCLES
      //  House Altair
    ),
    PLANET_AXIS_NOVENA = new System(
      "Axis Novena", null, 1, 0,
      TUNDRA_BLOOD, NOMINAL_GRAVITY,
      MAKES, CIRCUITRY, MEDICINE, SERVICE_SHIPPING,
      NEEDS, GREENS, METAL_ORE, FUEL_CORES,
      HIVES_BIRTH, GUILDER_BIRTH,
      OFTEN, PHYSICIAN_CIRCLES, ARTIFICER_CIRCLES, RARELY, AESTHETE_CIRCLES
      //  House Taygeta
    ),
    
    ALL_PLANETS[] = {
      PLANET_ASRA_NOVI, PLANET_PAREM_V, PLANET_HALIBAN, PLANET_AXIS_NOVENA
    } ;
  
  
  final public static Background
    ALL_BACKGROUNDS[] = (Background[]) all.toArray(Background.class) ;
  
  
  
  
  
  final public String name ;
  final protected Texture costume, portrait ;
  
  final public int standing ;
  final public int guild ;
  final Table <Skill, Integer> baseSkills = new Table <Skill, Integer> () ;
  final Table <Trait, Float> traitChances = new Table <Trait, Float> () ;
  final List <Service> gear = new List <Service> () ;
  
  
  
  protected Background(
    String name, String costumeTex, String portraitTex,
    int standing, int guild, Object... args
  ) {
    this.name = name ;
    if (costumeTex == null) this.costume = null ;
    else this.costume = Texture.loadTexture(COSTUME_DIR+costumeTex) ;
    if (portraitTex == null) this.portrait = null ;
    else this.portrait = Texture.loadTexture(COSTUME_DIR+portraitTex) ;
    
    this.standing = standing ;
    this.guild = guild ;
    
    int level = 10 ;
    float chance = 0.5f ;
    for (int i = 0 ; i < args.length ; i++) {
      final Object o = args[i] ;
      if      (o instanceof Integer) { level  = (Integer) o ; }
      else if (o instanceof Float  ) { chance = (Float)   o ; }
      else if (o instanceof Skill) {
        baseSkills.put((Skill) o, level) ;
      }
      else if (o instanceof Trait) {
        traitChances.put((Trait) o, chance) ;
      }
      else if (o instanceof Service) {
        gear.add((Service) o) ;
      }
    }
    all.add(this) ;
  }
  
  
  protected Texture costumeFor(String texName) {
    return Texture.loadTexture(COSTUME_DIR+texName) ;
  }
  
  
  public String toString() {
    return name ;
  }
  
  
  public String nameFor(Actor actor) {
    return name ;
  }
  
  
  public Texture costumeFor(Actor actor) {
    return costume ;
  }
  
  
  public Texture portraitFor(Actor actor) {
    return portrait ;
  }
}








