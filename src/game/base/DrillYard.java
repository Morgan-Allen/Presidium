

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
//  Dummies have to be given names.  And probably a dedicated class.


public class DrillYard extends Venue {
  
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/military/" ;
  final static Model
    YARD_MODEL = ImageModel.asPoppedModel(
      DrillYard.class, IMG_DIR+"drill_yard.png", 5, 4
    ),
    DRILL_MODELS[] = ImageModel.loadModels(
      DrillYard.class, 2, 1, IMG_DIR, ImageModel.TYPE_BOX,
      "drill_melee.png",
      "drill_ranged.png",
      "drill_pilot_sim.png",
      "drill_survival.png"
    ) ;
  
  
  final public static int
    NOT_DRILLING    = -1,
    DRILL_MELEE     =  0,
    DRILL_RANGED    =  1,
    DRILL_PILOT_SIM =  2,
    DRILL_SURVIVAL  =  3,
    NUM_DRILLS      =  4,
    
    STATE_RED_ALERT =  5,
    DRILL_STATES[] = { 0, 1, 2, 3 },
    
    NUM_DUMMIES = 4 ;
  final static String DRILL_STATE_NAMES[] = {
    "Close Combat", "Target Practice", "Pilot Simulation", "Survival Course"
  } ;
  
  
  
  final public Garrison belongs ;
  protected Element dummies[] = new Element[NUM_DUMMIES] ;
  
  protected int drill = NOT_DRILLING, nextDrill = NOT_DRILLING ;
  protected int equipQuality = -1 ;
  protected boolean drillOrders[] = new boolean[NUM_DRILLS] ;
  
  
  
  
  public DrillYard(Garrison belongs) {
    super(5, 1, ENTRANCE_EAST, belongs.base()) ;
    structure.setupStats(50, 10, 25, 0, Structure.TYPE_FIXTURE) ;
    this.belongs = belongs ;
    
    final GroupSprite sprite = new GroupSprite() ;
    sprite.attach(YARD_MODEL, 0, 0, -0.05f) ;
    attachSprite(sprite) ;
    setupDummies() ;
  }
  

  public DrillYard(Session s) throws Exception {
    super(s) ;
    belongs = (Garrison) s.loadObject() ;
    for (int i = 0 ; i < NUM_DRILLS ; i++) {
      dummies[i] = (Element) s.loadObject() ;
    }
    
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
    return FIXTURE_OWNS ;
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
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    
    int numModes = 0 ;
    for (boolean b : drillOrders) if (b) numModes++ ;
    if (numModes > 0) {
      final int hour = (int) ((world.currentTime() % 1) * 12) ;
      nextDrill = hour % numModes ;
    }
  }
  
  
  public Service[] services() { return null ; }
  
  protected Background[] careers() { return null ; }
  
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
    PILOT_OFFS[] = {
      2, 0,  2, 1,  2, 2,  2, 3,
    },
    SURVIVE_OFFS[] = {
      0, 0,  0, 3,  4, 3,  4, 0,
    },
    DUMMY_OFFS[] = {
      0, 0,  0, 1,  0, 2,  0, 3
    }
  ;
  
  
  private void setupDummies() {
    final Tile o = origin() ;
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
      if (Plan.competition(Drilling.class, e, actor) > 0) continue ;
      return e ;
    }
    return null ;
  }
  
  
  protected Target nextMoveTarget(Target dummy, Actor actor) {
    float offs[] = null ; switch (drill) {
      case (DRILL_MELEE    ) : offs = MELEE_OFFS   ; break ;
      case (DRILL_RANGED   ) : offs = RANGED_OFFS  ; break ;
      case (DRILL_PILOT_SIM) : offs = PILOT_OFFS   ; break ;
      case (DRILL_SURVIVAL ) : offs = SURVIVE_OFFS ; break ;
      default: return null ;
    }
    final Tile o = origin() ;
    for (int i = dummies.length, n = 0 ; i-- > 0 ;) {
      final Tile t = world.tileAt(o.x + offs[n++], o.y + offs[n++]) ;
      if (dummies[i] == dummy) return t ;
    }
    return null ;
  }
  
  
  protected int drillDC(int drillType) {
    if (drillType != drill) return 0 ;
    return equipQuality ;
  }
  
  
  protected Upgrade bonusFor(int state) {
    switch (state) {
      case (DRILL_MELEE    ) : return Garrison.MELEE_TRAINING     ;
      case (DRILL_RANGED   ) : return Garrison.MARKSMAN_TRAINING  ;
      case (DRILL_PILOT_SIM) : return Garrison.TECHNICAL_TRAINING ;
      case (DRILL_SURVIVAL ) : return Garrison.SURVIVAL_TRAINING  ;
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
      s.attach(m.makeSprite(), -1.5f, -1.5f, 0) ;
      s.attach(m.makeSprite(), -1.5f,  0.5f, 0) ;
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







/*
private Tile nextOffFree(float offs[], Actor actor) {
  final Tile o = origin() ;
  posLoop : for (int n = 0 ; n < offs.length ;) {
    final Tile t = world.tileAt(o.x + offs[n++], o.y + offs[n++]) ;
    for (Mobile m : t.inside()) {
      if (m != actor) continue posLoop ;
    }
    return t ;
  }
  return null ;
}


protected Target nextMoveTarget(int drillType, Actor actor) {
  switch (drillType) {
    case (NOT_DRILLING) : return null ;
    case (DRILL_MELEE ) : return nextOffFree(MELEE_OFFS, actor) ;
    case (DRILL_RANGED) : return nextOffFree(RANGED_OFFS, actor) ;
  }
  return null ;
}


protected Target nextLookTarget(int drillType, Target moveTarget) {
  if (moveTarget == null) return null ;
  return Spacing.nearest(dummies, moveTarget) ;
}
//*/




