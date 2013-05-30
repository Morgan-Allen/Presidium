


package src.game.planet ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;



public class Outcrop extends Fixture {
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  //
  //  What type of outcrop are you?  Dune?  Spire?  Lode?  Deposit?
  //  And what minerals do you contain?
  
  final public static int
    TYPE_MESA   = -1,
    TYPE_DUNE    =  0,
    TYPE_DEPOSIT =  2 ;
  
  int type, mineral ;
  
  
  public Outcrop(int size, int high, int type, int mineral) {
    super(size, high * size) ;
    this.type = type ;
    this.mineral = mineral ;
    
    Model model = null ;
    if (size == 1 && type != TYPE_DUNE) {
      model = Habitat.SPIRE_MODELS[Rand.index(3)][2] ;
    }
    else if (type == TYPE_MESA) {
      if (mineral == 0 || Rand.yes()) {
        int highID = Rand.yes() ? 1 : (3 - size) ;// (Rand.index(3) == 0) ? 0 : 1 ;
        model = Habitat.SPIRE_MODELS[Rand.index(3)][highID] ;
      }
      else {
        model = Habitat.ROCK_LODE_MODELS[mineral - 1] ;
      }
    }
    else if (type == TYPE_DUNE) {
      model = Habitat.DUNE_MODELS[Rand.index(3)] ;
    }
    else if (type == TYPE_DEPOSIT) {
      //model = Habitat.ROCK_LODE_MODELS[mineral - 1] ;
      model = Habitat.MINERAL_MODELS[mineral - 1] ;
    }
    else I.complain("NOT A VALID OUTCROP TYPE!") ;
    
    final Sprite s = model.makeSprite() ;
    if (size > 1 || type == TYPE_DUNE) s.scale = size / 2f ;
    attachSprite(s) ;
  }
  
  
  public Outcrop(Session s) throws Exception {
    super(s) ;
    type = s.loadInt() ;
    mineral = s.loadInt() ;
    if (size > 1 || type == TYPE_DUNE) sprite().scale = size / 2f ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(type) ;
    s.saveInt(mineral) ;
  }
  

  public boolean canPlace() {
    //  This only gets called just before the constructor, so I think I can
    //  put it here.  TODO:  Move the location-verification code from the
    //  TerrainGen class to here?  ...Might be neater.
    final World world = origin().world ;
    for (Tile t : world.tilesIn(area(), false)) {
      if (t == null || t.blocked()) return false ;
    }
    return true ;
  }
  
  
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    setInceptTime(-10) ;
  }
  
  
  public int owningType() {
    return type == TYPE_DUNE ?
      Element.ENVIRONMENT_OWNS :
      Element.TERRAIN_OWNS ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Outcrop" ;
  }
  
  
  public Texture portrait() {
    return null ;
  }
  
  
  public String helpInfo() {
    return
      "Rock outcrops are a frequent indication of underlying mineral wealth." ;
  }

  public String[] infoCategories() {
    return null ;
  }
  
  
  public void writeInformation(Description d, int categoryID) {
    d.append(helpInfo()) ;
  }
  
  
  public void whenClicked() {
  }
}








