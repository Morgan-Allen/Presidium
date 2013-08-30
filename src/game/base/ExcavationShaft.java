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



public class ExcavationShaft extends Venue implements
  BuildConstants, TileConstants
{
  
  
  /**  Constants, fields, constructors and save/load methods-
    */
  final static String
    IMG_DIR = "media/Buildings/artificer/" ;
  final static ImageModel
    SHAFT_MODEL = ImageModel.asIsometricModel(
      ExcavationShaft.class, IMG_DIR+"excavation_shaft.gif", 4, 1
    ) ;
  
  final static int
    MAX_DIG_RANGE = 6 ;
  
  protected MineFace firstFace = null ;
  final MineFace faceGrid[][] ;
  final Box2D digArea = new Box2D() ;
  
  final Sorting <MineFace> faceSorting = new Sorting <MineFace> () {
    public int compare(MineFace a, MineFace b) {
      if (a == b || a.promise == b.promise) return 0 ;
      return a.promise < b.promise ? 1 : -1 ;
    }
  } ;
  private boolean needsSorting = true ;
  
  
  
  public ExcavationShaft(Base base) {
    super(4, 1, Venue.ENTRANCE_EAST, base) ;
    structure.setupStats(
      200, 15, 350,
      VenueStructure.SMALL_MAX_UPGRADES, false
    ) ;
    attachSprite(SHAFT_MODEL.makeSprite()) ;
    final int gridSize = 4 + (MAX_DIG_RANGE * 2) ;
    faceGrid = new MineFace[gridSize][gridSize] ;
  }


  public ExcavationShaft(Session s) throws Exception {
    super(s) ;
    firstFace = (MineFace) s.loadObject() ;
    digArea.loadFrom(s.input()) ;
    s.loadObjects(faceSorting) ;
    needsSorting = s.loadBool() ;
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
    s.saveBool(needsSorting) ;
    final int gridSize = 4 + (MAX_DIG_RANGE * 2) ;
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      s.saveObject(faceGrid[c.x][c.y]) ;
    }
  }
  
  
  
  /**  Excavation functions-
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
  
  
  protected MineFace faceAt(Tile t) {
    if (t == null) return null ;
    final int
      offX = t.x - (int) (digArea.xpos() + 0.5f),
      offY = t.y - (int) (digArea.ypos() + 0.5f) ;
    try { return faceGrid[offX][offY] ; }
    catch (ArrayIndexOutOfBoundsException e) { return null ; }
  }
  
  
  private void refreshSorting() {
    if (! needsSorting) return ;
    final Batch <MineFace> faces = new Batch <MineFace> () ;
    for (MineFace f : faceSorting) faces.add(f) ;
    faceSorting.clear() ;
    for (MineFace f : faces) faceSorting.add(f) ;
    needsSorting = false ;
  }
  
  
  private MineFace insertFace(Tile t) {
    final int
      offX = t.x - (int) (digArea.xpos() + 0.5f),
      offY = t.y - (int) (digArea.ypos() + 0.5f) ;
    final MineFace face = new MineFace(this) ;
    face.setPosition(t.x, t.y, world) ;
    faceGrid[offX][offY] = face ;
    updatePromise(face) ;
    refreshSorting() ;
    faceSorting.add(face) ;
    return face ;
  }
  
  
  protected void openFace(MineFace face) {
    I.say("Opening new face from "+face.origin()) ;
    needsSorting = true ;
    refreshSorting() ;
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
  }
  
  
  protected void updatePromise(MineFace face) {
    final Terrain terrain = world.terrain() ;
    float promise = 1 ;
    
    promise += terrain.mineralsAt(
      face.origin(), Terrain.TYPE_CARBONS
    ) * (structure.upgradeBonus(CARBONS) + 1) ;
    promise += terrain.mineralsAt(
      face.origin(), Terrain.TYPE_METALS
    ) * (structure.upgradeBonus(METALS) + 1) ;
    promise += terrain.mineralsAt(
      face.origin(), Terrain.TYPE_ISOTOPES
    ) * (structure.upgradeBonus(ISOTOPES) + 1) ;
    
    final int MDR = MAX_DIG_RANGE ;
    promise *= MDR / (Spacing.distance(this, face) + MDR) ;
    face.promise = promise ;
    I.say("  UPDATING PROMISE: "+face) ;
  }
  
  
  private MineFace findNextFace() {
    refreshSorting() ;
    for (MineFace face : faceSorting) {
      if (world.activities.includes(face, Mining.class)) continue ;
      if (face.promise == -1 || face.workDone >= 100) continue ;
      return face ;
    }
    return null ;
  }
  
  
  
  /**  Economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    ExcavationShaft.class, "excavation_upgrades"
  ) ;
  protected Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    CARBON_TITRATION = new Upgrade(
      "Carbon Titration",
      "Allows deposits of complex hydrocarbons to be processed and stored "+
      "more efficiently.",
      CARBONS, 2, null, ALL_UPGRADES
    ),

    METALS_SMELTING = new Upgrade(
      "Metals Smelting",
      "Allows heavy metal deposits to be processed and extracted more "+
      "efficiently.",
      METALS, 2, null, ALL_UPGRADES
    ),

    ISOTOPE_CAPTURE = new Upgrade(
      "Isotope Capture",
      "Allows deposits of radiactive isotopes to be processed and stored "+
      "more efficiently.",
      ISOTOPES, 2, null, ALL_UPGRADES
    ),
    
    EXCAVATOR_QUARTERS = new Upgrade(
      "Excavator Quarters",
      "Excavators are responsible for seeking out subterranean mineral "+
      "deposits and bringing them to the surface.",
      Vocation.EXCAVATOR, 1, null, ALL_UPGRADES
    ) ;
  
  
  
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.EXCAVATOR } ;
  }
  
  
  protected Service[] services() {
    return new Service[] { CARBONS, METALS, ISOTOPES } ;
  }
  
  
  public int numOpenings(Vocation v) {
    final int NO = super.numOpenings(v) ;
    if (v == Vocation.EXCAVATOR) return NO + 2 ;
    return 0 ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    
    final Delivery d = stocks.nextDelivery(actor, services()) ;
    if (d != null) return d ;
    
    final MineFace opening = findNextFace() ;
    if (opening != null) return new Mining(actor, opening) ;
    
    //
    //  TODO:  Consider opening new shafts as you expand.
    return null ;
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Excavation Shaft" ;
  }


  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/excavation_button.gif") ;
  }


  public String helpInfo() {
    return
      "Excavation Shafts permit extraction of useful mineral wealth from "+
      "the terrain surrounding your settlement." ;
  }

  
  public String buildCategory() {
    return InstallTab.TYPE_ARTIFICER ;
  }
}







/*
  OPENING_MODELS[] = ImageModel.loadModels(
    ExcavationShaft.class, 3, 2.5f, IMG_DIR,
    "carbons_smelter.gif",
    "metals_smelter.gif",
    "isotopes_smelter.gif",
    "sunk_shaft.gif",
    "shaft_1.png",
    "shaft_2.png"
  ) ;
  //*/

