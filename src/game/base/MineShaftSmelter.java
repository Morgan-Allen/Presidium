/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.user.* ;
import src.util.* ;



public class MineShaftSmelter extends Venue implements VenueConstants {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/artificer/" ;
  final static Model 
    DRILL_MODELS[] = ImageModel.loadModels(
      MineShaftSmelter.class, 3, 3, IMG_DIR,
      "isotopes_smelter.gif",
      "metals_smelter.gif",
      "carbons_smelter.gif"
    ),
    ALL_MOLD_MODELS[][] = ImageModel.fromTextureGrid(
      MineShaftSmelter.class, Texture.loadTexture(IMG_DIR+"all_molds.png"),
      4, 5, 1, ImageModel.TYPE_FLAT
    ) ;
  final static int
    MOLD_COORDS[] = {
      0, 0,
      1, 0,
      2, 0,
      3, 0,
      3, 1,
      3, 2,
      3, 3
    },
    NUM_MOLDS = MOLD_COORDS.length / 2,
    MAX_MOLD_LEVEL = 4 ;
  
  final static int
    MOLD_COOL_TIME = 8 ;
  final static Item.Type
    MINED_TYPES[] = { ISOTOPES, METALS, CARBONS } ;
  
  
  static class Mold {
    int coolTime = -1 ;
    float minerals = 0 ;
  }
  
  
  final Item.Type mined ;
  final int variant ;
  final Mold molds[] = new Mold[NUM_MOLDS] ;
  
  
  public MineShaftSmelter(Base belongs, int variant) {
    super(4, 3, Venue.ENTRANCE_NORTH, belongs) ;
    this.variant = variant ;
    this.mined = MINED_TYPES[variant] ;
    initSprite() ;
    initMolds() ;
  }
  
  
  public MineShaftSmelter(Session s) throws Exception {
    super(s) ;
    variant = s.loadInt() ;
    mined = MINED_TYPES[variant] ;
    initMolds() ;
    for (Mold m : molds) {
      m.coolTime = s.loadInt() ;
      m.minerals = s.loadFloat() ;
    }
    initSprite() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(variant) ;
    for (Mold m : molds) {
      s.saveInt(m.coolTime) ;
      s.saveFloat(m.minerals) ;
    }
  }
  
  
  private void initMolds() {
    for (int n = NUM_MOLDS ; n-- > 0 ;) {
      molds[n] = new Mold() ;
    }
  }
  
  
  
  /**  Behaviour implementation.
    */
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    for (Mold m : molds) {
      if (m.coolTime < 0) continue ;
      m.coolTime = Math.min(m.coolTime + 1, MOLD_COOL_TIME) ;
    }
  }


  protected Item.Type[] services() {
    return new Item.Type[] { mined } ;
  }
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.EXCAVATOR } ;
  }
  
  
  public Behaviour jobFor(Actor actor) {

    final Delivery d = orders.nextDelivery(actor, services()) ;
    if (d != null) return d ;
    
    if (stocks.amountOf(mined) > 50) return null ;
    
    boolean anyEmpty = false, allFull = true ;
    for (Mold m : molds) {
      if (m.coolTime == -1) anyEmpty = true ;
      if (m.coolTime < MOLD_COOL_TIME) allFull = false ;
    }
    
    if (anyEmpty) {
      return new Action(
        actor, this,
        this, "actionDrill",
        Action.BUILD, "Drilling for "+mined.name
      ) ;
    }
    if (allFull) {
      return new Action(
        actor, this,
        this, "actionCollect",
        Action.BUILD, "Collecting "+mined.name
      ) ;
    }
    
    return null ;
  }
  
  
  private float avgMinerals() {
    float sum = 0, count = 0 ;
    for (Tile t : world().tilesIn(area(), false)) {
      sum += t.habitat().minerals() ;
      count++ ;
    }
    return sum / count ;
  }
  
  
  public boolean actionDrill(Actor actor, MineShaftSmelter drilling) {
    //I.say("Performing drill action...") ;
    for (Mold m : molds) {
      if (m.coolTime != -1) continue ;
      m.coolTime = 0 ;
      m.minerals = 1 ;
      m.minerals += actor.traits.test(GEOPHYSICS, 5, 1) ? 1 : 0 ;
      m.minerals += actor.traits.test(HARD_LABOUR, 10, 1) ? 1 : 0 ;
      return true ;
    }
    return false ;
  }
  
  
  public boolean actionCollect(Actor actor, Venue v) {
    float collected = 0 ;
    for (Mold m : molds) {
      collected += m.minerals ;
      m.coolTime = -1 ;
      m.minerals = 0 ;
    }
    collected *= (avgMinerals() / NUM_MOLDS) ;
    stocks.addItem(mined, collected) ;
    return true ;
  }
  


  /**  Rendering and interface methods-
    */
  private void initSprite() {
    final GroupSprite s ;
    if (sprite() == null) s = new GroupSprite() ;
    else s = (GroupSprite) sprite() ;
    final float xo = -1.5f, yo = -1.5f ;
    s.attach(DRILL_MODELS[variant], 1 + xo, 2 + xo, 0) ;
    for (int i = 0 ; i < MOLD_COORDS.length ;) {
      s.attach(ALL_MOLD_MODELS[variant][0],
        MOLD_COORDS[i++] + xo,
        MOLD_COORDS[i++] + yo,
      0) ;
    }
    attachSprite(s) ;
  }
  
  
  public void renderFor(Rendering rendering, Base base) {
    final GroupSprite s = (GroupSprite) sprite() ;
    for (int i = 0 ; i < NUM_MOLDS ; i++) {
      final ImageSprite module = (ImageSprite) s.atIndex(i + 1) ;
      final int cooled = molds[i].coolTime ;
      final Model model ;
      if (cooled == -1) model = ALL_MOLD_MODELS[variant][0] ;
      else {
        float moldLevel = cooled * 1f / MOLD_COOL_TIME ;
        moldLevel *= (MAX_MOLD_LEVEL - 1) ;
        model = ALL_MOLD_MODELS[variant][(int) (moldLevel + 1)] ;
      }
      module.setModel((ImageModel) model) ;
    }
    super.renderFor(rendering, base) ;
  }
  
  
  public Composite portrait(BaseUI UI) {
    return new Composite(UI, "media/GUI/Buttons/excavation_button.gif") ;
  }
  
  
  public String fullName() { return mined.name+" Drill" ; }
  
  
  public String helpInfo() {
    return
      mined.name+" Drills extract large quantities of "+mined.name+
      " from subterranean mineral deposits.";
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ARTIFICER ;
  }
}



















