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
    ALWAYS    =  0.9f,
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
    
    EXCAVATOR = new Vocation("Excavator", "pyon_skin.gif", SLAVE_CLASS,
      PRACTICED, HARD_LABOUR, NOVICE, GEOPHYSICS, ASSEMBLY,
      OFTEN, STUBBORN, RARELY, NERVOUS, HANDSOME
    ),
    
    TECHNICIAN = new Vocation("Technician", "artificer_skin.gif", LOWER_CLASS,
      PRACTICED, ASSEMBLY, LIFE_SUPPORT, NOVICE, HARD_LABOUR,
      SOMETIMES, DUTIFUL, RARELY, INDOLENT
    ),
    
    FABRICATOR = new Vocation("Fabricator", "pyon_skin.gif", LOWER_CLASS,
      PRACTICED, CHEMISTRY, NOVICE, HARD_LABOUR, CHEMISTRY, GRAPHIC_MEDIA,
      RARELY, INDOLENT, SOMETIMES, STUBBORN
    ),
    
    ARTIFICER = new Vocation("Artificer", "artificer_skin.gif", UPPER_CLASS,
      EXPERT, ASSEMBLY, PRACTICED, FIELD_THEORY, CHEMISTRY,
      NOVICE, ANCIENT_LORE,
      SOMETIMES, INQUISITIVE, RARELY, LOVING
    )
  ;

  final public static Vocation
    
    VAT_BREEDER = new Vocation("Vat Breeder", null, UPPER_CLASS, new Object[] {
      10, GENE_CULTURE, PHARMACY, 5, CHEMISTRY, ASSEMBLY,
      RARELY, DEBAUCHED
    }),

    PHYSICIAN = new Vocation("Physician", null, UPPER_CLASS, new Object[] {
      15, ANATOMY, PHARMACY, 10, GENE_CULTURE, LOGOS_MENSA, 5, COUNSEL,
      OFTEN, INQUISITIVE, SOMETIMES, HONOURABLE, IMPASSIVE, RARELY, DEBAUCHED
    })
  ;
  
  final public static Vocation
    
    FIELD_HAND = new Vocation("Field Hand", null, SLAVE_CLASS, new Object[] {
      10, CULTIVATION, HARD_LABOUR, 5, DOMESTIC_SERVICE,
      OFTEN, SOCIABLE, RARELY, AMBITIOUS
    }),
    
    BOTANIST = new Vocation("Botanist", null, UPPER_CLASS, new Object[] {
      15, CULTIVATION, 10, GENE_CULTURE, 5, XENOBIOLOGY, GEOPHYSICS,
      CHEMISTRY,
      SOMETIMES, EMPATHIC
    })
  ;
  
  final public static Vocation

    FRONTMAN = new Vocation("Frontman", null, LOWER_CLASS, new Object[] {
      5, COUNSEL, SUASION, 10, DOMESTIC_SERVICE, 5, CHEMISTRY,
      ADMINISTRATION,
      SOMETIMES, ACQUISITIVE
    }),
    
    SUPPLY_CORPS = new Vocation("Supply Corps", null, LOWER_CLASS, new Object[] {
      5, PILOTING, ASSEMBLY, HARD_LABOUR,
      OFTEN, INDOLENT, SOMETIMES,
    }),
    
    STOCK_VENDOR = new Vocation("Stock Vendor", null, LOWER_CLASS, new Object[] {
      5, SUASION, HARD_LABOUR, ADMINISTRATION,
    }),
    
    AUDITOR = new Vocation("Auditor", null, UPPER_CLASS, new Object[] {
      15, COUNSEL, COMMAND, 10, ADMINISTRATION,
      5, LOGOS_MENSA,
      ALWAYS, STUBBORN, DUTIFUL,
      SOMETIMES, AMBITIOUS, IMPASSIVE, RARELY, DEBAUCHED
    }) ;
  
  final public static Vocation
    
    MILITANT = new Vocation("Militant", null, UPPER_CLASS, new Object[] {
      15, CLOSE_COMBAT, 5, HARD_LABOUR,
      10, MARKSMANSHIP, SURVEILLANCE, BATTLE_TACTICS,
      OFTEN, DUTIFUL, AMBITIOUS, SOMETIMES, STUBBORN,
    }) ;
  
  
  final public static Vocation
    
    PROPAGANDIST = new Vocation("Propagandist", null, UPPER_CLASS, new Object[] {
      15, SUASION, GRAPHIC_MEDIA, 5, ASSEMBLY,
      NEVER, HONOURABLE, RARELY, INDOLENT, STUBBORN, OFTEN, AMBITIOUS
    }),
    
    COMPANION = new Vocation("Companion", null, UPPER_CLASS, new Object[] {
      15, CARNAL_PLEASURE, COUNSEL, SUASION, DISGUISE,
      10, DOMESTIC_SERVICE, MUSIC_AND_SONG,
      ALWAYS, HANDSOME, RARELY, STOUT, OFTEN, FEMININE, EMPATHIC, TALL,
      SOMETIMES, INDOLENT, AMBITIOUS,
    }) ;
  
  final public static Vocation
    ALL_VOCATIONS[] = (Vocation[]) all.toArray(Vocation.class) ;
  
  
  
  
  
  final public String name ;
  final public Texture costume ;
  
  final public int standing ;
  Table <Skill, Integer> baseSkills = new Table() ;
  Table <Trait, Float> traitChances = new Table() ;
  List <Item.Type> gear = new List() ;
  
  
  
  Vocation(String name, String costumeTex, int standing, Object... args) {
    this.name = name ;
    if (costumeTex == null) this.costume = null ;
    else this.costume = Texture.loadTexture(COSTUME_DIR+costumeTex) ;
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
  
  
  public void configTraits(Actor actor) {
    for (Skill s : baseSkills.keySet()) {
      final int level = baseSkills.get(s) ;
      actor.training.raiseTo(level + Rand.index(5), s) ;
    }
    for (Trait t : traitChances.keySet()) {
      final float chance = traitChances.get(t) ;
      actor.training.raiseBy(chance * Rand.num() * 2, t) ;
    }
  }
  
  
}












