

package src.game.base ;
import src.game.common.* ;
import src.game.building.* ;
import src.util.* ;


//
//  How do you put crops inside, though?

//  Wait.  Have this extend the Venue class.  Own everything, but only occupy
//  a couple of tiles?


public class Plantation extends Fixture implements TileConstants {
  
  
  final static int
    TYPE_NURSERY = 0,
    TYPE_BED     = 1 ;
  
  BotanicalStation belongs ;
  int type ;
  private Crop planted[] = new Crop[4] ;
  
  
  
  public Plantation(BotanicalStation belongs, int type) {
    super(2, 0) ;
    this.belongs = belongs ;
    this.type = type ;
    if (type == TYPE_NURSERY) {
      attachModel(BotanicalStation.NURSERY_MODEL) ;
    }
  }
  

  public Plantation(Session s) throws Exception {
    super(s) ;
    belongs = (BotanicalStation) s.loadObject() ;
    for (int i = 0 ; i < 4 ; i++) {
      planted[i] = (Crop) s.loadObject() ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(belongs) ;
    for (Crop c : planted) s.saveObject(c) ;
  }
  
  
  public int owningType() {
    return FIXTURE_OWNS ;
  }
  
  
  public int pathType() {
    return Tile.PATH_HINDERS ;
  }
  
  
  
  /**  Establishing crop areas-
    */
  final static int
    allot_dirs[]  = { N, E, S, W },
    crop_coords[] = { 0, 0, 0, 1, 1, 0, 1, 1 } ;
  
  
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    if (type == TYPE_NURSERY) {
      world.terrain().maskAsPaved(Spacing.perimeter(area(), world), true) ;
      return ;
    }
    for (int c = 0, i = 0 ; c < 4 ; c++) {
      final Tile t = world.tileAt(x + crop_coords[i++], y + crop_coords[i++]) ;
      planted[c] = new Crop(belongs, belongs.pickSpecies(t, belongs)) ;
      planted[c].enterWorldAt(t.x, t.y, world) ;
    }
  }
  
  
  Crop[] planted() {
    return planted ;
  }
  
  
  
  /**  Finding space.
    */
  static Plantation[] placeAllotment(
    final BotanicalStation parent, final int minSize
  ) {
    final World world = parent.world() ;
    
    Plantation bestSite[] = null ;
    float bestRating = 0 ;
    
    for (int m = 10 ; m-- > 0 ;) {
      final Tile t = Spacing.pickRandomTile(parent, 12, world) ;
      if (t == null) continue ;
      final int off = Rand.index(4) ;
      for (int n = 4 ; n-- > 0 ;) {
        final Plantation allots[] = new Plantation[minSize] ;
        final int i = (n + off) % 4 ;
        if (tryPlacementAt(t, parent, allots, allot_dirs[i])) {
          final float rating = rateArea(allots, world) ;
          if (rating > bestRating) { bestSite = allots ; bestRating = rating ; }
        }
      }
    }
    if (bestSite != null) {
      for (Plantation p : bestSite) p.enterWorld() ;
      return bestSite ;
    }
    /*
    final TileSpread spread = new TileSpread(init) {
      protected boolean canAccess(Tile t) {
        return t.owner() == parent || ! t.blocked() ;
      }
      
      protected boolean canPlaceAt(Tile t) {
        return false ;
      }
    } ;
    spread.doSearch() ;
    //*/
    return null ;
  }
  
  
  static float rateArea(Plantation allots[], World world) {
    float fertility = 0, num = 0 ;
    for (Plantation p : allots) {
      for (Tile t : world.tilesIn(p.area(), false)) {
        fertility += t.habitat().moisture() ;
        num++ ;
      }
    }
    return fertility / num ;
  }
  
  
  
  
  private static boolean tryPlacementAt(
    Tile t, BotanicalStation parent, Plantation allots[], int dir
  ) {
    for (int i = 0 ; i < allots.length ; i++) try {
      if (allots[i] == null) {
        allots[i] = new Plantation(parent, i == 0 ? TYPE_NURSERY : TYPE_BED) ;
      }
      final Plantation p = allots[i] ;
      p.setPosition(
        t.x + (N_X[dir] * 2 * i),
        t.y + (N_Y[dir] * 2 * i),
        t.world
      ) ;
      if (! p.canPlace()) return false ;
    } catch (Exception e) { return false ; }
    return true ;
  }
}
















