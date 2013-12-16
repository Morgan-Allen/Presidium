


package src.game.planet ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.util.* ;



public class Ambience {
  
  
  
  /**  Data fields, constructors, and save/load functionality-
    */
  final World world ;
  final MipMap mapValues ;
  
  
  public Ambience(World world) {
    this.world = world ;
    this.mapValues = new MipMap(world.size) ;
  }
  
  
  public void loadState(Session s) throws Exception {
    mapValues.loadFrom(s.input()) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    mapValues.saveTo(s.output()) ;
  }
  
  
  
  /**  Queries and value updates-
    */
  public void updateAt(Tile tile) {
    int value = 0 ;
    final Element owner = tile.owner() ;
    
    if (owner instanceof Installation) {
      value = ((Installation) owner).structure().ambienceVal() ;
    }
    if (owner instanceof Flora) {
      value = ((Flora) owner).growStage() * 4 ;
    }
    for (Mobile m : tile.inside()) if (m instanceof Actor) {
      value -= 5 * ((Actor) m).health.stressPenalty() ;
    }
    
    mapValues.set((byte) value, tile.x, tile.y) ;
  }
  
  
  public float valueAt(Tile t) {
    return mapValues.blendValAt(t.x, t.y, 0.5f) ;
  }
  
  
  public float valueAt(Target t) {
    final Vec3D v = t.position(null) ;
    return mapValues.blendValAt(v.x, v.y, 0.5f) ;
  }
}



