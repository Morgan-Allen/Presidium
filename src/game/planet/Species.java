/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.planet ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;


//
//  TODO:  Get rid of the habitat preferences.  That most likely needs to be
//  specified on a per-map level.

public abstract class Species {
  
  
  /**  Type, instance and media definitions-
    */
  final static String
    FILE_DIR = "media/Actors/fauna/",
    XML_PATH = FILE_DIR+"FaunaModels.xml" ;
  
  public static enum Type {
    BROWSER,
    PREDATOR,
    HUMANOID,
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
    ) { Fauna newSpecimen() { return null ; } } ,
    
    
    QUUD = new Species(
      "Quud",
      "Quud are placid, slow-moving, vegetarian browsers that rely on their "+
      "dense, leathery hides and intractable grip on the ground to protect "+
      "themselves from most predators.",
      "QuudPortrait.png",
      MS3DModel.loadMS3D(
        Species.class, FILE_DIR, "Quud.ms3d", 0.025f
      ).loadXMLInfo(XML_PATH, "Quud"),
      Type.BROWSER,
      Habitat.MEADOW, 0.5f,
      Habitat.SWAMPLANDS, 1.0f,
      Habitat.ESTUARY, 0.5f
    ) { Fauna newSpecimen() { return new Quud() ; } },
    
    
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
      Type.BROWSER,
      Habitat.DESERT, 0.5f,
      Habitat.BARRENS, 1.0f,
      Habitat.MEADOW, 0.5f
    ) { Fauna newSpecimen() { return new Vareen() ; } },
    
    
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
      Type.PREDATOR,
      Habitat.MEADOW, 1.0f,
      Habitat.SWAMPLANDS, 0.5f,
      Habitat.BARRENS, 0.5f
    ) { Fauna newSpecimen() { return new Micovore() ; } },
    
    ALL_SPECIES[] = { HUMAN, QUUD, VAREEN, MICOVORE }
    ;
  
  
  
  /**  Fields and constructors.
    */
  final String name, info ;
  final Texture portrait ;
  final Model model ;
  
  private static int nextID = 0 ;
  final int ID = nextID++ ;
  
  final Type type ;
  final Habitat preferred[] ;
  final float prefWeights[] ;
  
  
  Species(
    String name, String info, String portraitTex, Model model,
    Type type, Object... prefs
  ) {
    this.name = name ;
    this.info = info ;
    this.portrait = Texture.loadTexture(FILE_DIR+portraitTex) ;
    this.model = model ;
    
    this.type = type ;
    final int numH = prefs.length / 2 ;
    preferred = new Habitat[numH] ;
    prefWeights = new float[numH] ;
    for (int n = 0 ; n < numH ; n++) {
      preferred[n] = (Habitat) prefs[n * 2] ;
      prefWeights[n] = (Float) prefs[(n * 2) + 1] ;
    }
  }
  
  
  public float preference(Habitat h) {
    for (int n = preferred.length ; n-- > 0 ;) {
      if (preferred[n] == h) return prefWeights[n] ;
    }
    return 0 ;
  }
  
  
  abstract Fauna newSpecimen() ;
  //abstract Fixture newLair() ;
}



/*
final public static Model
  MODEL_MALE = MS3DModel.loadMS3D(
    Actor.class, FILE_DIR, "MaleAnimNewSkin.ms3d", 0.025f
  ).loadXMLInfo(XML_PATH, "MalePrime"),
  MODEL_FEMALE = MS3DModel.loadMS3D(
    Actor.class, FILE_DIR, "FemaleAnimNewSkin.ms3d", 0.025f
  ).loadXMLInfo(XML_PATH, "FemalePrime") ;
//*/


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
String name ;
Model model ;


public Species(String name) {
  super(CATEGORIC, name, null) ;
}
//*/