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



//
//  TODO:  This class needs to include data for base speed, sight range, damage
//  and armour, et cetera, et cetera.

public class Species implements Session.Saveable {
  
  
  /**  Type, instance and media definitions-
    */
  final static String
    FILE_DIR = "media/Actors/fauna/",
    LAIR_DIR = "media/Buildings/lairs and ruins/",
    XML_PATH = FILE_DIR+"FaunaModels.xml" ;
  final public static Model
    MODEL_NEST_QUUD = ImageModel.asIsometricModel(
      Species.class, LAIR_DIR+"nest_quud.png", 2.5f, 2
    ),
    MODEL_NEST_VAREEN = ImageModel.asIsometricModel(
      Species.class, LAIR_DIR+"nest_vareen.png", 2.5f, 3
    ),
    MODEL_NEST_MICOVORE = ImageModel.asIsometricModel(
      Species.class, LAIR_DIR+"nest_micovore.png", 3.5f, 2
    ),
    MODEL_MIDDENS[] = ImageModel.loadModels(
      Species.class, 1.25f, 1, LAIR_DIR, ImageModel.TYPE_BOX,
      "midden_a.png",
      "midden_b.png",
      "midden_c.png"
    ) ;
  
  public static enum Type {
    BROWSER,
    PREDATOR,
    OMNIVORE,
    HUMANOID,
    FLORA
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
      Type.HUMANOID
    ) {
      public Fauna newSpecimen() { return null ; }
      public Lair createLair() { return null ; }
    },
    
    
    QUUD = new Species(
      "Quud",
      "Quud are placid, slow-moving, vegetarian browsers that rely on their "+
      "dense, leathery hides and intractable grip on the ground to protect "+
      "themselves from most predators.",
      "QuudPortrait.png",
      MS3DModel.loadMS3D(
        Species.class, FILE_DIR, "Quud.ms3d", 0.025f
      ).loadXMLInfo(XML_PATH, "Quud"),
      Type.BROWSER
    ) {
      public Fauna newSpecimen() { return new Quud() ; }
      public Lair createLair() { return new Lair(
        2, 2, Venue.ENTRANCE_EAST, this, MODEL_NEST_QUUD
      ) ; }
    },
    
    
    VAREEN = new Species(
      "Vareen",
      "Vareen are sharp-eyed aerial omnivores active by day, with a twinned "+
      "pair of wings that makes them highly maneuverable flyers.  Their "+
      "diet includes fruit, nuts, insects and carrion, but symbiotic algae "+
      "in their skin also allow them to subsist partially on sunlight.",
      "VareenPortrait.png",
      MS3DModel.loadMS3D(
        Species.class, FILE_DIR, "Vareen.ms3d", 0.025f
      ).loadXMLInfo(XML_PATH, "Vareen"),
      Type.BROWSER
    ) {
      public Fauna newSpecimen() { return new Vareen() ; }
      public Lair createLair() { return new Lair(
        2, 2, Venue.ENTRANCE_EAST, this, MODEL_NEST_VAREEN
      ) ; }
    },
    
    
    MICOVORE = new Species(
      "Micovore",
      "The Micovore is an imposing bipedal obligate carnivore capable of "+
      "substantial bursts of speed and tackling even the most stubborn prey. "+
      "They defend established nest sites where they tend their young, using "+
      "scented middens, rich in spice, to mark the limits of their territory.",
      "MicovorePortrait.png",
      MS3DModel.loadMS3D(
        Species.class, FILE_DIR, "Micovore.ms3d", 0.025f
      ).loadXMLInfo(XML_PATH, "Micovore"),
      Type.PREDATOR
    ) {
      public Fauna newSpecimen() { return new Micovore() ; }
      public Lair createLair() { return new Lair(
        3, 2, Venue.ENTRANCE_EAST, this, MODEL_NEST_MICOVORE
      ) ; }
    },
    
    ANIMAL_SPECIES[] = { HUMAN, QUUD, VAREEN, MICOVORE, },
    
