/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.util.* ;



public class Crop implements Session.Saveable, Target {
  
  
  final static int
    NOT_PLANTED = -1,
    MIN_GROWTH  =  1,
    MAX_GROWTH  =  3 ;
  
  final Plantation parent ;
  final Tile tile ;
  
  int varID ;
  float growStage, health ;
  //
  //  TODO:  Allow crops to become disease/weed-infested, and require attention
  //  during the growth cycle.
  
  
  Crop(Plantation parent, int varID, Tile t) {
    this.parent = parent ;
    this.varID = varID ;
    this.tile = t ;
    growStage = NOT_PLANTED ;
    health = 1.0f ;
    //attachModel(Plantation.speciesModel(varID, 0)) ;
  }
  
  
  public Crop(Session s) throws Exception {
    s.cacheInstance(this) ;
    parent = (Plantation) s.loadObject() ;
    tile = (Tile) s.loadTarget() ;
    varID = s.loadInt() ;
    growStage = s.loadFloat() ;
    health = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(parent) ;
    s.saveTarget(tile) ;
    s.saveInt(varID) ;
    s.saveFloat(growStage) ;
    s.saveFloat(health) ;
  }
  
  
  
  /**  Implementing the Target interface-
    */
  private Object flagged ;
  public boolean inWorld() { return parent.inWorld() ; }
  public boolean destroyed() { return parent.destroyed() ; }
  public Vec3D position(Vec3D v) { return tile.position(v) ; }
  public float height() { return tile.height() ; }
  public float radius() { return tile.radius() ; }
  public void flagWith(Object f) { this.flagged = f ; }
  public Object flaggedWith() { return flagged ; }
}




/*
public void onGrowth() {
  if (growStage == NOT_PLANTED) return ;
  final float growChance = Visit.clamp(
    origin().habitat().moisture() / 10f, 0.2f, 0.8f
  ) ;
  if (Rand.num() < growChance) growStage++ ;
  if (growStage > MAX_GROWTH) growStage = MAX_GROWTH ;
  //
  //  Update the sprite-
  final Model m = Plantation.speciesModel(varID, (int) growStage) ;
  ((ImageSprite) sprite()).setModel((ImageModel) m) ;
}


public void exitWorld() {
  super.exitWorld() ;
  ///parent.planted.remove(this) ;
}
  
  
  
  public int pathType() {
    return Tile.PATH_HINDERS ;
  }
  
  
  public int owningType() {
    return Element.FIXTURE_OWNS ;
  }
//*/
