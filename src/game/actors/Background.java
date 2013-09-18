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
      "Native Birth", "native_skin.gif", null, -1,
      NOVICE, NATIVE_TABOO, LEARNING, HANDICRAFTS, MARKSMANSHIP, XENOZOOLOGY
    ),
    //
    //  The following are available to most actors as part of their careers-
    HIVES_BIRTH = new Background(
      "Hives Birth", "artificer_skin.gif", null, SLAVE_CLASS,
      NOVICE, COMMON_CUSTOM, LEARNING, NATIVE_TABOO, HAND_TO_HAND, CHEMISTRY
    ),
    PYON_BIRTH = new Background(
      "Pyon Birth", "pyon_skin.gif", null, LOWER_CLASS,
      NOVICE, COMMON_CUSTOM, LEARNING, HARD_LABOUR, HOUSEKEEPING, ASSEMBLY
    ),
    FREE_BIRTH = new Background(
      "Free Birth", "citizen_skin.gif", null, MIDDLE_CLASS,
      NOVICE, COMMON_CUSTOM, LEARNING, SUASION, MARKSMANSHIP, PILOTING
    ),
    GUILDER_BIRTH = new Background(
      "Guilder Birth", "vendor_skin.gif", null, UPPER_CLASS,
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
      SLAVE_CLASS,
      EXPERT, HARD_LABOUR, PRACTICED, GEOPHYSICS, NOVICE, ASSEMBLY,
      OFTEN, STUBBORN, RARELY, NERVOUS, HANDSOME
    ),
    
    TECHNICIAN = new Background(
      "Technician", "artificer_skin.gif", "artificer_portrait.png",
      LOWER_CLASS,
      PRACTICED, ASSEMBLY, HARD_LABOUR, NOVICE, FIELD_THEORY,
      SOMETIMES, DUTIFUL, RARELY, INDOLENT
    ),
    
    CORE_TECHNICIAN = new Background(
      "Core Technician", "citizen_skin.gif", "artificer_portrait.png",
      MIDDLE_CLASS,
      EXPERT, FIELD_THEORY, PRACTICED, CHEMISTRY, NOVICE, SHIELD_AND_ARMOUR,
      OFTEN, DUTIFUL, SOMETIMES, NERVOUS
    ),
    
    ARTIFICER = new Background(
      "Artificer", "artificer_skin.gif", "artificer_portrait.png",
      UPPER_CLASS,
      EXPERT, ASSEMBLY, PRACTICED, FIELD_THEORY, CHEMISTRY,
      NOVICE, ANCIENT_LORE, PSYCHOANALYSIS,
      SOMETIMES, INQUISITIVE, RARELY, NATURALIST
    ),
    
    ARTIFICER_CIRCLES[] = { EXCAVATOR, TECHNICIAN, CORE_TECHNICIAN, ARTIFICER }
  ;

  final public static Background
    
    MINDER = new Background(
      "Minder", "citizen_skin.gif", null,
      MIDDLE_CLASS,
      PRACTICED, HOUSEKEEPING, SUASION, NOVICE, ANATOMY, PHARMACY,
      OFTEN, EMPATHIC, SOMETIMES, STUBBORN
    ),
    
    VAT_BREEDER = new Background(
      "Vat Breeder", "citizen_skin.gif", null,
      MIDDLE_CLASS,
      PRACTICED, GENE_CULTURE, PHARMACY, CHEMISTRY,
      RARELY, DEBAUCHED, INDOLENT
    ),
    
    ARCHIVIST = new Background(
      "Archivist", "citizen_skin.gif", null,
      MIDDLE_CLASS,
      PRACTICED, ADMINISTRATION, PSYCHOANALYSIS, NOVICE, COUNSEL,
      ALWAYS, INQUISITIVE, SOMETIMES, NERVOUS, IMPASSIVE
    ),

    PHYSICIAN = new Background(
      "Physician", "physician_skin.gif", "physician_portrait.png",
      UPPER_CLASS,
      EXPERT, ANATOMY, PHARMACY, PRACTICED, GENE_CULTURE, PSYCHOANALYSIS,
      NOVICE, COUNSEL, SUASION,
      OFTEN, INQUISITIVE, SOMETIMES, HONOURABLE, IMPASSIVE, RARELY, DEBAUCHED
    ),
    
    PHYSICIAN_CIRCLES[] = { MINDER, VAT_BREEDER, ARCHIVIST, PHYSICIAN }
  ;
  
  final public static Background
    
    FIELD_HAND = new Background(
      "Field Hand", "pyon_skin.gif", null,
      LOWER_CLASS,
      PRACTICED, CULTIVATION, HARD_LABOUR, NOVICE, HOUSEKEEPING,
      OFTEN, SOCIABLE, SOMETIMES, NATURALIST, RARELY, AMBITIOUS
    ),
    
    CLIMATE_ENGINEER = new Background(
      "Climate Engineer", "ecologist_skin.gif", null,
      MIDDLE_CLASS,
      PRACTICED, GEOPHYSICS, ASSEMBLY, HARD_LABOUR,
      NOVICE, GENE_CULTURE, XENOZOOLOGY, SURVEILLANCE,
      RARELY, SOCIABLE, OFTEN, OPTIMISTIC,
      CAMOUFLAGE
    ),
    
    SURVEYOR = new Background(
      "Surveyor", "ecologist_skin.gif", "ecologist_portrait.png",
      MIDDLE_CLASS,
      EXPERT, XENOZOOLOGY, SURVEILLANCE, STEALTH_AND_COVER,
      PRACTICED, MARKSMANSHIP, NATIVE_TABOO,
      NOVICE, FORMATION_COMBAT, HAND_TO_HAND,
      RARELY, NERVOUS, INDOLENT, OFTEN, NATURALIST,
      PHASE_BLASTER, CAMOUFLAGE
    ),
    
    ECOLOGIST = new Background(
      "Ecologist", "ecologist_skin.gif", "ecologist_portrait.png",
      UPPER_CLASS,
      EXPERT, CULTIVATION, PRACTICED, GENE_CULTURE, XENOZOOLOGY,
      PRACTICED, GEOPHYSICS, CHEMISTRY,
      ALWAYS, NATURALIST, SOMETIMES, EMPATHIC, INQUISITIVE
    ),
    
    ECOLOGIST_CIRCLES[] = { FIELD_HAND, CLIMATE_ENGINEER, SURVEYOR, ECOLOGIST }
  ;
  
  final public static Background
    
    SUPPLY_CORPS = new Background(
      "Supply Corps", "pyon_skin.gif", null,
      LOWER_CLASS,
      NOVICE, PILOTING, HARD_LABOUR,
      OFTEN, INDOLENT, RARELY, AMBITIOUS
    ),
    
    SOMA_VENDOR = new Background(
      "Soma Vendor", "vendor_skin.gif", null,
      MIDDLE_CLASS,
      PRACTICED, COUNSEL, SUASION, NOVICE, HOUSEKEEPING, CHEMISTRY,
      ADMINISTRATION,
      SOMETIMES, ACQUISITIVE
    ),
    
    STOCK_VENDOR = new Background(
      "Stock Vendor", "vendor_skin.gif", "vendor_portrait.png",
      MIDDLE_CLASS,
      NOVICE, SUASION, HARD_LABOUR, ADMINISTRATION
    ),
    
    AUDITOR = new Background(
      "Auditor", "vendor_skin.gif", "vendor_portrait.png",
      UPPER_CLASS,
      EXPERT, COUNSEL, ADMINISTRATION, PRACTICED, COMMAND, ANCIENT_LORE,
      ALWAYS, STUBBORN, OFTEN, DUTIFUL,
      SOMETIMES, AMBITIOUS, IMPASSIVE, RARELY, DEBAUCHED
    ),
    
    VENDOR_CIRCLES[] = { SUPPLY_CORPS, SOMA_VENDOR, STOCK_VENDOR, AUDITOR }
  ;
  
  final public static Background
    
    VOLUNTEER = new Background(
      "Volunteer", "militant_skin.gif", "militant_portrait.png",
      LOWER_CLASS,
      PRACTICED, HAND_TO_HAND, MARKSMANSHIP,
      NOVICE, SURVEILLANCE, ASSEMBLY, HARD_LABOUR, SHIELD_AND_ARMOUR,
      SOMETIMES, DUTIFUL, AGGRESSIVE, RARELY, NERVOUS, FEMININE,
      TASE_STAFF, PHASE_BLASTER, PARTIAL_ARMOUR
    ),
    
    RUNNER = new Background(
      "Runner", "runner_skin.gif", "vendor_portrait.png",
      MIDDLE_CLASS,
      EXPERT, PILOTING, MARKSMANSHIP, STEALTH_AND_COVER,
      PRACTICED, SUASION, SURVEILLANCE, DISGUISE,
      OFTEN, ACQUISITIVE, SOMETIMES, NERVOUS, RARELY, HONOURABLE,
      PHASE_BLASTER, CAMOUFLAGE
    ),
    
    TECH_RESERVE = new Background(
      "Tech Reserve", "artificer_skin.gif", "artificer_portrait.png",
      MIDDLE_CLASS,
      PRACTICED, HARD_LABOUR, ASSEMBLY, MARKSMANSHIP,
      NOVICE, ANATOMY, PHARMACY, COMMAND,
      RARELY, SOCIABLE, SOMETIMES, HONOURABLE,
      PHASE_BLASTER, SHIELD_BELT
    ),
    
    VETERAN = new Background(
      "Veteran", "militant_skin.gif", "militant_portrait.png",
      UPPER_CLASS,
      EXPERT, HAND_TO_HAND, MARKSMANSHIP, PRACTICED, SURVEILLANCE,
      FORMATION_COMBAT, COMMAND, SHIELD_AND_ARMOUR, BATTLE_TACTICS,
      OFTEN, DUTIFUL, SOMETIMES, STUBBORN, AMBITIOUS, NEVER, NERVOUS,
      TASE_STAFF, PHASE_BLASTER, BODY_ARMOUR
    ),
    
    MILITARY_CIRCLES[] = { VOLUNTEER, RUNNER, TECH_RESERVE, VETERAN }
  ;
  
  final public static Background
    
    PERFORMER = new Background(
      "Performer", "aesthete_male_skin.gif", "aesthete_portrait.png",
      LOWER_CLASS,
      PRACTICED, MUSIC_AND_SONG, NOVICE, EROTIC_DANCE, DISGUISE,
      OFTEN, HANDSOME, RARELY, STOUT, SOMETIMES, EMPATHIC, DEBAUCHED
    ),
    
    FABRICATOR = new Background(
      "Fabricator", "citizen_skin.gif", null,
      MIDDLE_CLASS,
      PRACTICED, CHEMISTRY, GRAPHIC_DESIGN, NOVICE, HARD_LABOUR, HANDICRAFTS,
      SOMETIMES, STUBBORN, NERVOUS
    ),
    
    SCULPTOR = new Background(
      "Sculptor", "aesthete_male_skin.gif", null,
      UPPER_CLASS,
      EXPERT, GRAPHIC_DESIGN, PRACTICED, HANDICRAFTS, NOVICE, ANATOMY,
      RARELY, STUBBORN, IMPASSIVE, OFTEN, DEBAUCHED
    ),
    
    PROPAGANDIST = new Background(
      "Propagandist", "citizen_skin.gif", null,
      UPPER_CLASS,
      EXPERT, GRAPHIC_DESIGN, SUASION,
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
      SLAVE_CLASS,
      NOVICE, STEALTH_AND_COVER, LEARNING, HANDICRAFTS,
      OFTEN, NERVOUS, ACQUISITIVE, RARELY, INDOLENT
    ),
    //
    //  Mechanics and captains keep your dropships in working order.
    SHIP_MECHANIC = new Background(
      "Ship Mechanic", null, null,
      LOWER_CLASS,
      PRACTICED, ASSEMBLY, HARD_LABOUR,
      NOVICE, FIELD_THEORY, SHIELD_AND_ARMOUR
    ),
    SHIP_CAPTAIN = new Background(
      "Ship Captain", null, null,
      MIDDLE_CLASS,
      EXPERT, PILOTING, MARKSMANSHIP, PRACTICED, COMMAND, SUASION,
      NOVICE, ASTROGATION, BATTLE_TACTICS, COMMON_CUSTOM
    ),
    //
    //  Companions won't generally stay put, but might visit your settlement if
    //  the place is nice.
    COMPANION = new Background(
      "Companion", "aesthete_male_skin.gif", "aesthete_portrait.png",
      UPPER_CLASS,
      EXPERT, EROTIC_DANCE, COUNSEL, SUASION, DISGUISE, NOBLE_ETIQUETTE,
      PRACTICED, HOUSEKEEPING, MUSIC_AND_SONG, COMMAND, HAND_TO_HAND,
      ALWAYS, HANDSOME, OFTEN, FEMININE, EMPATHIC, TALL, RARELY, STOUT
    ),
    
    MIGRANT_CIRCLES[] = { SCAVENGER, SHIP_MECHANIC, SHIP_CAPTAIN, COMPANION } ;
  
  
  final public static Background
    //
    //  You'll always get a few of these in a given native village-
    GATHERER = new Background(
      "Gatherer", "native_skin.gif", null,
      LOWER_CLASS,
      PRACTICED, HANDICRAFTS, HOUSEKEEPING,
      NOVICE, CULTIVATION, HARD_LABOUR,
      RARELY, INDOLENT, OFTEN, SOCIABLE
    ),
    HUNTER = new Background(
      "Hunter", "native_skin.gif", null,
      LOWER_CLASS,
      EXPERT, SURVEILLANCE, STEALTH_AND_COVER,
      PRACTICED, MARKSMANSHIP, XENOZOOLOGY, ATHLETICS,
      NOVICE, HAND_TO_HAND, HANDICRAFTS, DISGUISE,
      OFTEN, NATURALIST
    ),
    CHIEFTAIN = new Background(
      "Chieftain", "native_skin.gif", null,
      UPPER_CLASS,
      EXPERT, NATIVE_TABOO, COMMAND, SUASION, MARKSMANSHIP,
      PRACTICED, HAND_TO_HAND, BATTLE_TACTICS,
      RARELY, NERVOUS, OFTEN, TRADITIONAL
    ),
    //
    //  Depending on tribal culture, you might also get the following-
    PARIAH = new Background(
      "Pariah", "native_skin.gif", null,
      MIDDLE_CLASS,
      PRACTICED, MUSIC_AND_SONG, NATIVE_TABOO, DISGUISE,
      NOVICE, EROTIC_DANCE, HANDICRAFTS,
      LEARNING, METABOLISM, SYNESTHESIA,
      ALWAYS, GIFTED, SOMETIMES, MUTATION, RARELY, LOVING
    ),
    CARGO_CULTIST = new Background(
      "Cargo Cultist", "native_skin.gif", null,
      MIDDLE_CLASS,
      EXPERT, HANDICRAFTS, PRACTICED, HOUSEKEEPING,
      NOVICE, ANCIENT_LORE, ASSEMBLY, COMMON_CUSTOM,
      ALWAYS, ACQUISITIVE, OPTIMISTIC
    ),
    SHAMAN = new Background(
      "Shaman", "native_skin.gif", null,
      UPPER_CLASS,
      PRACTICED, CULTIVATION, NATIVE_TABOO, COUNSEL,
      NOVICE, PHARMACY, ANATOMY, ANCIENT_LORE, MUSIC_AND_SONG,
      ALWAYS, TRADITIONAL, OFTEN, DUTIFUL
    ),
    
    //  TODO:  Implement a soFar() method for these
    NATIVE_CIRCLES[] = {
      GATHERER, HUNTER, CHIEFTAIN,
      PARIAH, CARGO_CULTIST, SHAMAN
    } ;
  
  
  final static Background
    //
    //  These positions don't have any particular skill-sets associated.
    //  Instead the player is expected to fill them solely based on their own
    //  evaluation of fitness for the post (or possibly have them selected
    //  during game setup.)  TODO:  Consider the right setup for this...
    //
    //  Aristocratic titles start you off with more power, but limit subsequent
    //  advancement somewhat.
    KNIGHTED = null,
    COUNT    = null,
    BARON    = null,
    DUKE     = null,
    //
    //  Elected titles take more effort to earn, but do afford more scope for
    //  ambition.  (Consuls, in principle, are on an equal footing with the
    //  System Lords and Empress.)
    PREFECT  = null,
    GOVERNOR = null,
    SENATOR  = null,
    CONSUL   = null,
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
    //  Your family, bodyguards, servants and prisoners/hostages-
    CONSORT      = null,
    HONOUR_GUARD = null,
    STEWARD      = null,
    CAPTIVE      = null ;
  
  
  
  final public static int
    INTENSE_GRAVITY = -2,
    STRONG_GRAVITY  = -1,
    NORMAL_GRAVITY  =  0,
    MILD_GRAVITY    =  1,
    NOMINAL_GRAVITY =  2 ;
  
  final public static System
    //
    //  Some simplifying assumptions in place for now.  These will later be
    //  replaced by more detailed planetary descriptors, including gravity,
    //  culture, ruling house, local factional interests, et cetera.
    PLANET_ASRA_NOVI = new System(
      "Asra Novi", null, 0, 1,
      ALWAYS, DESERT_BLOOD,
      MAKES, SOMA, PLASTICS, DECOR, SPICE,
      NEEDS, WATER, SERVICE_CONSORTS, DATALINKS,
      FREE_BIRTH,
      OFTEN, ECOLOGIST_CIRCLES, AESTHETE_CIRCLES
    ),
    PLANET_PAREM_V = new System(
      "Parem V", null, 1, 1,
      ALWAYS, WASTES_BLOOD,
      MAKES, PARTS, DATALINKS, SERVICE_CAPTIVES,
      NEEDS, P_CARBONS, DECOR, RELICS,
      HIVES_BIRTH, PYON_BIRTH,
      OFTEN, ARTIFICER_CIRCLES, RARELY, ECOLOGIST_CIRCLES
    ),
    PLANET_HALIBAN = new System(
      "Haliban", null, 0, 0,
      ALWAYS, FOREST_BLOOD,
      MAKES, CARBS, GREENS, P_CARBONS,
      NEEDS, SERVICE_ARMAMENT, PARTS, MEDICINE,
      GUILDER_BIRTH, FREE_BIRTH,
      OFTEN, MILITARY_CIRCLES, PHYSICIAN_CIRCLES, RARELY, VENDOR_CIRCLES
    ),
    PLANET_NOVENA = new System(
      "Novena", null, 1, 0,
      ALWAYS, TUNDRA_BLOOD,
      MAKES, CIRCUITRY, MEDICINE, SERVICE_SHIPPING,
      NEEDS, GREENS, METAL_ORE, FUEL_CORES,
      HIVES_BIRTH, GUILDER_BIRTH,
      OFTEN, VENDOR_CIRCLES, ARTIFICER_CIRCLES, RARELY, AESTHETE_CIRCLES
    ),
    //
    //  TODO:  Introduce Calivor and Theta Rho.
    
    ALL_PLANETS[] = {
      PLANET_ASRA_NOVI, PLANET_PAREM_V, PLANET_HALIBAN, PLANET_NOVENA
    } ;
  
  
  final public static Background
    ALL_BACKGROUNDS[] = (Background[]) all.toArray(Background.class) ;
  
  
  
  
  
  final public String name ;
  final public Texture costume, portrait ;
  
  final public int standing ;
  Table <Skill, Integer> baseSkills = new Table() ;
  Table <Trait, Float> traitChances = new Table() ;
  List <Service> gear = new List() ;
  
  
  
  protected Background(
    String name, String costumeTex, String portraitTex,
    int standing, Object... args
  ) {
    this.name = name ;
    if (costumeTex == null) this.costume = null ;
    else this.costume = Texture.loadTexture(COSTUME_DIR+costumeTex) ;
    if (portraitTex == null) this.portrait = null ;
    else this.portrait = Texture.loadTexture(COSTUME_DIR+portraitTex) ;
    this.standing = standing ;
    
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
  
  
  public String toString() {
    return name ;
  }
}













