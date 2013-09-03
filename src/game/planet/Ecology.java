


package src.game.planet ;
import src.game.common.* ;
import src.util.* ;



public class Ecology {
  
  
  final World world ;
  final int SS ;

  final RandomScan growthMap ;
  float fertilities[][] ;  //Of crops/flora.
  float abundances[][][] ;  //Of species.
  float squalorMap[][] ;    //For pollution/ambience.
  
  
  
  public Ecology(final World world) {
    this.world = world ;
    SS = world.size / World.SECTION_RESOLUTION ;
    
    growthMap = new RandomScan(world.size) {
      protected void scanAt(int x, int y) {
        growthAt(world.tileAt(x, y)) ;
      }
    } ;
    fertilities = new float[SS][SS] ;
    abundances = new float[SS][SS][] ;
    squalorMap = new float[SS][SS] ;
  }
  
  
  public void loadState(Session s) throws Exception {
    growthMap.loadState(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    growthMap.saveState(s) ;
  }
  
  
  
  /**  Continuous updates-
    */
  public void updateEcology() {
    
    final int size = world.size ;
    final float time = world.currentTime() ;
    
    float growIndex = (time % World.GROWTH_INTERVAL) ;
    growIndex *= size * size * 1f / World.GROWTH_INTERVAL ;
    growthMap.scanThroughTo((int) growIndex) ;
    
    //
    //  Update squalor values...
  }
  
  
  private void growthAt(Tile t) {
    Flora.tryGrowthAt(t.x, t.y, world, false) ;
    final Element owner = t.owner() ;
    if (owner != null) owner.onGrowth() ;
  }
  
  
  
  
}













