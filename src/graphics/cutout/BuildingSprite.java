/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.cutout ;
import src.graphics.common.* ;
import src.graphics.sfx.* ;
import src.util.* ;
import java.io.* ;
import org.lwjgl.opengl.GL11 ;




public class BuildingSprite extends GroupSprite {
  
  
  /**  Data fields, static constants, constructors and save/load methods-
    */
  final private static Class <BuildingSprite> C = BuildingSprite.class ;
  
  final static Model BUILDING_MODEL = new Model("building_model", C) {
    public Sprite makeSprite() { return new BuildingSprite() ; }
  } ;
  
  
  final public static ImageModel
    BLAST_MODEL = ImageModel.asAnimatedModel(
      C, Texture.loadTexture("media/SFX/blast_anim.gif"),
      5, 4, 1.0f, 17, 1.36f
    ),
    FLAG_MODEL = ImageModel.asFlatModel(
      C, Texture.loadTexture("media/GUI/flag_install.gif"),
      1.5f
    ) ;
  
  final static String DIR = "media/Buildings/artificer/" ;
  final public static Model SCAFF_MODELS[] = {
    ImageModel.asSolidModel(C, DIR+"scaff_0.png", 1.0f, 1),
    ImageModel.asHollowModel(C, DIR+"scaff_1.png", 1.2f, 0),
    ImageModel.asHollowModel(C, DIR+"scaff_2.png", 2.2f, 0),
    ImageModel.asHollowModel(C, DIR+"scaff_3.png", 3.2f, 0),
    ImageModel.asHollowModel(C, DIR+"scaff_4.png", 4.2f, 0),
    ImageModel.asHollowModel(C, DIR+"scaff_5.png", 5.2f, 0),
    ImageModel.asHollowModel(C, DIR+"scaff_5.png", 6.2f, 0),
    ImageModel.asHollowModel(C, DIR+"scaff_6.png", 7.2f, 0),
    ImageModel.asHollowModel(C, DIR+"scaff_6.png", 8.2f, 0),
  } ;
  
  
  Sprite scaffolding = null ;
  Sprite baseSprite  = null ;
  
  int statusDisplayIndex = -1 ;
  List <MoteFX> statusFX = new List <MoteFX> () ;
  List <ItemStack> stackFX = new List <ItemStack> () ;
  Sprite lightingFX = null ;
  
  int size, high, maxStages ;
  float condition = 0 ;
  
  
  public BuildingSprite(Sprite baseSprite, int size, int high) {
    this.size = size ;
    this.high = high ;
    this.maxStages = maxStages() ;
    this.baseSprite = baseSprite ;
    attach(baseSprite, 0, 0, 0) ;
  }
  
  
  private BuildingSprite() {}
  public Model model() { return BUILDING_MODEL ; }
  
