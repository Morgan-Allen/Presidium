


package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.util.* ;


//
//  You need something similar for squalor/ambience.


public class DangerMap {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final static float
    SHORT_TERM_MULT = 0.1f,
    LONG_TERM_MULT  = 1f / World.DEFAULT_DAY_LENGTH ;
  
  final World world ;
  final Base base ;
  final int SS, SR ;
  
  float shortTermVals[][], longTermVals[][], overallVal = 0.5f ;
  
  
  
  public DangerMap(World world, Base base) {
    this.world = world ;
    this.base = base ;
    this.SR = world.sections.resolution ;
    this.SS = world.size / SR ;
    this.shortTermVals = new float[SS][SS] ;
    this.longTermVals  = new float[SS][SS] ;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, SS, SS, 1)) {
      shortTermVals[c.x][c.y] = s.loadFloat() ;
      longTermVals [c.x][c.y] = s.loadFloat() ; 
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, SS, SS, 1)) {
      s.saveFloat(shortTermVals[c.x][c.y]) ;
      s.saveFloat(longTermVals [c.x][c.y]) ;
    }
  }
  
  
  
  /**  Methods for regularly updating, adjusting and querying danger values-
    */
  public void updateVals() {
    for (Coord c : Visit.grid(0, 0, SS, SS, 1)) {
      shortTermVals[c.x][c.y] *= (1 - SHORT_TERM_MULT) ;
      longTermVals [c.x][c.y] *= (1 -  LONG_TERM_MULT) ;
    }
    //overallVal *= (1 - LONG_TERM_MULT) ;
  }
  
  
  public void imposeVal(Tile at, float power) {
    shortTermVals[at.x / SR][at.y / SR] += power * SHORT_TERM_MULT / 3 ;
    longTermVals [at.x / SR][at.y / SR] += power *  LONG_TERM_MULT / 3 ;
    //overallVal += power * LONG_TERM_MULT / (SS * SS * 3) ;
  }
  
  
  public float valAt(Tile at) {
    final int aX = at.x / SR, aY = at.y / SR ;
    return (shortTermVals[aX][aY] + longTermVals[aX][aY]) ;
  }
  
  
}








