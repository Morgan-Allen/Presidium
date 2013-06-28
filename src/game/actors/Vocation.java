/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.util.* ;



public class Vocation implements ActorConstants {
  

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
    SLAVE_CLASS =  0,
    LOWER_CLASS =  1,
    UPPER_CLASS =  2,
    RULER_CLASS =  3 ;
  
  final static String COSTUME_DIR = "media/Actors/human/" ;
  
  static int nextID = 0 ;
  final public int ID = nextID++ ;
  private static Batch <Vocation> all = new Batch() ;
  
  
  
  final public static Vocation
    //
    //  Some simplifying assumptions in place for now.  These will later be
    //  replaced by more detailed planetary descriptors, including gravity,
    //  culture, ruling house, local factional interests, et cetera.
    PLANET_ASRA_NOVI = new Vocation(
      "Asra Novi"  , null, null, -1,
      ALWAYS, DESERT_BLOOD
    ),
    PLANET_PAREM_V = new Vocation(
      "Parem V"  , null, null, -1,
      ALWAYS, WASTES_BLOOD
    ),
    PLANET_HALIBAN = new Vocation(
      "Theta Rho", null, null, -1,
      ALWAYS, FOREST_BLOOD
    ),
    PLANET_AXIS_NOVENA = new Vocation(
      "Axis Novena", null, null, -1,
      ALWAYS, TUNDRA_BLOOD
    ),
    ALL_PLANETS[] = {
      PLANET_ASRA_NOVI, PLANET_PAREM_V, PLANET_HALIBAN, PLANET_AXIS_NOVENA
    } ;
  
  
  final public static Vocation
    NATIVE_BIRTH = new Vocation(
      "Native Birth", "native_skin.gif", null, SLAVE_CLASS
    ),
    PYON_BIRTH = new Vocation(
      "Pyon Birth", "pyon_skin.gif", null, LOWER_CLASS
    ),
    CITIZEN_BIRTH = new Vocation(
      "Citizen Birth", "citizen_skin.gif", null, UPPER_CLASS
    ),
    HIGH_BIRTH = new Vocation(
      "High Birth", "highborn_male_skin.gif", null, RULER_CLASS
    ),
    ALL_CLASSES[] = { PYON_BIRTH, CITIZEN_BIRTH } ;
  
  
  
  final public static Vocation
    
    EXCAVATOR = new Vocation(
      "Excavator", "pyon_skin.gif", null,
      SLAVE_CLASS,
      PRACTICED, HARD_LABOUR, NOVICE, GEOPHYSICS, ASSEMBLY,
      OFTEN, STUBBORN, RARELY, NERVOUS, HANDSOME
    ),
    
    TECHNICIAN = new Vocation(
      "Technician", "artificer_skin.gif", "artificer_portrait.png",
      LOWER_CLASS,
      PRACTICED, ASSEMBLY, LIFE_SUPPORT, NOVICE, HARD_LABOUR,
      SOMETIMES, DUTIFUL, RARELY, INDOLENT
    ),
    
    FABRICATOR = new Vocation(
      "Fabricator", "pyon_skin.gif", null,
      LOWER_CLASS,
      PRACTICED, CHEMISTRY, NOVICE, HARD_LABOUR, CHEMISTRY, GRAPHIC_MEDIA,
      RARELY, INDOLENT, SOMETIMES, STUBBORN
    ),
    
    ARTIFICER = new Vocation(
      "Artificer", "artificer_skin.gif", "artificer_portrait.png",
      UPPER_CLASS,
      EXPERT, ASSEMBLY, PRACTICED, FIELD_THEORY, CHEMISTRY,
      NOVICE, ANCIENT_LORE,
      SOMETIMES, INQUISITIVE, RARELY, NATURALIST
    )
  ;

  final public static Vocation
    
    VAT_BREEDER = new Vocation(
      "Vat Breeder", null, null, UPPER_CLASS, new Object[] {
      10, GENE_CULTURE, PHARMACY, 5, CHEMISTRY, ASSEMBLY,
      RARELY, DEBAUCHED
    }),

    PHYSICIAN = new Vocation(
      "Physician", "physician_skin.gif", "physician_portrait.png",
      UPPER_CLASS,
      EXPERT, ANATOMY, PHARMACY, PRACTICED, GENE_CULTURE, LOGOS_MENSA,
      NOVICE, COUNSEL, SUASION,
      OFTEN, INQUISITIVE, SOMETIMES, HONOURABLE, IMPASSIVE, RARELY, DEBAUCHED
    )
  ;
  
