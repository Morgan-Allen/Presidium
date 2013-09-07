


package src.debug ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;
import src.util.* ;
import org.lwjgl.opengl.GL11 ;



public class DebugSprites extends ViewLoop {
  
  
  
  public static void main(String a[]) {
    DebugSprites DB = new DebugSprites() ;
    DB.run() ;
  }
  
  
  final static String
    //DIR_PATH = "media/Actors/fauna/",
    //XML_PATH = DIR_PATH+"FaunaModels.xml",
    DIR_PATH = "media/Actors/artilects/",
    XML_PATH = DIR_PATH+"ArtilectModels.xml",
    NODE_NAMES[] = {
      //"Quud", "Vareen", "Micovore"
      "Defence Drone", "Recon Drone", "Blast Drone",
      "Tripod", "Cranial", "Tesseract"
    },
    ANIM_NAME = Model.AnimNames.FALL ;
  private Batch <Sprite> sprites = new Batch <Sprite> () ;
  
  
  
  protected void setup() {
    final XML mainInfo = XML.load(XML_PATH) ;
    float i = 0.5f - (NODE_NAMES.length * 0.5f) ;
    
    for (String name : NODE_NAMES) {
      final XML kidInfo = mainInfo.matchChildValue("name", name) ;
      final String modelName = kidInfo.value("file") ;
      final Model model = MS3DModel.loadMS3D(
        this.getClass(), DIR_PATH, modelName, 1
      ).loadXMLInfo(XML_PATH, name) ;
      
      I.say("Loading model: "+name+", animations: "+model.animRanges().size()) ;
      
      final Sprite sprite = model.makeSprite() ;
      sprite.position.y = i ;
      sprite.position.x = -i / 2f ;
      sprite.position.z = -0.0f ;
      sprites.add(sprite) ;
      i++ ;
    }
    rendering.port.cameraPosition.z += 1 ;
    rendering.port.cameraZoom = 15f / NODE_NAMES.length ;
  }
  
  
  protected void update() {
    //
    //  Make sure the light is always in the right position to illuminate the
    //  models.
    GL11.glClearColor(0.0f, 1.0f, 1.0f, 1.0f) ;
    rendering.lighting.direct(
      rendering.port.viewInvert(new Vec3D(0, 0, 1))
    ) ;
    //
    //  Now, iterate across each sprite, update their animations, and draw
    //  them to the main view-
    long nanoTime = System.nanoTime() ;
    float realTime = ((int) (nanoTime / 1000)) / 1000000f ;
    ///I.say("Real time: "+realTime+", nanotime: "+nanoTime) ;
    for (Sprite sprite : sprites) {
      final Model.AnimRange range = sprite.model().rangeWithName(ANIM_NAME) ;
      if (range != null) {
        sprite.setAnimation(ANIM_NAME, (realTime / range.duration) % 1f) ;
      }
      sprite.update() ;
      rendering.addClient(sprite) ;
    }
  }
}

