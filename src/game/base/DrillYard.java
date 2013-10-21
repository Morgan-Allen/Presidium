

package src.game.base ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;

//
//  TODO:  I need some method of ensuring that actors won't just walk through
//  the drill yard unless they have business there.

public class DrillYard extends Venue {
  
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/military/" ;
  final static Model
    YARD_MODEL = ImageModel.asHollowModel(
      DrillYard.class, IMG_DIR+"drill_yard.png", 4.25f, 1
    ),
    MELEE_MODEL = ImageModel.asHollowModel(
      DrillYard.class, IMG_DIR+"drill_melee.png", 2, 1
    ),
    RANGED_MODEL = ImageModel.asHollowModel(
      DrillYard.class, IMG_DIR+"drill_ranged.png", 2, 1
    ),
    ENDURE_MODEL = ImageModel.asSolidModel(
      DrillYard.class, IMG_DIR+"drill_endurance.png", 2, 1
    ),
    AID_MODEL = ImageModel.asSolidModel(
      DrillYard.class, IMG_DIR+"drill_aid_table.png", 2, 1
    ),
    DRILL_MODELS[] = {
      MELEE_MODEL, RANGED_MODEL, ENDURE_MODEL, AID_MODEL
    } ;
  
  
  final public static int
    NOT_DRILLING    = -1,
    DRILL_MELEE     =  0,
    DRILL_RANGED    =  1,
    DRILL_ENDURANCE =  2,
    DRILL_AID       =  3,
    NUM_DRILLS      =  4,
    
    STATE_RED_ALERT =  4,
    DRILL_STATES[]  = { 0, 1, 2, 3 },
    
    NUM_DUMMIES = 2,
    NUM_OFFSETS = 4,
    DRILL_INTERVAL = World.STANDARD_DAY_LENGTH ;
  
  final static String DRILL_STATE_NAMES[] = {
    "Close Combat", "Target Practice", "Endurance Course", "Aid Table"
  } ;
  
  
  
  final public Garrison belongs ;
  protected Element dummies[] = new Element[NUM_DUMMIES] ;
  
  protected int drill = NOT_DRILLING, nextDrill = NOT_DRILLING ;
  protected int equipQuality = -1 ;
  protected boolean drillOrders[] = new boolean[NUM_DRILLS] ;
  
  
  
  
  public DrillYard(Garrison belongs) {
    super(4, 1, ENTRANCE_EAST, belongs.base()) ;
    structure.setupStats(50, 10, 25, 0, Structure.TYPE_FIXTURE) ;
    this.belongs = belongs ;
    
    final GroupSprite sprite = new GroupSprite() ;
    sprite.attach(YARD_MODEL, 0, 0, -0.05f) ;
    attachSprite(sprite) ;
  }
  

  public DrillYard(Session s) throws Exception {
    super(s) ;
    belongs = (Garrison) s.loadObject() ;
    for (int i = 0 ; i < NUM_DUMMIES; i++) {
      dummies[i] = (Element) s.loadObject() ;
    }
    dummies = new Element[NUM_DUMMIES] ;
    setupDummies() ;
    drill = s.loadInt() ;
    nextDrill = s.loadInt() ;
    equipQuality = s.loadInt() ;
    for (int i = 0 ; i < NUM_DRILLS ; i++) drillOrders[i] = s.loadBool() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(belongs) ;
    for (Element e : dummies) s.saveObject(e) ;
    s.saveInt(drill) ;
    s.saveInt(nextDrill) ;
    s.saveInt(equipQuality) ;
    for (boolean b : drillOrders) s.saveBool(b) ;
  }
  
  
  
  /**  Owning/pathing modifications-
    */
  public int owningType() {
    return VENUE_OWNS ;
  }
  
  
  public int pathType() {
    return Tile.PATH_HINDERS ;
  }
  
  
  public boolean privateProperty() {
    return true ;
  }
  
  
  public boolean canPlace() {
    if (! super.canPlace()) return false ;
    //if (! Spacing.adjacent(this, belongs)) return false ;
    //if (Spacing.distance(this, belongs) > 2) return false ;
    return true ;
  }
  
  
  protected boolean canTouch(Element e) {
    return (e.owningType() < this.owningType()) || e == belongs ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    setupDummies() ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    
    int numModes = 0 ;
    for (boolean b : drillOrders) if (b) numModes++ ;
    if (numModes > 0) {
      final int mode = (int) (world.currentTime() / DRILL_INTERVAL) ;
      for (int i = 0, d = 0 ; i < drillOrders.length ; i++) {
        if (! drillOrders[i]) continue ;
        if (d == mode % numModes) {
          nextDrill = DRILL_STATES[i] ;
          break ;
        }
        else d++ ;
      }
    }
  }
  
  
  public Service[] services() { return null ; }
  
  public Background[] careers() { return null ; }
  
  public Behaviour jobFor(Actor actor) { return null ; }
  
  
  
