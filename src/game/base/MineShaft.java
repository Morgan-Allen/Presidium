


package src.game.base ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.user.* ;
import src.util.* ;




//
//  See if there is space for a new shaft to be sunk.  If so, go out there
//  and sink it.  Plant the shaft surface above.  Mine all the adjoining areas
//  and take the result to the smelters on the surface.
//  ...Favour areas that have the highest concentration of desired minerals-
//  carbons, metals, or isotopes.  So, you're using a spread.
//  Should mineral outcrops be visible on the surface?  ...Probably.

//  I want subterranean tunnels, too.  I can finally handle that!

//
//  Once you finish excavating a given area, you add any viable neighbours to
//  the facing.  Extract minerals until then.  (Random chance of full
//  success.  Call it 100 attempts?)  300 seconds per day, 256 tiles,
//  25,600 seconds = 85 days for one miner to exhaust an area.  Give or take.
//
//  We'll assume that a lifetime is 300 days.  1500 minutes = 25 hours of
//  play.  A full, uninterrupted day plus change.  For a virtual lifetime.





public class MineShaft extends Venue implements VenueConstants, TileConstants {
  
  
  /**  Constants, fields, constructors and save/load methods-
    */
  final static String
    IMG_DIR = "media/Buildings/artificer aura/" ;
  final static ImageModel
    SHAFT_MODEL = ImageModel.asIsometricModel(
      MineShaft.class, IMG_DIR+"excavation_shaft.gif", 4, 2
    ),
    OPENING_MODELS[] = ImageModel.loadModels(
      MineShaft.class, 3, 2.5f, IMG_DIR,
      "carbons_smelter.gif",
      "metals_smelter.gif",
      "isotopes_smelter.gif",
      //*
      "sunk_shaft.gif",
      "shaft_1.png",
      "shaft_2.png"
      //*/
    ) ;
  
  final static int
    MAX_DIG_RANGE = 6 ;
  
  
  private boolean
    seekCarbons  = false,
    seekMetals   = true ,
    seekIsotopes = false ;
  private boolean
    refreshPromise = false ;
  
  MineFace firstFace = null ;
  final MineFace mineGrid[][] ;
  final Box2D digArea = new Box2D() ;
  
  final Sorting <MineFace> workFace = new Sorting <MineFace> () {
    public int compare(MineFace a, MineFace b) {
      if (a == b || a.promise == b.promise) return 0 ;
      return a.promise < b.promise ? 1 : -1 ;
    }
  } ;
  
  
  public MineShaft(Base base) {
    super(4, 2, Venue.ENTRANCE_EAST, base) ;
    attachSprite(SHAFT_MODEL.makeSprite()) ;
    final int gridSize = 4 + (MAX_DIG_RANGE * 2) ;
    mineGrid = new MineFace[gridSize][gridSize] ;
  }


  public MineShaft(Session s) throws Exception {
    super(s) ;
    
    seekCarbons  = s.loadBool() ;
    seekMetals   = s.loadBool() ;
    seekIsotopes = s.loadBool() ;
    
    firstFace = (MineFace) s.loadObject() ;
    digArea.loadFrom(s.input()) ;
    s.loadObjects(workFace) ;
    final int gridSize = 4 + (MAX_DIG_RANGE * 2) ;
    mineGrid = new MineFace[gridSize][gridSize] ;
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      mineGrid[c.x][c.y] = (MineFace) s.loadObject() ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    
    s.saveBool(seekCarbons ) ;
    s.saveBool(seekMetals  ) ;
    s.saveBool(seekIsotopes) ;
    
    s.saveObject(firstFace) ;
    digArea.saveTo(s.output()) ;
    s.saveObjects(workFace) ;
    final int gridSize = 4 + (MAX_DIG_RANGE * 2) ;
    for (Coord c : Visit.grid(0, 0, gridSize, gridSize, 1)) {
      s.saveObject(mineGrid[c.x][c.y]) ;
    }
  }
  
  
  
  /**  Excavation functions-
    */
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    digArea.setTo(area()).expandBy(MAX_DIG_RANGE) ;
    firstFace = insertOpening(world.tileAt(this)) ;
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
  
  //  TODO:  Maybe a Table would be simpler?  Speed might not be critical here.
  
