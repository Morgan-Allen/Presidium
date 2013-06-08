



package src.graphics.jointed ;
//import src.game.actors.Actor;
import src.graphics.common.* ;



public class HumanSprite extends JointSprite {

  //
  //  TODO:  All this graphics/media related code might be more productively
  //  kept elsewhere.
  final static String
    FILE_DIR = "media/Actors/human/",
    XML_PATH = FILE_DIR+"HumanModels.xml" ;
  final public static Model
    MODEL_MALE = MS3DModel.loadMS3D(
      HumanSprite.class, FILE_DIR, "MaleAnimNewSkin.ms3d", 0.025f
    ).loadXMLInfo(XML_PATH, "MalePrime"),
    MODEL_FEMALE = MS3DModel.loadMS3D(
      HumanSprite.class, FILE_DIR, "FemaleAnimNewSkin.ms3d", 0.025f
    ).loadXMLInfo(XML_PATH, "FemalePrime") ;
  //
  //  All this stuff is intimately dependant on the layout of the collected
  //  portraits image specified- do not modify without close inspection.
  final public static Texture
    BASE_FACES = Texture.loadTexture(FILE_DIR+"face_portraits.png") ;
  final static int
    CHILD_FACE_OFF[] = {2, 0},
    UGLYS_FACE_OFF[] = {2, 1},
    F_AVG_FACE_OFF[] = {1, 1},
    F_HOT_FACE_OFF[] = {0, 1},
    M_AVG_FACE_OFF[] = {1, 0},
    M_HOT_FACE_OFF[] = {0, 0} ;
  
  final static int
    M_HAIR_OFF[][] = {{0, 5}, {1, 5}, {2, 5}, {3, 5}, {4, 5}, {5, 5}},
    F_HAIR_OFF[][] = {{0, 4}, {1, 4}, {2, 4}, {3, 4}, {4, 4}, {5, 4}} ;
  /**
    *  5- greyish  (1, 1)
    *  4- black    (0, 1)
    *  3- auburn   (0, 0)
    *  2- redhead  (1, 0)
    *  1- blonde   (2, 0)
    *  0- white    (2, 1)
    *  beards:  (3, 1) (4, 1) (5, 1)
    *  
    *  Wastes:  1-5
    *  Tundra:  2-5
    *  Forest:  3-5
    *  Desert:  4-5
    *  5 and 6 are used mainly as the actor gets older.  Women are slightly
    *  more likely to have fair tones (min of range reduced by one.)  Men are
    *  slightly more likely to have dark tones (max of range increased by one.)
    *  There's a chance of hairstyles being swapped for genders, but only men
    *  get beards (hirsute trait.)  By default, there's a bias toward lighter
    *  hair tones.
    */
  
  final static Texture BLOOD_SKINS[] = Texture.loadTextures(
    FILE_DIR+"desert_blood.gif",
    FILE_DIR+"tundra_blood.gif",
    FILE_DIR+"forest_blood.gif",
    FILE_DIR+"wastes_blood.gif"
  ) ;
  final static int BLOOD_FACE_OFFSETS[][] = {
    {3, 4}, {0, 4}, {3, 2}, {0, 2}
  } ;
  
  
  
  
  //
  //  Hmm.  This will require a lot of traits.
  //  Sex, ethnicity, costume, portrait.
  //  Later on, height and weight.
  //  ...Do I create the Composite here?
  
  public static HumanSprite spriteFor(
    boolean male, int bloodID, Texture costume
  ) {
    
    return null ;
  }
  
  
  
  
  
  
  public HumanSprite(JointModel model) {
    super(model) ;
  }
}








