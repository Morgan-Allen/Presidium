/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.common ;
import src.util.* ;
import java.io.* ;
import java.lang.reflect.* ;



/**  Models are shared between sprites as a way to encapsulate common geometry
  *  information, and other miscellaneous parameters- scaling, bounds, etc.
  *  Every sprite corresponds to a particular model.
  *  NOTE: ALL MODELS SHOULD BE DEFINED AS STATIC CONSTANTS TO ENSURE THEY CAN
  *  BE LOADED AND CACHED CORRECTLY.
  */
public abstract class Model {
  
  
  /**  These methods are associated with the saving, caching and loading of any
    *  models used by various game sprites.
    */
  static Table <String, Model>
    modelCache = new Table <String, Model> (1000) ;
  static Table <Integer, Model>
    IDModels = new Table <Integer, Model> (1000) ;
  static Table <Model, Integer>
    modelIDs = new Table <Model, Integer> (1000) ;
  static int counterID = 0 ;
  
  
  
  public static void clearModelIDs() {
    IDModels.clear() ;
    modelIDs.clear() ;
    counterID = 0 ;
  }
  
  
  public static void saveModel(
    Model model,
    DataOutputStream out
  ) throws Exception {
    if (model == null) { out.writeInt(-1) ; return ; }
    final Integer modelID = modelIDs.get(model) ;
    if (modelID != null) { out.writeInt(modelID) ; return ; }
    final int newID = counterID++ ;
    modelIDs.put(model, newID) ;
    out.writeInt(newID) ;
    LoadService.writeString(out, model.modelName) ;
    LoadService.writeString(out, model.modelClass.getName()) ;
  }
  
  
  public static Model loadModel(
    DataInputStream in
  ) throws Exception {
    final int modelID = in.readInt() ;
    if (modelID == -1) return null ;
    Model loaded = IDModels.get(modelID) ;
    if (loaded != null) return loaded ;
    final String modelName = LoadService.readString(in) ;
    final String className = LoadService.readString(in) ;
    ClassLoader.getSystemClassLoader().loadClass(className) ;
    loaded = modelCache.get(modelName) ;
    if (loaded == null)
      I.complain("MODEL NO LONGER DEFINED IN SPECIFIED CLASS: "+className) ;
    IDModels.put(modelID, loaded) ;
    return loaded ;
  }
  
  

  public static void saveSprite(
    Sprite sprite,
    DataOutputStream out
  ) throws Exception {
    if (sprite == null) { saveModel(null, out) ; return ; }
    final Model model = sprite.model() ;
    if (model == null) I.complain("Sprite must have model!") ;
    saveModel(model, out) ;
    sprite.saveTo(out) ;
  }
  
  
  public static Sprite loadSprite(
    DataInputStream in
  ) throws Exception {
    final Model model = loadModel(in) ;
    if (model == null) return null ;
    final Sprite sprite = model.makeSprite() ;
    sprite.loadFrom(in) ;
    return sprite ;
  }
  
  
  

  /**  Field definitions and basic accessors-
    */
  final public static String
    SPRITE_FORMATS[] = { "MS3D", "IMAGE" },
    SPRITE_TYPES[]   = { "FLAT", "MODEL", "JOINT" } ;
  final public static byte
    FORMAT_MS3D  = 0,
    FORMAT_IMAGE = 1,
    TYPE_2D      = 0,
    TYPE_MODEL   = 1,
    TYPE_JOINT   = 2 ;
  
  
  final public String modelName ;
  final public Class modelClass ;
  protected float
    scale = 1,
    bound = 1,
    stride = 1,
    selectBound = 1 ;
  protected int
    format,
    type ;
  protected Stack <AnimRange> animRanges = new Stack <AnimRange> () ;
  protected Stack <AttachPoint> attachPoints = new Stack <AttachPoint> () ;
  
  
  public float scale() { return scale ; }
  public float bound() { return bound ; }
  public float stride() { return stride ; }
  public float selectBound() { return selectBound ; }
  public int format() { return format ; }
  public int type() { return type ; }
  public Stack <AnimRange> animRanges() { return animRanges ; }
  public Stack <AttachPoint> attachPoints() { return attachPoints ; }
  
  
  /**  Basic constructor and interface contract.
    */
  protected Model(String modelName, Class modelClass) {
    this.modelName = modelName ;
    this.modelClass = modelClass ;
    modelCache.put(modelName, this) ;
    LoadService.cacheResource(this, modelName) ;
  }
  