  protected MineFace openingAt(Tile t) {
    if (t == null) return null ;
    final int
      offX = t.x - (int) (digArea.xpos() + 0.5f),
      offY = t.y - (int) (digArea.ypos() + 0.5f) ;
    try { return mineGrid[offX][offY] ; }
    catch (ArrayIndexOutOfBoundsException e) { return null ; }
  }
  
  
  private MineFace insertOpening(Tile t) {
    final int
      offX = t.x - (int) (digArea.xpos() + 0.5f),
      offY = t.y - (int) (digArea.ypos() + 0.5f) ;
    final MineFace near = new MineFace(this) ;
    near.setPosition(t.x, t.y, world) ;
    mineGrid[offX][offY] = near ;
    updatePromise(near) ;
    workFace.add(near) ;
    return near ;
  }
  
  
  protected void openFace(MineFace opening) {
    I.say("Opening new face from "+opening.origin()) ;
    int zzz = 4 ;
    workFace.delete(opening) ;
    opening.promise = -1 ;
    //updatePromise(opening) ;
    final Tile o = opening.origin() ;
    for (int n : N_ADJACENT) {
      final Tile tN = world.tileAt(o.x + N_X[n], o.y + N_Y[n]) ;
      if (! digArea.contains(tN.x, tN.y)) continue ;
      if (openingAt(tN) != null) continue ;
      insertOpening(tN) ;
    }
    zzz += 3 ;
  }
  
  
  //  There may not be a need for this.  Once an opening is exhausted, it's
  //  currently removed from the workface.
  protected void updatePromise(MineFace opening) {
    final int MDR = MAX_DIG_RANGE ;
    final Terrain terrain = world.terrain() ;
    float promise = 1 ;
    if (seekCarbons) promise += terrain.mineralsAt(
      opening.origin(), Terrain.TYPE_CARBONS
    ) ;
    if (seekMetals) promise += terrain.mineralsAt(
      opening.origin(), Terrain.TYPE_METALS
    ) ;
    if (seekIsotopes) promise += terrain.mineralsAt(
      opening.origin(), Terrain.TYPE_ISOTOPES
    ) ;
    promise *= MDR / (Spacing.distance(this, opening) + MDR) ;
    opening.promise = promise ;
  }
  
  
  private MineFace findNextFace() {
    for (MineFace opening : workFace) {
      if (world.activities.includes(opening, Mining.class)) continue ;
      return opening ;
    }
    return null ;
  }
  
  
  
  /**  Economic functions-
    */
  protected Vocation[] careers() {
    return new Vocation[] { Vocation.EXCAVATOR } ;
  }
  
  
  protected Item.Type[] goods() {
    return new Item.Type[] { CARBONS, METALS, ISOTOPES } ;
  }
  
  
  public Behaviour jobFor(Citizen actor) {
    final MineFace opening = findNextFace() ;
    if (opening != null) return new Mining(actor, opening) ;
    return null ;
  }
  
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Excavation Shaft" ;
  }


  public Texture portrait() {
    return Texture.loadTexture("media/GUI/Buttons/excavation_button.gif") ;
  }


  public String helpInfo() {
    return
      "Excavation Shafts permit extraction of useful mineral wealth from "+
      "the terrain surrounding your settlement." ;
  }

  
  public String buildCategory() {
    return BuildingsTab.TYPE_ARTIFICER ;
  }
  
  
  public void writeInformation(Description d, int categoryID) {
    //
    //  TODO:  If any of these priorities are changed, you'll have to
    //  re-evaluate the promise of all current facings.
    
    d.append(new Description.Link("\n[Seek Carbons]") {
      public void whenClicked() {
        seekCarbons = ! seekCarbons ;
      }
    }, seekCarbons ? Colour.GREEN : Colour.RED) ;

    d.append(new Description.Link("\n[Seek Metals]") {
      public void whenClicked() {
        seekMetals = ! seekMetals ;
      }
    }, seekMetals ? Colour.GREEN : Colour.RED) ;

    d.append(new Description.Link("\n[Seek Isotopes]") {
      public void whenClicked() {
        seekIsotopes = ! seekIsotopes ;
      }
    }, seekIsotopes ? Colour.GREEN : Colour.RED) ;
    
    d.append("\n\n") ;
    
    super.writeInformation(d, categoryID) ;
  }
}








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


//  I NEED SOMETHING SIMPLER!
//
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