  //
  //  NOTE:  I'm not bothering to save/load MoteFX here, since they're
  //  refreshed or toggled every frame regardless.
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in) ;
    size = in.readInt() ;
    high = in.readInt() ;
    this.maxStages = maxStages() ;
    condition = in.readFloat() ;
    
    final int baseIndex = in.readInt() ;
    if (baseIndex == -1) baseSprite = Model.loadSprite(in) ;
    else baseSprite = super.atIndex(baseIndex) ;
    scaffolding = super.atIndex(in.readInt()) ;
    lightingFX = super.atIndex(in.readInt()) ;
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out) ;
    out.writeInt(size) ;
    out.writeInt(high) ;
    out.writeFloat(condition) ;
    
    final int baseIndex = super.indexOf(baseSprite) ;
    out.writeInt(baseIndex) ;
    if (baseIndex == -1) Model.saveSprite(baseSprite, out) ;
    out.writeInt(super.indexOf(scaffolding)) ;
    out.writeInt(super.indexOf(lightingFX)) ;
  }
  
  
  
  /**  Modifications and updates to sprite state-
    */
  public void update() {
    
  }
  
  
  public void clearFX() {
    statusFX.clear() ;
    stackFX.clear() ;
    lightingFX = null ;
  }
  
  
  public void toggleLighting(
    Model lightsModel, boolean lit, float xoff, float yoff, float zoff
  ) {
    if (! lit) {
      if (lightingFX != null) detach(lightingFX) ;
      lightingFX = null ;
    }
    else {
      ///I.say("Toggling lighting on...") ;
      if (lightingFX != null && lightingFX.model() != lightsModel) {
        detach(lightingFX) ;
        lightingFX = null ;
      }
      if (lightingFX == null) {
        lightingFX = lightsModel.makeSprite() ;
        lightingFX.colour = Colour.LIGHT ;
        attach(lightingFX, xoff, yoff, zoff) ;
      }
      else {
        //  TODO:  Vary the intensity of the transparency.
      }
    }
  }
  
  
  public void toggleFX(Model model, boolean on) {
    if (on) {
      for (MoteFX FX : statusFX) if (FX.mote.model() == model) return ;
      final MoteFX FX = new MoteFX(model.makeSprite()) ;
      statusFX.add(FX) ;
      FX.position.setTo(position) ;
      FX.position.z += high + 0.5f ;
      if (statusDisplayIndex < 0) statusDisplayIndex = 0 ;
    }
    else {
      int index = 0 ;
      for (MoteFX FX : statusFX) if (FX.mote.model() == model) {
        statusFX.remove(FX) ;
        if (statusDisplayIndex >= index) statusDisplayIndex-- ;
        return ;
      }
      else index++ ;
    }
  }
  
  
  public void updateItemDisplay(
    Model itemModel, float amount, float xoff, float yoff
  ) {
    ItemStack match = null ;
    
    for (ItemStack s : stackFX) if (s.itemModel == itemModel) match = s ;
    if (amount < 1) { if (match != null) stackFX.remove(match) ; return ; }
    if (match == null) stackFX.add(match = new ItemStack(itemModel)) ;
    
    match.updateAmount((int) amount) ;
    match.position.set(
      position.x + xoff,
      position.y + yoff,
      position.z
    ) ;
  }
  
  
  public void updateCondition(
    float newCondition, boolean normalState, boolean burning
  ) {
    //
    //  Firstly, make any necessary state transitions-
    final float oldCondition = condition ;
    condition = newCondition ;
    if (scaffolding == null && ! normalState) {
      clearAllAttachments() ;
      scaffolding = scaffoldFor(size, high, condition) ;
      attach(scaffolding, 0, 0, 0) ;
    }
    if (scaffolding != null && normalState) {
      clearAllAttachments() ;
      scaffolding = null ;
      attach(baseSprite, 0, 0, 0) ;
    }
    //
    //  Then, update appearance based on current condition-
    if (scaffolding != null) {
      final int oldStage = scaffoldStage(size, high, oldCondition) ;
      final int newStage = scaffoldStage(size, high, newCondition) ;
      if (oldStage == newStage) return ;
      clearAllAttachments() ;
      scaffolding = scaffoldFor(size, high, newCondition) ;
      attach(scaffolding, 0, 0, 0) ;
    }
    else {
      final float c = (1 - condition) ;
      this.fog *= (4 - (c * c)) / 4f ;
    }
  }
  
  
  public Sprite baseSprite()  { return baseSprite  ; }
  public Sprite scaffolding() { return scaffolding ; }
  
  
  
  /**  Actual rendering routines-
    */
  public void renderTo(Rendering rendering) {
    ///if (colour != null && colour.r < 1) I.say("  Colour: "+colour) ;
    super.renderTo(rendering) ;

    final MoteFX displayed = statusFX.atIndex(statusDisplayIndex) ;
    if (displayed != null) {
      displayed.progress += 0.04f ;
      float alpha = displayed.progress ;
      alpha = alpha * 4 * (1 - alpha) ;
      displayed.colour = Colour.transparency(alpha) ;
      
      displayed.renderTo(rendering) ;
      if (displayed.progress >= 1) {
        displayed.progress = 0 ;
        statusDisplayIndex = (statusDisplayIndex + 1) % statusFX.size() ;
      }
    }
    else statusDisplayIndex = statusFX.size() - 1 ;
    
    if (stackFX.size() > 0) {
      GL11.glDisable(GL11.GL_DEPTH_TEST) ;
      for (ItemStack stack : stackFX) stack.renderTo(rendering) ;
      GL11.glEnable(GL11.GL_DEPTH_TEST) ;
    }
  }
  
  final static int GL_DISABLES[] = new int[] { GL11.GL_LIGHTING } ;
  public int[] GL_disables() { return GL_DISABLES ; }
  
  
  
  /**  Producing and updating scaffold sprites-
    */
  private int maxStages() {
    int max = 0 ;
    for (int z = 0 ; z < high ; z++)
      for (int x = 1 ; x < (size - z) ; x++)
        for (int y = 1 + z ; y < size ; y++) max++ ;
    return max ;
  }
  
  
  private int scaffoldStage(int size, int high, float condition) {
    final int newStage = (int) (condition * (maxStages + 1)) ;
    return newStage ;
  }
  
  
  private Sprite scaffoldFor(int size, int high, float condition) {
    if (size == 1) return SCAFF_MODELS[0].makeSprite() ;
    if (condition < 0) condition = 0 ;
    if (condition > 1) condition = 1 ;
    final int stage = scaffoldStage(size, high, condition) ;
    //
    //  Otherwise, put together a composite sprite where the number of mini-
    //  scaffolds provides a visual indicator of progress.
    final GroupSprite sprite = new GroupSprite() ;
    final Model base = SCAFF_MODELS[Visit.clamp(size, SCAFF_MODELS.length)] ;
    sprite.attach(base, 0, 0, 0) ;
    final float xoff = (size / 2f), yoff = (size / 2f) ;
    int numS = 0 ;
    //
    //  Iterate over the entire coordinate space as required-
    loop: for (int z = 0 ; z < high ; z++) {
      final float l = z * 1f / high, h = z - (l * l), i = z / 2f ;
      for (int x = 1 ; x < (size - z) ; x++) {
        for (int y = 1 + z ; y < size ; y++) {
          if (++numS > stage) break loop ;
          sprite.attach(
            SCAFF_MODELS[0],
            x + i - xoff,
            y - (yoff + i),
            h
          ) ;
        }
      }
    }
    return sprite ;
  }
}







