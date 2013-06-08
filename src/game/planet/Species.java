/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.planet ;
//import src.game.actors.* ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;



public class Species {
  
  
  /**  Type, instance and media definitions-
    */
  final static String
    FILE_DIR = "media/Actors/fauna/",
    XML_PATH = FILE_DIR+"FaunaModels.xml" ;
  
  public static enum Type {
    BROWSER,
    PREDATOR,
    OMNIVORE
  }
  
  final public static Species
    
    QUUD = new Species(
      "Quud", "QudPortrait.png", MS3DModel.loadMS3D(
        Species.class, FILE_DIR, "Quud.ms3d", 0.025f
      ).loadXMLInfo(XML_PATH, "Quud"),
      Type.BROWSER,
      Habitat.MEADOW, 1.0f,
      Habitat.SWAMPLANDS, 0.5f,
      Habitat.BARRENS, 0.5f
    ),
    
    VAREEN = new Species(
      "Vareen", "VareenPortrait.png", MS3DModel.loadMS3D(
        Species.class, FILE_DIR, "Vareen.ms3d", 0.025f
      ).loadXMLInfo(XML_PATH, "Vareen"),
      Type.OMNIVORE,
      Habitat.MEADOW, 0.5f,
      Habitat.SWAMPLANDS, 1.0f,
      Habitat.ESTUARY, 0.5f
    ),
    
    MICOVORE = new Species(
      "Micovore", "MicovorePortrait.png", MS3DModel.loadMS3D(
        Species.class, FILE_DIR, "Micovore.ms3d", 0.025f
      ).loadXMLInfo(XML_PATH, "Micovore"),
      Type.PREDATOR,
      Habitat.DESERT, 0.5f,
      Habitat.BARRENS, 1.0f,
      Habitat.MEADOW, 0.5f
    ),
    
    ALL_SPECIES[] = { QUUD, VAREEN, MICOVORE }
    ;
  
  
  
  /**  Fields and constructors.
    */
  final String name ;
  final Texture portrait ;
  final Model model ;
  
  private static int nextID = 0 ;
  final int ID = nextID++ ;
  
  final Type type ;
  final Habitat preferred[] ;
  final float prefWeights[] ;
  
  
  Species(
    String name, String portraitTex, Model model,
    Type type, Object... prefs
  ) {
    this.name = name ;
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