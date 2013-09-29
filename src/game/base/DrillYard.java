

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





public class DrillYard extends Venue {
  
  
  
  /**  Constructors, data fields, setup and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/military/" ;
  final static Model
    YARD_MODEL = ImageModel.asPoppedModel(
      DrillYard.class, IMG_DIR+"drill_yard.png", 5, 4
    ),
    DRILL_MODELS[] = ImageModel.loadModels(
      DrillYard.class, 2, 1, IMG_DIR, ImageModel.TYPE_POPPED_BOX,
      "drill_melee.png",
      "drill_ranged.png",
      "drill_pilot_sim.png",
      "drill_survival.png"
    ) ;
  
  
  final public static int
    STATE_NONE            = 0,
    STATE_DRILL_MELEE     = 1,
    STATE_DRILL_RANGED    = 2,
    STATE_DRILL_PILOT_SIM = 3,
    STATE_DRILL_SURVIVAL  = 4,
    STATE_RED_ALERT       = 5,
    ALL_STATES[] = { 0, 1, 2, 3, 4, 5 } ;
  final static String DRILL_STATE_NAMES[] = {
    "None", "Melee", "Ranged", "Pilot Sim", "Survival", "RED ALERT"
  } ;
  
  
  
  final Garrison belongs ;
  private int state = STATE_NONE ;
  
  
  public DrillYard(Garrison belongs) {
    super(5, 0, ENTRANCE_EAST, belongs.base()) ;
    structure.setupStats(50, 10, 25, 0, Structure.TYPE_FIXTURE) ;
    this.belongs = belongs ;
    
    final GroupSprite sprite = new GroupSprite() ;
    sprite.attach(YARD_MODEL, 0, 0, -0.05f) ;
    attachSprite(sprite) ;
  }
  

  public DrillYard(Session s) throws Exception {
    super(s) ;
    belongs = (Garrison) s.loadObject() ;
    state = s.loadInt() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(belongs) ;
    s.saveInt(state) ;
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
  }
  
  
  public Service[] services() { return null ; }
  
  protected Background[] careers() { return null ; }
  
  public Behaviour jobFor(Actor actor) { return null ; }
  
  
  protected void switchDrillState(int newState) {
    final int oldState = state ;
    state = newState ;
    if (newState != oldState) updateSprite() ;
  }
  
  
  
  final static float
    MELEE_OFFS[] = {
      1, 0,  1, 1,  1, 2,  1, 3,  1, 5
    },
    RANGED_OFFS[] = {
      3, 0,  3, 1,  3, 2,  3, 3,  3, 5
    },
    PILOT_OFFS[] = {
      2, 0,  2, 1,  2, 2,  2, 3,  2, 5
    },
    SURVIVE_OFFS[] = {
      0, 0,  0, 3,  4, 3,  4, 0,  2, 1.5f
    },
    DUMMY_OFFS[][] = {
      {0.5f, 1.0f},  {0.5f, 3.0f}
    }
  ;
  
  
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
      case (STATE_NONE) : return null ;
      case (STATE_DRILL_MELEE ) : return nextOffFree(MELEE_OFFS, actor) ;
      case (STATE_DRILL_RANGED) : return nextOffFree(RANGED_OFFS, actor) ;
    }
    return null ;
  }
  
  
  protected Target nextLookTarget(int drillType, Target moveTarget) {
    if (moveTarget == null) return null ;
    //
    //  Return whichever dummy is closest.
    final Batch <Target> dummies = new Batch <Target> () ;
    for (float c[] : DUMMY_OFFS) {
      dummies.add(world.tileAt(c[0], c[1])) ;
    }
    return Spacing.nearest(dummies, moveTarget) ;
  }
  
  
  private int bonus(Upgrade u) {
    return (belongs.structure.upgradeLevel(u) + 1) * 5 / 2 ;
  }
  
  
  protected int drillDC(int drillType) {
    switch (drillType) {
      case (STATE_NONE) : return 0 ;
      case (STATE_DRILL_MELEE    ) : return bonus(Garrison.MELEE_TRAINING    ) ;
      case (STATE_DRILL_RANGED   ) : return bonus(Garrison.MARKSMAN_TRAINING ) ;
      case (STATE_DRILL_PILOT_SIM) : return bonus(Garrison.TECHNICAL_TRAINING) ;
      case (STATE_DRILL_SURVIVAL ) : return bonus(Garrison.SURVIVAL_TRAINING ) ;
    }
    return 5 ;
  }
  
  
  public int drillType() {
    return state ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected void updateSprite() {
    final GroupSprite s = (GroupSprite) buildSprite().baseSprite() ;
    final Model m =
      (state > 0 && state < STATE_RED_ALERT) ?
      DRILL_MODELS[state - 1] : null ;
    
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
    //
    //  TODO:  You want to be able to toggle on and off different drill types,
    //  and the garrison will be responsible for setting up the equipment at
    //  different times of day so as to alternate between them.
    
    if (categoryID == 0) {
      d.append("\n\nToggle Drill Types:") ;
      for (final int s : ALL_STATES) {
        d.append("\n  ") ;
        d.append(new Description.Link(DRILL_STATE_NAMES[s]) {
          public void whenClicked() { switchDrillState(s) ; }
        }, s == state ? Colour.GREEN : Colour.BLUE) ;
      }
    }
  }
}