    ONI_RICE    = new Species("Oni Rice"   , "", null, null, Type.FLORA),
    DURWHEAT    = new Species("Durwheat"   , "", null, null, Type.FLORA),
    TUBER_LILY  = new Species("Tuber Lily ", "", null, null, Type.FLORA),
    BROADFRUITS = new Species("BroadFruits", "", null, null, Type.FLORA),
    HIVE_CELLS  = new Species("Hive Cells" , "", null, null, Type.FLORA),
    MUSSEL_BEDS = new Species("Mussel Beds", "", null, null, Type.FLORA),
    PIONEERS    = new Species("Pioneers"   , "", null, null, Type.FLORA),
    SAPLINGS    = new Species("Saplings"   , "", null, null, Type.FLORA),
    CROP_SPECIES[] = {
      DURWHEAT, ONI_RICE, BROADFRUITS, TUBER_LILY,
      HIVE_CELLS, MUSSEL_BEDS, PIONEERS, SAPLINGS
    },
    
    ALL_SPECIES[] = {
      HUMAN, QUUD, VAREEN, MICOVORE,
      
      DURWHEAT, ONI_RICE, BROADFRUITS, TUBER_LILY,
      HIVE_CELLS, MUSSEL_BEDS, PIONEERS, SAPLINGS,
    }
  ;
  
  
  
  /**  Fields and constructors.
    */
  final public String name, info ;
  final public Texture portrait ;
  final public Model model ;
  
  private static int nextID = 0 ;
  final public int ID = nextID++ ;
  
  final public Type type ;
  
  
  Species(
    String name, String info, String portraitTex, Model model,
    Type type
  ) {
    this.name = name ;
    this.info = info ;
    if (portraitTex == null) this.portrait = null ;
    else this.portrait = Texture.loadTexture(FILE_DIR+portraitTex) ;
    this.model = model ;
    this.type = type ;
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
  
  
  public Lair createLair() {
    return null ;
  }
  
  
  public boolean browses() {
    return type == Type.BROWSER || type == Type.OMNIVORE ;
  }
  
  
  public boolean goesHunt() {
    return type == Type.PREDATOR || type == Type.OMNIVORE ;
  }
  
  
  public int forageRange() {
    if (type == Species.Type.BROWSER) return Lair.BROWSER_SAMPLE_RANGE ;
    else return Lair.PREDATOR_SAMPLE_RANGE ;
  }
  
  
  public int maxLairPop() {
    if (type == Species.Type.PREDATOR) return Lair.PREDATOR_LAIR_POPULATION ;
    return Lair.BROWSER_LAIR_POPULATION ;
  }
  
  
  public String toString() { return name ; }
}




/*
final public static Species
  //
  //  Friendlies first (would these count as different species, though?)-
  PRIMITIVE   = new Species("Primitive"),
  MUTANT      = new Species("Mutant"),
  INFECTED    = new Species("Infected"),
  CHANGELING  = new Species("Changeling"),
  KRECH       = new Species("Krech"),
  JOVIAN      = new Species("Jovian"),
  //
  //  Insectiles-
  GIANT_ROACH = new Species("Giant Roach"),
  ROACHMAN    = new Species("Roachman"),
  TERMAGANT   = new Species("Termagant"),
  //
  //  Browsers and Predators-
  QUD         = new Species("Qud"), //X
  TRIGOX      = new Species("Trigox"),
  HAREEN      = new Species("Hareen"), //X
  DRIVODIL    = new Species("Drivodil"),
  GIGANS      = new Species("Gigans"), //Y
  LICTOVORE   = new Species("Lictovore"), //X
  //
  //  Artilects-
  DRONE       = new Species("Drone"), //Y
  TRIPOD      = new Species("Tripod"), //X
  CRANIAL     = new Species("Cranial"),
  //
  //  Silicates-
  REM_LEECH   = new Species("Rem Leech"),
  MERCURIAL   = new Species("Mercurial"),
  ARCHON      = new Species("Archon")
;
//*/


/*
final int numH = prefs.length / 2 ;
preferred = new Habitat[numH] ;
prefWeights = new float[numH] ;
for (int n = 0 ; n < numH ; n++) {
  preferred[n] = (Habitat) prefs[n * 2] ;
  prefWeights[n] = (Float) prefs[(n * 2) + 1] ;
}
//*/
//final Habitat preferred[] ;
//final float prefWeights[] ;
/*
public float preference(Habitat h) {
  for (int n = preferred.length ; n-- > 0 ;) {
    if (preferred[n] == h) return prefWeights[n] ;
  }
  return 0 ;
}
//*/