  /**  Helping to configure drill actions-
    */
  final static float
    MELEE_OFFS[] = {
      1, 0,  1, 1,  1, 2,  1, 3,
    },
    RANGED_OFFS[] = {
      3, 0,  3, 1,  3, 2,  3, 3,
    },
    ENDURE_OFFS[] = {
      0, 0,  0, 3,  3, 3,  3, 0,
    },
    AID_OFFS[] = {
      2, 0,  2, 1,  2, 2,  2, 3,
    },
    DUMMY_OFFS[] = {
      0.5f, 2,  0.5f, 3,
    }
  ;
  private void setupDummies() {
    final Tile o = origin() ;
    if (o == null) return ;
    for (int i = NUM_DUMMIES, c = 0 ; i-- > 0 ;) {
      final Tile t = world.tileAt(
        o.x + DUMMY_OFFS[c++],
        o.y + DUMMY_OFFS[c++]
      ) ;
      if (dummies[i] == null) dummies[i] = new Element(t, null) ;
    }
  }
  
  
  protected Target nextDummyFree(int drillType, Actor actor) {
    for (Element e : dummies) {
      if (Plan.competition(Training.class, e, actor) > 0) continue ;
      return e ;
    }
    return null ;
  }
  
  
  protected Target nextMoveTarget(Target dummy, Actor actor) {
    float offs[] = null ; switch (drill) {
      case (DRILL_MELEE    ) : offs = MELEE_OFFS  ; break ;
      case (DRILL_RANGED   ) : offs = RANGED_OFFS ; break ;
      case (DRILL_ENDURANCE) : offs = ENDURE_OFFS ; break ;
      case (DRILL_AID      ) : offs = AID_OFFS    ; break ;
      default: return null ;
    }
    final Tile o = origin(), a = actor.origin() ;
    Tile pick = null ;
    for (int i = 0, n = 0 ; i < NUM_OFFSETS ; i++) {
      final Tile t = world.tileAt(o.x + offs[n++], o.y + offs[n++]) ;
      if (pick == null) pick = t ;
      if (t == a) {
        n = ((i + 1) % NUM_OFFSETS) * 2 ;
        return world.tileAt(o.x + offs[n++], o.y + offs[n++]) ;
      }
    }
    return pick ;
  }
  
  
  protected int drillDC(int drillType) {
    if (drillType != drill) return 0 ;
    return equipQuality ;
  }
  
  
  protected Upgrade bonusFor(int state) {
    switch (state) {
      case (DRILL_MELEE    ) : return Garrison.MELEE_TRAINING     ;
      case (DRILL_RANGED   ) : return Garrison.MARKSMAN_TRAINING  ;
      case (DRILL_ENDURANCE) : return Garrison.ENDURANCE_TRAINING ;
      case (DRILL_AID      ) : return Garrison.AID_TRAINING       ;
    }
    return null ;
  }
  
  
  public int drillType() {
    return drill ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected void updateSprite() {
    final GroupSprite s = (GroupSprite) buildSprite().baseSprite() ;
    final Model m =
      (drill == NOT_DRILLING || drill == STATE_RED_ALERT) ?
      null : DRILL_MODELS[drill] ;
    
    final ImageSprite old1 = (ImageSprite) s.atIndex(1) ;
    if (old1 != null && old1.model() == m) return ;
    if (old1 != null) {
      final Sprite old2 = s.atIndex(2) ;
      s.detach(old1) ;
      s.detach(old2) ;
      world.ephemera.addGhost(null, 2, old1, 1.0f) ;
      world.ephemera.addGhost(null, 2, old2, 1.0f) ;
    }
    
    if (m != null) {
      //
      //  TODO:  Fade in transparently too...
      s.attach(m.makeSprite(), -0.5f, -1.5f, 0) ;
      s.attach(m.makeSprite(), -0.5f,  0.5f, 0) ;
    }
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/garrison_button.gif") ;
  }
  
  
  public String fullName() {
    return "Drill Yard" ;
  }
  
  
  public String helpInfo() {
    return
      "Soldiers from your garrison and other military structures will gather "+
      "to practice at your drill yard." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_MILITANT ;
  }
  
  
  public void writeInformation(Description d, int categoryID, HUD UI) {
    super.writeInformation(d, categoryID, UI) ;
    if (categoryID == 0) {
      d.append("\n\nDrill Orders:") ;
      for (final int s : DRILL_STATES) {
        d.append("\n  ") ;
        d.append(new Description.Link(DRILL_STATE_NAMES[s]) {
          public void whenClicked() {
            drillOrders[s] = ! drillOrders[s] ;
          }
        }) ;
        if (drillOrders[s]) d.append(" (Scheduled)") ;
      }
      if (nextDrill != NOT_DRILLING) {
        d.append("\n\nNext Drill: "+DRILL_STATE_NAMES[nextDrill]) ;
      }
    }
  }
}