/*
private boolean
  seekCarbons  = false,
  seekMetals   = true ,
  seekIsotopes = false ;
//*/
/*
public void writeInformation(Description d, int categoryID, HUD UI) {
  
  d.append(new Description.Link("\n[Seek Carbons]") {
    public void whenClicked() {
      seekCarbons = ! seekCarbons ;
      refreshPromise = true ;
    }
  }, seekCarbons ? Colour.GREEN : Colour.RED) ;
  
  d.append(new Description.Link("\n[Seek Metals]") {
    public void whenClicked() {
      seekMetals = ! seekMetals ;
      refreshPromise = true ;
    }
  }, seekMetals ? Colour.GREEN : Colour.RED) ;
  
  d.append(new Description.Link("\n[Seek Isotopes]") {
    public void whenClicked() {
      seekIsotopes = ! seekIsotopes ;
      refreshPromise = true ;
    }
  }, seekIsotopes ? Colour.GREEN : Colour.RED) ;
  
  d.append("\n\n") ;
  
  super.writeInformation(d, categoryID, UI) ;
}
//*/


/*
public Behaviour jobFor(Citizen actor) {
  //  This will only work with a single digger, since the placement doesn't
  //  check for overlap with unplaced shafts...
  MineOpening toDig = null ;
  for (MineOpening opening : openings) {
    if (! opening.inWorld()) { toDig = opening ; break ; }
  }
  if (toDig == null) {
    final MineOpening opening = placeNextShaft() ;
    if (opening == null) return null ;
    toDig = opening ;
  }
  
  final Action digAction = new Action(
    actor, toDig,
    this, "actionDigShaft",
    Model.AnimNames.BUILD, "digging new shaft"
  ) ;
  return digAction ;
}


//  PROBLEM- this is too predictable.  You need to vary placement more.
MineOpening placeNextShaft() {
  //
  //  You have to find space for a new shaft.
  final Box2D limit = new Box2D().setTo(area()).expandBy(MAX_DIG_RANGE) ;
  final Tile o = world.tileAt(this) ;
  final Tile initTile = Spacing.nearestOpenTile(world.tileAt(
    o.x + (((Rand.num() * 2) - 1) * MAX_DIG_RANGE),
    o.y + (((Rand.num() * 2) - 1) * MAX_DIG_RANGE)
  ), this) ;
  final MineOpening
    partA = new MineOpening(this, OPENING_MODELS[Rand.index(3)]),
    partB = new MineOpening(this, OPENING_MODELS[3 + Rand.index(3)]) ;
  partA.setPosition(0, 0, world) ;
  partB.setPosition(0, 3, world) ;
  Fixture match[] = TileSpread.tryPlacement(
    initTile, limit, this, partA, partB
  ) ;
  if (match == null) {
    partA.setPosition(0, 0, world) ;
    partB.setPosition(3, 0, world) ;
    match = TileSpread.tryPlacement(
      initTile, limit, this, partA, partB
    ) ;
  }
  if (match == null) return null ;
  for (Fixture f : match) openings.add((MineOpening) f) ;
  return (MineOpening) match[0] ;
}


public boolean actionDigShaft(Actor actor, MineOpening opening) {
  opening.clearSurrounds() ;
  opening.enterWorld() ;
  return true ;
}
//*/