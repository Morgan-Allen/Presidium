/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


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
//  I'd like the mining process to proceed in 3 or 4 phases:
//  *  Strip mining, on the surface, which removes outcrops and/or deforms the
//     terrain.
//  *  Deep mining, in which peripheral shafts are dug and more ore is
//     extracted.
//  *  Mantle drilling, in which ores are brought up from the molten core of a
//     planet, providing a virtually inexhaustible supply of minerals.

//  As one type of mining is completed, another will take over.  This is how
//  peripheral structures are established, by digging under far enough that
//  they need to be brought forth.

//
//  TODO:  Have the number of smelters determined by number of staff, and the
//  type influenced by your upgrades.

//  ...Or maybe by extent?  ...Nah.

//  Okay.  Strip mining and mantle drills/shaft openings.



public class ExcavationSite extends Venue implements
  Economy, TileConstants
{
  
  
  /**  Constants, fields, constructors and save/load methods-
    */
  final static String
    IMG_DIR = "media/Buildings/artificer/" ;
  final static ImageModel
    SHAFT_MODEL = ImageModel.asSolidModel(
      ExcavationSite.class, IMG_DIR+"excavation_shaft.gif", 4, 1
    ) ;
  
  final static Service ALL_MINED[] = {
    PETROCARBS, METAL_ORE, FUEL_CORES
  } ;
  final static int
    MAX_DIG_RANGE = 6 ;
  
  private static boolean verbose = false ;
  

  final Box2D digArea = new Box2D() ;
  protected MineFace firstFace = null ;
  final MineFace faceGrid[][] ;
  
  final Sorting <MineFace> faceSorting = new Sorting <MineFace> () {
    public int compare(MineFace a, MineFace b) {
      if (a == b || a.promise == b.promise) return 0 ;
      return a.promise < b.promise ? 1 : -1 ;
    }
  } ;
  
  List <Smelter> smelters = new List <Smelter> () ;
  //  Also, a list of areas dug, and secondary shafts sunk.
  
  
  
  
  public ExcavationSite(Base base) {
    super(4, 1, Venue.ENTRANCE_EAST, base) ;
    structure.setupStats(
      200, 15, 350,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_FIXTURE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    attachModel(SHAFT_MODEL) ;
    final int gridSize = 4 + (MAX_DIG_RANGE * 2) ;
    faceGrid = new MineFace[gridSize][gridSize] ;
  }


  public ExcavationSite(Session s) throws Exception {
    super(s) ;
    
    firstFace = (MineFace) s.loadObject() ;
    digArea.loadFrom(s.input()) ;
    s.loadObjects(faceSorting) ;
    s.loadObjects(smelters) ;
    
    final int gridSize = 4 + (MAX_DIG_RANGE * 2) ;
    faceGrid = new MineFace[gridSize][gridSize] ;
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      faceGrid[c.x][c.y] = (MineFace) s.loadObject() ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    
    s.saveObject(firstFace) ;
    digArea.saveTo(s.output()) ;
    s.saveObjects(faceSorting) ;
    s.saveObjects(smelters) ;
    
    final int gridSize = 4 + (MAX_DIG_RANGE * 2) ;
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      s.saveObject(faceGrid[c.x][c.y]) ;
    }
  }
  
  
  
  /**  Presence in the world and boardability-
    */
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    digArea.setTo(area()).expandBy(MAX_DIG_RANGE) ;
    firstFace = insertFace(world.tileAt(this)) ;
  }
  
  
  public Boardable[] canBoard(Boardable batch[]) {
    if (batch == null) batch = new Boardable[2] ;
    batch[0] = mainEntrance() ;
    batch[1] = firstFace ;
    for (int i = batch.length ; i-- > 2 ;) batch[i] = null ;
    return batch ;
  }
  
  public boolean isEntrance(Boardable b) {
    return b == firstFace || b == mainEntrance() ;
  }
  
  
  public void exitWorld() {
    super.exitWorld() ;
    //
    //  TODO:  Close all your shafts?  Eject occupants?
  }
  
  
  public void onDestruction() {
    super.onDestruction() ;
  }
  
  
  public void onCompletion() {
    super.onCompletion() ;
  }
  
  
  
  /**  Methods for sorting and returning mine-faces in order of promise.
    */
  protected MineFace faceAt(Tile t) {
    if (t == null) return null ;
    final int
      offX = t.x - (int) (digArea.xpos() + 0.5f),
      offY = t.y - (int) (digArea.ypos() + 0.5f) ;
    try { return faceGrid[offX][offY] ; }
    catch (ArrayIndexOutOfBoundsException e) { return null ; }
  }
  
  
  private void refreshSorting() {
    final Batch <MineFace> faces = new Batch <MineFace> () ;
    for (MineFace f : faceSorting) {
      faces.add(f) ;
      updatePromise(f) ;
    }
    faceSorting.clear() ;
    for (MineFace f : faces) faceSorting.add(f) ;
  }
  
  
  private MineFace insertFace(Tile t) {
    final int
      offX = t.x - (int) (digArea.xpos() + 0.5f),
      offY = t.y - (int) (digArea.ypos() + 0.5f) ;
    final MineFace face = new MineFace(this, MineFace.TYPE_UNDER_SHAFT) ;
    face.setPosition(t.x, t.y, world) ;
    faceGrid[offX][offY] = face ;
    updatePromise(face) ;
    faceSorting.add(face) ;
    if (verbose) I.sayAbout(this, "Inserted face at: "+face.origin()) ;
    return face ;
    
    //
    //  TODO:  If you're outside the original shaft, and not right next to an
    //  existing opening, try adding a new opening.  Otherwise, the shaft
    //  cannot be opened.
    
  }
  
  
  protected void openFace(MineFace face) {
    if (verbose) I.sayAbout(this, "Opening new face from "+face.origin()) ;
    refreshSorting() ;
    
    if (verbose && I.talkAbout == this) {
      I.say("Shafts open:") ;
      for (MineFace f : this.faceSorting) {
        I.say("  "+f) ;
      }
      I.say("Now opening "+face) ;
    }
    
    faceSorting.delete(face) ;
    face.promise = -1 ;
    final Tile o = face.origin() ;
    for (int n : N_ADJACENT) {
      final Tile tN = world.tileAt(o.x + N_X[n], o.y + N_Y[n]) ;
      if (tN == null || ! digArea.contains(tN.x, tN.y)) continue ;
      //
      //  You also need to skip this block if it's been taken by another shaft.
      //  ...so how do I know that?  ...Permanent ownership?  Underground tiles?
      if (faceAt(tN) != null) continue ;
      insertFace(tN) ;
    }
    refreshSorting() ;
  }
  
  
  private void updatePromise(MineFace face) {
    final Terrain terrain = world.terrain() ;
    float promise = 1 ;
    
    promise += terrain.mineralsAt(
      face.origin(), Terrain.TYPE_CARBONS
    ) * (structure.upgradeBonus(PETROCARBS)  + 2) ;
    promise += terrain.mineralsAt(
      face.origin(), Terrain.TYPE_METALS
    ) * (structure.upgradeBonus(METAL_ORE)   + 2) ;
    promise += terrain.mineralsAt(
      face.origin(), Terrain.TYPE_ISOTOPES
    ) * (structure.upgradeBonus(FUEL_CORES) + 2) ;
    
    final int MDR = MAX_DIG_RANGE ;
    promise *= MDR / (Spacing.distance(this, face) + MDR) ;
    face.promise = promise ;
    if (verbose) I.sayAbout(this, "  UPDATING PROMISE: "+face) ;
  }
  
  
  
  /**  Economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    ExcavationSite.class, "excavation_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    CARBON_TITRATION = new Upgrade(
      "Carbon Titration",
      "Allows reserves of complex hydrocarbons to be tapped more fequently, "+
      "and installs smelters to further augment production.",
      100,
      PETROCARBS, 1, null, ALL_UPGRADES
    ),
    
    METALS_SMELTING = new Upgrade(
      "Metals Smelting",
      "Allows veins of heavy metals to be detected more reliably, and "+
      "installs surface smelters to augment production.",
      150,
      METAL_ORE, 1, null, ALL_UPGRADES
    ),
    
    ISOTOPE_CAPTURE = new Upgrade(
      "Isotope Capture",
      "Allows deposits of radiactive isotopes to be sought out more reliably, "+
      "and installs external smelters to augment production.",
      200,
      FUEL_CORES, 1, null, ALL_UPGRADES
    ),
    
    EXCAVATOR_STATION = new Upgrade(
      "Excavator Station",
      "Excavators are responsible for seeking out subterranean mineral "+
      "deposits and bringing them to the surface.",
      50,
      Background.EXCAVATOR, 1, null, ALL_UPGRADES
    ),
    
    //
    //  TODO:  Have this extend the maximum range of excavations?
    STRIP_MINING = new Upgrade(
      "Strip Mining",
      "Allows surface and topsoil mineral deposits to be extracted along "+
      "with forest carbons, but inflicts lasting damage on the landscape.",
      150,
      null, 1, EXCAVATOR_STATION, ALL_UPGRADES
    ),
    
    MANTLE_DRILLING = new Upgrade(
      "Mantle Drilling",
      "Enables deep sub-surface boring to bring up an indefinite supply of "+
      "metals and isotopes from the planet's molten core, at the cost of "+
      "heavy pollution.",
      350,
      null, 1, METALS_SMELTING, ALL_UPGRADES
    )
  ;
  
  
  
  public Background[] careers() {
    return new Background[] { Background.EXCAVATOR } ;
  }
  
  
  public Service[] services() {
    return new Service[] { PETROCARBS, METAL_ORE, FUEL_CORES, RARITIES } ;
  }
  
  
  public int numOpenings(Background v) {
    final int NO = super.numOpenings(v) ;
    if (v == Background.EXCAVATOR) return NO + 2 ;
    return 0 ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    //
    //  Outside deliveries get top priority-
    final Delivery d = Deliveries.nextDeliveryFor(
      actor, this, services(), 10, world
    ) ;
    ///I.say("Next delivery is: "+d) ;
    if (d != null && personnel.assignedTo(d) < 1) return d ;
    final Choice choice = new Choice(actor) ;
    //
    //  Consider smelting ores-
    for (Smelter smelter : smelters) {
      choice.add(new Smelting(actor, smelter, this, smelter.type)) ;
    }
    for (Service mined : ALL_MINED) {
      choice.add(new Smelting(actor, this, this, mined)) ;
    }
    //
    //  Also consider straightforward mining-
    final Mining m = nextMiningFor(actor) ;
    if (m != null) choice.add(m) ;
    //
    //  TODO:  Consider opening new shafts as you expand.
    return choice.weightedPick(actor.mind.whimsy()) ;
  }
  
  
  //
  //  TODO:  Restore this once secondary shafts are opened-
  /*
  protected Smelter nearestSmelter(Actor actor, Service mines) {
    Smelter picked = null ;
    float minDist = Float.POSITIVE_INFINITY ;
    for (Smelter s : smelters) if (s.mined == mines && s.structure.intact()) {
      final float dist = Spacing.distance(actor, s) ;
      if (dist < minDist) { picked = s ; minDist = dist ; }
    }
    return picked ;
  }
  //*/
  
  
  protected Mining nextMiningFor(Actor actor) {
    refreshSorting() ;
    for (MineFace face : faceSorting) {
      if (face.promise == -1 || face.workDone >= 100) continue ;
      final Mining m = new Mining(actor, face) ;
      if (personnel.assignedTo(m) > 0) continue ;
      return m ;
    }
    return null ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    
    if (verbose && I.talkAbout == this) {
      I.say("Shafts open:") ;
      for (MineFace face : this.faceSorting) {
        I.say("  "+face) ;
      }
    }
    
    adjustSmelters(PETROCARBS, CARBON_TITRATION) ;
    adjustSmelters(METAL_ORE , METALS_SMELTING ) ;
    adjustSmelters(FUEL_CORES, ISOTOPE_CAPTURE ) ;
  }
  
  
  private void adjustSmelters(Service mined, Upgrade related) {
    final int properCount = (1 + structure.upgradeLevel(related)) / 2 ;
    int count = 0 ;
    for (Smelter s : smelters) {
      if (s.destroyed()) smelters.remove(s) ;
      else if (s.type == mined) count++ ;
    }
    
    if (verbose) I.sayAbout(this,
      "Proper number of "+mined+" smelters: "+properCount+", actual "+count
    ) ;
    
    if (properCount > count) {
      final Smelter s = Smelter.siteNewSmelter(this, mined) ;
      if (s != null) smelters.add(s) ;
    }
    if (properCount < count) {
      final Smelter s = Smelter.cullWorst(smelters, this) ;
      if (s != null) smelters.remove(s) ;
    }
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Excavation Site" ;
  }


  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/excavation_button.gif") ;
  }


  public String helpInfo() {
    return
      "Excavation Sites expedite the extraction of mineral wealth and "+
      "buried artifacts from the terrain surrounding your settlement." ;
  }

  
  public String buildCategory() {
    return InstallTab.TYPE_ARTIFICER ;
  }
}