  final public static Vocation
    
    FIELD_HAND = new Vocation(
      "Field Hand", "pyon_skin.gif", null,
      LOWER_CLASS,
      PRACTICED, CULTIVATION, HARD_LABOUR, NOVICE, DOMESTIC_SERVICE,
      OFTEN, SOCIABLE, SOMETIMES, NATURALIST, RARELY, AMBITIOUS
    ),
    
    BOTANIST = new Vocation(
      "Botanist", "ecologist_skin.gif", "ecologist_portrait.png",
      UPPER_CLASS,
      EXPERT, CULTIVATION, PRACTICED, GENE_CULTURE,
      NOVICE, XENOBIOLOGY, GEOPHYSICS, CHEMISTRY,
      OFTEN, NATURALIST, SOMETIMES, EMPATHIC, RARELY, INDOLENT
    )
  ;
  
  final public static Vocation

    FRONTMAN = new Vocation(
      "Frontman", "vendor_skin.gif", "vendor_portrait.png",
      LOWER_CLASS,
      PRACTICED, COUNSEL, SUASION, NOVICE, DOMESTIC_SERVICE, CHEMISTRY,
      ADMINISTRATION,
      SOMETIMES, ACQUISITIVE
    ),
    
    SUPPLY_CORPS = new Vocation(
      "Supply Corps", "pyon_skin.gif", null,
      LOWER_CLASS,
      NOVICE, PILOTING, ASSEMBLY, HARD_LABOUR,
      OFTEN, INDOLENT, RARELY, AMBITIOUS
    ),
    
    STOCK_VENDOR = new Vocation(
      "Stock Vendor", "vendor_skin.gif", "vendor_portrait.png",
      LOWER_CLASS,
      NOVICE, SUASION, HARD_LABOUR, ADMINISTRATION
    ),
    
    AUDITOR = new Vocation(
      "Auditor", "vendor_skin.gif", "vendor_portrait.png",
      UPPER_CLASS,
      EXPERT, COUNSEL, ADMINISTRATION, PRACTICED, COMMAND, LOGOS_MENSA,
      ALWAYS, STUBBORN, DUTIFUL,
      SOMETIMES, AMBITIOUS, IMPASSIVE, RARELY, DEBAUCHED
    ) ;
  
  
  final public static Vocation
    
    //  Volunteer, Mercenary, Mech Trooper, etc.
    
    MILITANT = new Vocation(  //  Rename to Veteran.
      "Militant", "militant_skin.gif", "militant_portrait.png",
      UPPER_CLASS,
      EXPERT, CLOSE_COMBAT, PRACTICED, MARKSMANSHIP, SURVEILLANCE,
      BATTLE_TACTICS, NOVICE, HARD_LABOUR, COMMAND,
      OFTEN, DUTIFUL, SOMETIMES, STUBBORN, AMBITIOUS
    ) ;
  
  
  final public static Vocation
    
    PROPAGANDIST = new Vocation(
      "Propagandist", "citizen_skin.gif", null,
      UPPER_CLASS,
      EXPERT, SUASION, GRAPHIC_MEDIA, NOVICE, ADMINISTRATION,
      NEVER, HONOURABLE, RARELY, INDOLENT, STUBBORN, OFTEN, AMBITIOUS
    ),
    
    COMPANION = new Vocation(
      "Companion", "aesthete_male_skin.gif", "aesthete_portrait.png",
      UPPER_CLASS,
      EXPERT, CARNAL_PLEASURE, COUNSEL, SUASION, DISGUISE,
      PRACTICED, DOMESTIC_SERVICE, MUSIC_AND_SONG,
      ALWAYS, HANDSOME, OFTEN, FEMININE, EMPATHIC, TALL, RARELY, STOUT
    ) ;
  
  
  final public static Vocation
    ALL_VOCATIONS[] = (Vocation[]) all.toArray(Vocation.class) ;
  
  
  
  
  
  final public String name ;
  final public Texture costume, portrait ;
  
  final public int standing ;
  Table <Skill, Integer> baseSkills = new Table() ;
  Table <Trait, Float> traitChances = new Table() ;
  List <Item.Type> gear = new List() ;
  
  
  
  Vocation(
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
      else if (o instanceof Item.Type) {
        gear.add((Item.Type) o) ;
      }
    }
    all.add(this) ;
  }
  
  
  public String toString() {
    return name ;
  }
}












