/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.planet ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.cutout.ImageModel ;
import src.util.* ;


//  TODO:  Move that 'fade-in' trick to the element class.  It looks cool.


public class Flora extends Element implements TileConstants {
  
  
  /**  Field definitions and constructors-
    */
  final static int
    MAX_GROWTH = 4 ;
  
  final Habitat habitat ;
  final int varID ;
  float inceptTime ;
  float growth ;
  
  
  
  Flora(Habitat h) {
    this.habitat = h ;
    this.varID = Rand.index(4) ;
  }
  
  public Flora(Session s) throws Exception {
    super(s) ;
    habitat = Habitat.ALL_HABITATS[s.loadInt()] ;
    varID = s.loadInt() ;
    inceptTime = s.loadFloat() ;
    growth = s.loadFloat() ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(habitat.ID) ;
    s.saveInt(varID) ;
    s.saveFloat(inceptTime) ;
    s.saveFloat(growth) ;
  }
  
  
  
  /**  Method overrides-
    */
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    inceptTime = world.currentTime() ;
  }
  
  
  public void exitWorld() {
    super.exitWorld() ;
  }
  
  
  protected void renderFor(Rendering rendering, Base base) {
    float timeGone = world().currentTime() - inceptTime ;
    timeGone += PlayLoop.frameTime() / PlayLoop.UPDATES_PER_SECOND ;
    sprite().colour = Colour.transparency(timeGone) ;
    super.renderFor(rendering, base) ;
  }
  
  
  
  /**  Attempts to seed or grow new flora at the given coordinates.
    */
  public static void tryGrowthAt(int x, int y, World world, boolean init) {
    final Tile t = world.tileAt(x, y) ;
    final Habitat h = world.terrain().habitatAt(x, y) ;
    if (h.floraModels == null) return ;
    final float growChance = h.moisture / (10f * 4) ;
    
    if ((! init) && t.owner() instanceof Flora) {
      final Flora f = (Flora) t.owner() ;
      f.incGrowth(Rand.num() * 2 * 0.1f, world) ;
    }
    
    else if ((! t.blocked()) && Rand.num() < growChance) {
      int numBlocked = 0 ;
      for (int i : N_INDEX) {
        final Tile n = world.tileAt(t.x + N_X[i], t.y + N_Y[i]) ;
        if (n == null || n.blocked()) numBlocked++ ;
        if (n != null && n.owningType() > Element.ENVIRONMENT_OWNS) {
          numBlocked = 8 ;
          break ;
        }
      }
      if (numBlocked < 2) {
        final Flora f = new Flora(h) ;
        if (init) {
          f.enterWorldAt(t.x, t.y, world) ;
          if (Rand.num() < growChance * 4) f.incGrowth(MAX_GROWTH - 1, world) ;
          else f.incGrowth(1, world) ;
          f.inceptTime = -10 ;
        }
        else if (Rand.num() < 0.1f) {
          f.enterWorldAt(t.x, t.y, world) ;
          f.incGrowth(0.5f, world) ;
        }
      }
    }
  }
  
  
  protected void incGrowth(float inc, World world) {
    growth += inc ;
    if (growth < 0 || growth >= MAX_GROWTH) exitWorld() ;
    else {
      final int tier = (int) growth ;
      final ImageModel model = habitat.floraModels[varID][tier] ;
      this.attachSprite(model.makeSprite()) ;
      inceptTime = world.currentTime() ;
    }
  }
}














