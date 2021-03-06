/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.planet ;
import src.game.common.* ;
import src.game.building.* ;
import src.game.wild.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.jointed.* ;
import src.util.* ;



//
//  TODO:  This class needs to include data for base speed, sight range, damage
//  and armour, et cetera, et cetera.

public class Species implements Session.Saveable, Economy {
  
  
  /**  Type, instance and media definitions-
    */
  final static String
    FILE_DIR = "media/Actors/fauna/",
    LAIR_DIR = "media/Buildings/lairs and ruins/",
    XML_PATH = FILE_DIR+"FaunaModels.xml" ;
  final public static Model
    MODEL_NEST_QUUD = ImageModel.asSolidModel(
      Species.class, LAIR_DIR+"nest_quud.png", 2.5f, 2
    ),
    MODEL_NEST_VAREEN = ImageModel.asSolidModel(
      Species.class, LAIR_DIR+"nest_vareen.png", 2.5f, 3
    ),
    MODEL_NEST_MICOVORE = ImageModel.asSolidModel(
      Species.class, LAIR_DIR+"nest_micovore.png", 3.5f, 2
    ),
    MODEL_MIDDENS[] = ImageModel.loadModels(
      Species.class, 1.25f, 1, LAIR_DIR, ImageModel.TYPE_HOLLOW_BOX,
      "midden_a.png",
      "midden_b.png",
      "midden_c.png"
    ) ;
  
  public static enum Type {
    BROWSER,
    PREDATOR,
    HUMANOID,
    FLORA
  }
  
  
  
  /**  Lists and enumeration-
    */
  private static Batch <Species>
    soFar      = new Batch <Species> (),
    allSpecies = new Batch <Species> () ;
  
  private static Species[] speciesSoFar() {
    final Species s[] = soFar.toArray(Species.class) ;
    soFar.clear() ;
    return s ;
  }
  
  private static Species[] allSpecies() {
    return allSpecies.toArray(Species.class) ;
  }
  
  
  final public static Species
    
    HUMAN = new Species(
      "Human",
      "Humans are the most common intelligent space-faring species in the "+
      "known systems of the local cluster.  According to homeworld records, "+
      "they owe their excellent visual perception, biped gait and manual "+
      "dexterity to arboreal ancestry, but morphology and appearance vary "+
      "considerably in response to a system's climate and gravity, sexual "+
      "dimorphism, mutagenic factors and history of eugenic practices. "+
      "Generally omnivorous, they make capable endurance hunters, but most "+
      "populations have shifted to agriponics and vats-culture to sustain "+
      "their numbers.  Though inquisitive and gregarious, human cultures are "+
      "riven by clannish instincts and long-running traditions of feudal "+
      "governance, spurring conflicts that may threaten their ultimate "+
      "survival.",
      null,
      null,
      Type.HUMANOID, 1, 1, 1
    ) {
      public Fauna newSpecimen() { return null ; }
      public Nest createNest() { return null ; }
    },
    
    QUD = new Species(
      "Qud",
      "Qud are placid, slow-moving, vegetarian browsers that rely on their "+
      "dense, leathery hides and intractable grip on the ground to protect "+
      "themselves from most predators.",
      "QuudPortrait.png",
      MS3DModel.loadMS3D(
        Species.class, FILE_DIR, "Quud.ms3d", 0.025f
      ).loadXMLInfo(XML_PATH, "Quud"),
      Type.BROWSER,
      1.00f, //bulk
      0.35f, //speed
      0.65f  //sight
    ) {
      public Fauna newSpecimen() { return new Quud() ; }
      public Nest createNest() { return new Nest(
        2, 2, Venue.ENTRANCE_EAST, this, MODEL_NEST_QUUD
      ) ; }
    },
    
    HAREEN = new Species(
      "Hareen",
      "Hareen are sharp-eyed aerial omnivores active by day, with a twinned "+
      "pair of wings that makes them highly maneuverable flyers.  Their "+
      "diet includes fruit, nuts, insects and carrion, but symbiotic algae "+
      "in their skin also allow them to subsist partially on sunlight.",
      "VareenPortrait.png",
      MS3DModel.loadMS3D(
        Species.class, FILE_DIR, "Vareen.ms3d", 0.025f
      ).loadXMLInfo(XML_PATH, "Vareen"),
      Type.BROWSER,
      0.50f, //bulk
      1.60f, //speed
      1.00f  //sight
    ) {
      public Fauna newSpecimen() { return new Vareen() ; }
      public Nest createNest() { return new Nest(
        2, 2, Venue.ENTRANCE_EAST, this, MODEL_NEST_VAREEN
      ) ; }
    },
    
    LICTOVORE = new Species(
      "Lictovore",
      "The Lictovore is an imposing bipedal obligate carnivore capable of "+
      "substantial bursts of speed and tackling even the most stubborn prey. "+
      "They defend established nest sites where they tend their young, using "+
      "scented middens, rich in spice, to mark the limits of their territory.",
      "MicovorePortrait.png",
      MS3DModel.loadMS3D(
        Species.class, FILE_DIR, "Micovore.ms3d", 0.025f
      ).loadXMLInfo(XML_PATH, "Micovore"),
      Type.PREDATOR,
      2.50f, //bulk
      1.30f, //speed
      1.50f  //sight
    ) {
      public Fauna newSpecimen() { return new Micovore() ; }
      public Nest createNest() { return new Nest(
        3, 2, Venue.ENTRANCE_EAST, this, MODEL_NEST_MICOVORE
      ) ; }
    },
    
