


package src.game.tactical ;
import src.game.common.* ;
import src.game.planet.* ;
import src.graphics.common.* ;
import src.util.* ;



public class IntelMap {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final Base base ;
  private World world = null ;
  
  float fogVals[][] ;
  MipMap fogMap ;
  
  //  Note:  Any previously-explored tile will have a minimum fog value of
  //  0.5, or 0.3, or something.  That way, you still get the 'fadeout' effect
  //  at the edge of an actor's vision.
  
  ///float dangerLevel ;
  //  Create old and new fog maps later on.  Have one fade in completely before
  //  you fade the other out, every half-second or so.
  private Texture fogTex ;
  
  
  
  public IntelMap(Base base) {
    this.base = base ;
  }
  
  
  public void initFog(World world) {
    this.world = world ;
    final int size = world.size ;
    fogVals = new float[size][size] ;
    fogMap = new MipMap(size) ;
    
    fogTex = Texture.createTexture(size, size) ;
    byte vals[] = new byte[size * size * 4] ;
    for (int i = vals.length ; i-- > 0 ;) vals[i] = (byte) 0xff ;
    fogTex.putBytes(vals) ;
  }
  
  
  public void loadState(Session s) throws Exception {
    initFog(world = s.world()) ;
    for (Coord c : Visit.grid(0,  0, world.size, world.size, 1)) {
      final float val = fogVals[c.x][c.y] = s.loadFloat() ;
      fogTex.putColour(Colour.transparency(1 - val), c.x, c.y) ;
    }
    fogMap.loadFrom(s.input()) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    final int size = world.size ;
    for (Coord c : Visit.grid(0,  0, size, size, 1)) {
      s.saveFloat(fogVals[c.x][c.y]) ;
    }
    fogMap.saveTo(s.output()) ;
  }
  
  
  public MipMap fogMap() {
    return fogMap ;
  }
  
  
  public Texture fogTex() {
    return fogTex ;
  }
  
  
  public World world() {
    return world ;
  }
  
  
  
  /**  Queries and modifications-
    */
  public float fogAt(Tile t) {
    return fogVals[t.x][t.y] ;
  }
  
  //
  //  You also need to check for nearby actors, and incorporate an estimate of
  //  the danger they pose.
  
  public int liftFogAround(Target target, float radius) {
    //
    //  We record and return the number of new tiles seen-
    final Vec3D p = target.position(null) ;
    final Box2D area = new Box2D().set(
      p.x - radius, p.y - radius,
      radius * 2, radius * 2
    ) ;
    float tilesSeen = 0 ;
    //
    //  Iterate over any tiles within a certain distance of the target point-
    for (Tile t : world.tilesIn(area, true)) if (t != null) {
      final float distance = Spacing.distance(t, target) ;
      if (distance > radius) continue ;
      //
      //  Calculate the minimum fog value, based on target proximity-
      final float oldVal = fogVals[t.x][t.y] ;
      final float lift = Visit.clamp((1 - (distance / radius)) * 1.5f, 0, 1) ;
      final float newVal = Math.max(lift, oldVal) ;
      fogVals[t.x][t.y] = newVal ;
      //
      //  If there's been a change in fog value, update the reference and
      //  rendering data-
      if (oldVal != newVal) {
        if (oldVal == 0) fogMap.set((byte) 1, t.x, t.y) ;
        fogTex.putColour(Colour.transparency(1 - newVal), t.x, t.y) ;
      }
      tilesSeen += lift - oldVal ;
    }
    return (int) tilesSeen ;
  }
}

