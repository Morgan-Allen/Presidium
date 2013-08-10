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




public class BuildingSprite extends GroupSprite {
  
  
  
  /**  Data fields, static constants, constructors and save/load methods-
    */
  final private static Class <BuildingSprite> C = BuildingSprite.class ;
  final static Model VENUE_MODEL = new Model("building_model", C) {
    public Sprite makeSprite() { return new BuildingSprite() ; }
  } ;
  final static Model BLAST_MODEL = ImageModel.asAnimatedModel(
    C, Texture.loadTexture("media/SFX/blast_anim.gif"),
    5, 4, 1.0f, 17, 1.36f
  ) ;
  final public static ImageModel FLAG_MODEL = ImageModel.asFlatModel(
    C, Texture.loadTexture("media/GUI/flag_install.gif"),
    1.5f, 1.0f
  ) ;
  final static String DIR = "media/Buildings/artificer aura/" ;
  final public static Model SCAFF_MODELS[] = {
    ImageModel.asPoppedModel(C, DIR+"scaff_0.png", 1.0f, 1),
    ImageModel.asPoppedModel(C, DIR+"scaff_1.png", 1.2f, 1),
    ImageModel.asPoppedModel(C, DIR+"scaff_2.png", 2.2f, 1),
    ImageModel.asPoppedModel(C, DIR+"scaff_3.png", 3.2f, 1),
    ImageModel.asPoppedModel(C, DIR+"scaff_4.png", 4.2f, 1),
  } ;
  
  
  Sprite scaffolding = null ;
  Sprite baseSprite  = null ;
  List <MoteFX> flameFX = new List <MoteFX> () ;
  int size, high ;
  float condition = 0 ;
  
  
  public BuildingSprite(Sprite baseSprite, int size, int high) {
    this.size = size ;
    this.high = high ;
    this.baseSprite = baseSprite ;
    attach(baseSprite, 0, 0, 0) ;
  }
  
  
  private BuildingSprite() {
  }
  
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in) ;
    size = in.readInt() ;
    high = in.readInt() ;
    condition = in.readFloat() ;
    baseSprite = super.atIndex(in.readInt()) ;
    if (baseSprite == null) baseSprite = Model.loadSprite(in) ;
    scaffolding = super.atIndex(in.readInt()) ;
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
  }
  
  
  
  /**  Modifications and updates to sprite state-
    */
  public void update() {
    
  }
  
  //
  //  TODO:  This should be coupled to world updates, not the rendering cycle?
  public void updateCondition(float newCondition, boolean normalState) {
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
      //clearAllAttachments() ;
      scaffolding = scaffoldFor(size, high, newCondition) ;
      attach(scaffolding, 0, 0, 0) ;
    }
    else {
      //  TODO:  An actual 'grey' filter would be useful here.
      //  Grey up the sprite and add random explosion FX as damage is taken.
      final float c = (1 + (condition * condition)) / 2f ;
      this.colour = new Colour(c, c, c, 1) ;
      final float flameChance = (1 - condition) / 100f ;
      if (flameFX.size() < 1 && Rand.num() < flameChance) {
        final MoteFX flame = new MoteFX(BLAST_MODEL.makeSprite()) ;
        flame.position.setTo(position) ;
        flame.position.x += Rand.range(-size, size) / 2 ;
        flame.position.y += Rand.range(-size, size) / 2 ;
        flame.position.z += Rand.num() * (high - 1) ;
        flameFX.add(flame) ;
        I.say("Adding new flame FX at: "+position) ;
      }
      for (MoteFX flame : flameFX) {
        if (flame.progress >= 1) flameFX.remove(flame) ;
      }
    }
  }
  
  
  public Sprite baseSprite()  { return baseSprite  ; }
  public Sprite scaffolding() { return scaffolding ; }
  
  
  
  /**  Actual rendering routines-
    */
  public void renderTo(Rendering r) {
    ///I.say("Base sprite colour before: "+baseSprite.colour) ;
    super.renderTo(r) ;
    ///I.say("Base sprite colour after: "+baseSprite.colour) ;
    for (MoteFX flame : flameFX) {
      flame.progress += 0.04f ;
      if (flame.progress < 1) flame.renderTo(r) ;
    }
  }
  
  
  
  /**  Producing and updating scaffold sprites-
    */
  public static int scaffoldStage(int size, int high, float condition) {
    final int maxStages = (size - 1) * (size - 1) * high ;
    final int newStage = (int) (condition * (maxStages + 1)) ;
    return newStage ;
  }
  
  
  public static Sprite scaffoldFor(int size, int high, float condition) {
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
      for (int x = 1 ; x < size ; x++) {
        for (int y = 1 ; y < size ; y++) {
          if (++numS > stage) break loop ;
          final float h = (z * 0.9f) + 0.1f ;
          sprite.attach(SCAFF_MODELS[0], x - xoff, y - yoff, h) ;
        }
      }
    }
    return sprite ;
  }
}









/*
public void updateItemStack(Model itemModel, int amount) {
  //  TODO:  Implement.
}
//*/