    ANIMAL_SPECIES[] = Species.speciesSoFar(),
    
    
    ONI_RICE    = new Species("Oni Rice"   , Type.FLORA, 2, CARBS  ),
    DURWHEAT    = new Species("Durwheat"   , Type.FLORA, 2, CARBS  ),
    SABLE_OAT   = new Species("Sable Oat"  , Type.FLORA, 1, CARBS  ),
    
    TUBER_LILY  = new Species("Tuber Lily" , Type.FLORA, 2, GREENS ),
    BROADFRUITS = new Species("Broadfruits", Type.FLORA, 2, GREENS ),
    HIBERNUTS   = new Species("Hibernuts"  , Type.FLORA, 1, GREENS ),
    
    HIVE_GRUBS  = new Species("Hive Grubs" , Type.FLORA, 1, PROTEIN),
    BLUE_VALVES = new Species("Blue Valves", Type.FLORA, 1, PROTEIN),
    CLAN_BORE   = new Species("Clan Bore"  , Type.FLORA, 1, PROTEIN),
    
    GORG_APHID  = new Species("Gorg Aphid" , Type.FLORA, 1, SPICE  ),
    
    PIONEERS    = new Species("Pioneers"   , Type.FLORA),
    TIMBER      = new Species("Timber"     , Type.FLORA),
    
    CROP_SPECIES[] = Species.speciesSoFar(),
    ALL_SPECIES[] = Species.allSpecies()
  ;
  
  
  /**  Fields and constructors.
    */
  final public String name, info ;
  final public Texture portrait ;
  final public Model model ;
  
  private static int nextID = 0 ;
  final public int ID = nextID++ ;
  
  final public Type type ;
  final public Item nutrients[] ;
  
  final public float
    baseBulk, baseSpeed, baseSight ;
  
  
  Species(
    String name, String info, String portraitTex, Model model,
    Type type,
    float bulk, float speed, float sight
  ) {
    this.name = name ;
    this.info = info ;
    if (portraitTex == null) this.portrait = null ;
    else this.portrait = Texture.loadTexture(FILE_DIR+portraitTex) ;
    this.model = model ;
    
    this.type = type ;
    this.baseBulk = bulk ;
    this.baseSpeed = speed ;
    this.baseSight = sight ;
    nutrients = new Item[0] ;
    
    soFar.add(this) ;
    allSpecies.add(this) ;
  }
  
  
  Species(String name, Type type, Object... args) {
    this.name = name ;
    this.info = name ;
    this.portrait = null ;
    this.model = null ;
    
    this.type = type ;
    this.baseBulk = 1 ;
    this.baseSpeed = 0 ;
    this.baseSight = 0 ;
    
    int amount = 0 ;
    Batch <Item> n = new Batch <Item> () ;
    for (Object o : args) {
      if (o instanceof Integer) amount = (Integer) o ;
      if (o instanceof Service) n.add(Item.withAmount((Service) o, amount)) ;
    }
    nutrients = n.toArray(Item.class) ;
    
    soFar.add(this) ;
    allSpecies.add(this) ;
  }
  
  
  public static Session.Saveable loadConstant(Session s) throws Exception {
    return ALL_SPECIES[s.loadInt()] ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(ID) ;
  }
  
  
  public Fauna newSpecimen() {
    return null ;
  }
  
  
  public Nest createNest() {
    return null ;
  }
  
  
  public boolean browser() {
    return type == Type.BROWSER ;
  }
  
  
  public boolean predator() {
    return type == Type.PREDATOR ;
  }
  
  
  public String toString() { return name ; }
  
  
  public Item[] nutrients() { return nutrients ; }
  public float metabolism() { return baseBulk * baseSpeed ; }
}




/*
final public static Species
  //
  //  Friendlies first-
  HUMAN       = new Species("Human"),
  CHANGELING  = new Species("Changeling"),
  KRECH       = new Species("Krech"),
  JOVIAN      = new Species("Jovian"),
  //
  //  Insectiles-
  GIANT_ROACH = new Species("Giant Roach"),
  ROACHMAN    = new Species("Roachman"),
  ARAK_LANCER = new Species("Arak Lancer"),
  TERMAGANT   = new Species("Termagant"),
  //
  //  Browsers and Predators-
  QUD         = new Species("Qud"), //X
  HIREX       = new Species("Hirex"),
  LORGOX      = new Species("Trigox"),
  HAREEN      = new Species("Hareen"), //X
  DRIVODIL    = new Species("Drivodil"),
  GIGANS      = new Species("Gigans"), //Y
  LICTOVORE   = new Species("Lictovore"), //X
  DESERT_MAW  = new Species("Desert Maw"),
  CYAN_CLADE  = new Species("Cyan Clade"),
  //
  //  Artilects-
  DRONE       = new Species("Drone"), //Y
  TRIPOD      = new Species("Tripod"), //Y
  CRANIAL     = new Species("Cranial"), //Y
  MANIFOLD    = new Species("Manifold"),
  ORACLE      = new Species("Oracle"),
  OBELISK     = new Species("Obelisk"),
  //
  //  Silicates-
  REM_LEECH   = new Species("Rem Leech"),
  SILVER_HULK = new Species("Silver Hulk"),
  AGGREGANT   = new Species("Aggregant"),
  ARCHON      = new Species("Archon")
;
//*/
