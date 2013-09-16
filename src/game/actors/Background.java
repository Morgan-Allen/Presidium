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



public class Background implements BuildConstants {
  
  
  
  final public static Float
    ALWAYS    =  1.0f,
    OFTEN     =  0.6f,
    SOMETIMES =  0.3f,
    RARELY    = -0.7f,
    NEVER     = -1.0f ;
  final public static Integer
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
    50, 150, 250, 500, 1000  //Represents 10 days' salary...
  } ;
  
  
  final protected static Object
    MAKES = new Object(),
    NEEDS = new Object() ;
  
  final static String COSTUME_DIR = "media/Actors/human/" ;
  
  private static int nextID = 0 ;
  final public int ID = nextID++ ;
  private static Batch <Background> all = new Batch() ;
  
  
  final public static System
    //
    //  Some simplifying assumptions in place for now.  These will later be
    //  replaced by more detailed planetary descriptors, including gravity,
    //  culture, ruling house, local factional interests, et cetera.
    PLANET_ASRA_NOVI = new System(
      "Asra Novi"  , null, 0, 1,
      ALWAYS, DESERT_BLOOD,
      MAKES, SOMA, PLASTICS, DECOR,
      NEEDS, WATER, SERVICE_CAPTIVES, SPICE
    ),
    PLANET_PAREM_V = new System(
      "Parem V"  , null, 1, 1,
      ALWAYS, WASTES_BLOOD,
      MAKES, PARTS, DATALINKS, SERVICE_CAPTIVES,
      NEEDS, P_CARBONS, DECOR, RELICS
    ),
    PLANET_HALIBAN = new System(
      "Haliban", null, 0, 0,
      ALWAYS, FOREST_BLOOD,
      MAKES, CARBS, GREENS, P_CARBONS,
      NEEDS, SERVICE_ARMAMENT, PARTS, MEDICINE
    ),
    PLANET_NOVENA = new System(
      "Novena", null, 1, 0,
      ALWAYS, TUNDRA_BLOOD,
      MAKES, CIRCUITS, MEDICINE, SERVICE_SHIPPING,
      NEEDS, GREENS, ORES, FUEL_CORES
    ),
    
    ALL_PLANETS[] = {
      PLANET_ASRA_NOVI, PLANET_PAREM_V, PLANET_HALIBAN, PLANET_NOVENA
    } ;
  
  
  final public static Background
    NATIVE_BIRTH = new Background(
      "Native Birth", "native_skin.gif", null, SLAVE_CLASS
    ),
    PYON_BIRTH = new Background(
      "Pyon Birth", "pyon_skin.gif", null, LOWER_CLASS
    ),
    CITIZEN_BIRTH = new Background(
      "Citizen Birth", "citizen_skin.gif", null, UPPER_CLASS
    ),
    HIGH_BIRTH = new Background(
      "High Birth", "highborn_male_skin.gif", null, RULER_CLASS
    ),
    ALL_CLASSES[] = { PYON_BIRTH, CITIZEN_BIRTH } ;
  
  
  
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
      NOVICE, ANCIENT_LORE, LOGOS_MENSA,
      SOMETIMES, INQUISITIVE, RARELY, NATURALIST
    )
  ;

  final public static Background
    
    MINDER = new Background(
      "Minder", "citizen_skin.gif", null,
      MIDDLE_CLASS,
      PRACTICED, DOMESTIC_SERVICE, SUASION, NOVICE, ANATOMY, PHARMACY,
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
      PRACTICED, ADMINISTRATION, LOGOS_MENSA, NOVICE, COUNSEL,
      ALWAYS, INQUISITIVE, SOMETIMES, NERVOUS, IMPASSIVE
    ),

    PHYSICIAN = new Background(
      "Physician", "physician_skin.gif", "physician_portrait.png",
      UPPER_CLASS,
      EXPERT, ANATOMY, PHARMACY, PRACTICED, GENE_CULTURE, LOGOS_MENSA,
      NOVICE, COUNSEL, SUASION,
      OFTEN, INQUISITIVE, SOMETIMES, HONOURABLE, IMPASSIVE, RARELY, DEBAUCHED
    )
  ;
  
  final public static Background
    
    FIELD_HAND = new Background(
      "Field Hand", "pyon_skin.gif", null,
      LOWER_CLASS,
      PRACTICED, CULTIVATION, HARD_LABOUR, NOVICE, DOMESTIC_SERVICE,
      OFTEN, SOCIABLE, SOMETIMES, NATURALIST, RARELY, AMBITIOUS
    ),
    
    CLIMATE_ENGINEER = new Background(
      "Climate Engineer", "ecologist_skin.gif", null,
      MIDDLE_CLASS,
      PRACTICED, GEOPHYSICS, ASSEMBLY, HARD_LABOUR,
      NOVICE, GENE_CULTURE, XENOBIOLOGY, SURVEILLANCE,
      RARELY, SOCIABLE, OFTEN, OPTIMISTIC,
      CAMOUFLAGE
    ),
    
    SURVEYOR = new Background(
      "Surveyor", "ecologist_skin.gif", "ecologist_portrait.png",
      MIDDLE_CLASS,
      EXPERT, XENOBIOLOGY, SURVEILLANCE, STEALTH_AND_COVER,
      PRACTICED, MARKSMANSHIP, NATIVE_TABOO,
      NOVICE, BATTLE_TACTICS, CLOSE_COMBAT,
      RARELY, NERVOUS, INDOLENT, OFTEN, NATURALIST,
      PHASE_BLASTER, CAMOUFLAGE
    ),
    
    ECOLOGIST = new Background(
      "Ecologist", "ecologist_skin.gif", "ecologist_portrait.png",
      UPPER_CLASS,
      EXPERT, CULTIVATION, PRACTICED, GENE_CULTURE, XENOBIOLOGY,
      PRACTICED, GEOPHYSICS, CHEMISTRY,
      ALWAYS, NATURALIST, SOMETIMES, EMPATHIC, INQUISITIVE
    )
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
      PRACTICED, COUNSEL, SUASION, NOVICE, DOMESTIC_SERVICE, CHEMISTRY,
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
      EXPERT, COUNSEL, ADMINISTRATION, PRACTICED, COMMAND, LOGOS_MENSA,
      ALWAYS, STUBBORN, OFTEN, DUTIFUL,
      SOMETIMES, AMBITIOUS, IMPASSIVE, RARELY, DEBAUCHED
    )
  ;
  
  final public static Background
    
    VOLUNTEER = new Background(
      "Volunteer", "militant_skin.gif", "militant_portrait.png",
      LOWER_CLASS,
      PRACTICED, CLOSE_COMBAT, MARKSMANSHIP,
      NOVICE, SURVEILLANCE, ASSEMBLY, HARD_LABOUR, SHIELD_AND_ARMOUR,
      SOMETIMES, DUTIFUL, AGGRESSIVE, RARELY, NERVOUS, FEMININE,
      SHOCK_STAFF, BODY_ARMOUR
    ),
    
    RUNNER = new Background(
      "Runner", "runner_skin.gif", "vendor_portrait.png",
      MIDDLE_CLASS,
      EXPERT, PILOTING, MARKSMANSHIP, STEALTH_AND_COVER,
      PRACTICED, SUASION, SURVEILLANCE, DISGUISE,
      OFTEN, ACQUISITIVE, SOMETIMES, NERVOUS,
      PHASE_BLASTER, CAMOUFLAGE
    ),
    
    TECH_RESERVE = new Background(
      "Tech Reserve", "artificer_skin.gif", "artificer_portrait.png",
      MIDDLE_CLASS,
      PRACTICED, HARD_LABOUR, ASSEMBLY, MARKSMANSHIP,
      NOVICE, ANATOMY, PHARMACY, COMMAND,
      RARELY, SOCIABLE, SOMETIMES, HONOURABLE,
      PHASE_BLASTER
    ),
    
    VETERAN = new Background(
      "Veteran", "militant_skin.gif", "militant_portrait.png",
      UPPER_CLASS,
      EXPERT, CLOSE_COMBAT, MARKSMANSHIP, PRACTICED, SURVEILLANCE,
      BATTLE_TACTICS, COMMAND, SHIELD_AND_ARMOUR,
      OFTEN, DUTIFUL, SOMETIMES, STUBBORN, AMBITIOUS, NEVER, NERVOUS,
      SHOCK_STAFF, BODY_ARMOUR
    ) ;
  
  
  final public static Background
    
    PERFORMER = new Background(
      "Performer", "aesthete_male_skin.gif", "aesthete_portrait.png",
      LOWER_CLASS,
      PRACTICED, MUSIC_AND_SONG, NOVICE, CARNAL_PLEASURE, DISGUISE,
      OFTEN, HANDSOME, SOMETIMES, EMPATHIC
    ),
    
    FABRICATOR = new Background(
      "Fabricator", "citizen_skin.gif", null,
      MIDDLE_CLASS,
      PRACTICED, CHEMISTRY, GRAPHIC_MEDIA, NOVICE, HARD_LABOUR,
      SOMETIMES, STUBBORN, NERVOUS
    ),
    
    //
    //  TODO:  Cut out the Companion for now, replace with Sculptor?  I'm not
    //  planning to implement their venue for a while.
    //SCULPTOR = new Vocation(),
    
    PROPAGANDIST = new Background(
      "Propagandist", "citizen_skin.gif", null,
      UPPER_CLASS,
      EXPERT, GRAPHIC_MEDIA, SUASION, PRACTICED, MUSIC_AND_SONG, COUNSEL,
      NOVICE, ADMINISTRATION, LOGOS_MENSA,
      RARELY, HONOURABLE, STUBBORN, OFTEN, AMBITIOUS
    ),
    
    COMPANION = new Background(
      "Companion", "aesthete_male_skin.gif", "aesthete_portrait.png",
      UPPER_CLASS,
      EXPERT, CARNAL_PLEASURE, COUNSEL, SUASION, DISGUISE,
      PRACTICED, DOMESTIC_SERVICE, MUSIC_AND_SONG,
      ALWAYS, HANDSOME, OFTEN, FEMININE, EMPATHIC, TALL, RARELY, STOUT
    ) ;
  
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