  public abstract Sprite makeSprite() ;
  
  public Colour averageHue() {
    return Colour.WHITE ;
  }
  
  
  
  /**  XML factory methods-
    */
  public Model loadXMLInfo(
    String parentPath, String modelName
  ) {
    XML parentInfo = XML.load(parentPath) ;
    final XML modelInfo ;
    if (modelName == null) modelInfo = parentInfo.child(0) ;
    else modelInfo = parentInfo.matchChildValue("name", modelName) ;
    if (modelInfo == null) return this ;
    //
    //  If a suitable template is available, take in the data-
    this.scale = modelInfo.getFloat("scale") ;
    this.bound = modelInfo.getFloat("bound") ;
    this.stride = modelInfo.getFloat("stride") ;
    this.selectBound = this.bound * 0.5f ;  //for now...
    //
    //  Finally, load other forms of data and return-
    loadAnimRanges(modelInfo.child("animations")) ;
    loadAttachPoints(modelInfo.child("attachPoints")) ;
    return this ;
  }
  
  
  /**  Loads animation ranges from the given XML definition.
    */
  public static class AnimRange {
    
    final public String name ;
    final public float start, end, duration ;
    
    public AnimRange(String n, float s, float e, float d) {
      name = n ; start = s ; end = e ; duration = d ;
    }
  }
  
  
  public AnimRange rangeWithName(String name) {
    for (AnimRange range : animRanges) {
      if (range.name.equals(name)) return range ;
    }
    return null ;
  }
  
  
  public static interface AnimNames {
    static String
      STAND      = "stand",
      LOOK       = "look",
      MOVE       = "move",
      MOVE_FAST  = "move_fast",
      MOVE_SNEAK = "move_sneak",
      
      FIRE       = "fire",
      EVADE      = "evade",
      BLOCK      = "block",
      STRIKE     = "strike",
      STRIKE_BIG = "strike_big",
      FALL       = "fall",
      
      TALK       = "talk",
      TALK_LONG  = "talk_long",
      
      BUILD      = "build",
      REACH_DOWN = "reach_down"
    ;
  }
  
  
  private static Table <String, String> validAnimNames = null ;
  
  
  public void loadAnimRanges(XML anims) {
    if (anims == null) return ;
    //
    //  If neccesary, initialise the table of valid animation names-
    if (validAnimNames == null) {
      validAnimNames = new Table <String, String> (100) ;
      for (Field field : AnimNames.class.getFields()) try {
        if (field.getType() != String.class) continue ;
        final String value = (String) field.get(null) ;
        validAnimNames.put(value, value) ;
      }
      catch (Exception e) {}
    }
    //
    //  Quit if there are no animations to load.  Otherwise, check each entry-
    if (anims == null || anims.numChildren() < 0) return ;
    addLoop: for (XML anim : anims.children()) {
      //
      //  First, check to ensure that this animation has an approved name:
      String name = anim.value("name") ;
      if (validAnimNames.get(name) == null) I.say(
        "WARNING: ANIMATION WITH IRREGULAR NAME: "+name+
        " IN MODEL: "+modelName
      ) ;
      else name = (String) validAnimNames.get(name) ;
      for (AnimRange oldAnim : animRanges) {
        if (oldAnim.name.equals(name)) continue addLoop ;
      }
      //
      //  Either way, define the data-
      final float
        animStart  = Float.parseFloat(anim.value("start")),
        animEnd    = Float.parseFloat(anim.value("end")),
        animLength = Float.parseFloat(anim.value("duration")) ;
      final AnimRange newAnim = new AnimRange(
        name, animStart, animEnd, animLength
      ) ;
      animRanges.addFirst(newAnim) ;
    }
  }
  
  
  /**  Loads up the series of attachment points associated with this model.
    *  Only useful for joint-models.
    */
  public static class AttachPoint {
    final public String function, pointName ;
    AttachPoint(String f, String p) { function = f ; pointName = p ; }
  }
  
  
  public void loadAttachPoints(XML points) {
    if (points == null) return ;
    for (XML point : points.children()) {
      final AttachPoint newPoint = new AttachPoint(
        point.value("function"), point.value("joint")
      ) ;
      attachPoints.addLast(newPoint) ;
    }
  }
}

