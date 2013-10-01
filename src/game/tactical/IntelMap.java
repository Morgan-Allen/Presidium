


package src.game.tactical ;
import src.game.common.* ;
import src.game.planet.* ;
import src.graphics.common.* ;
import src.user.BaseUI;
import src.util.* ;



public class IntelMap {
  
  
  
  /**  Field definitions, constructors and save/load methods-
    */
  final Base base ;
  private World world = null ;
  
  //  Note:  Any previously-explored tile will have a minimum fog value of
  //  0.5, or 0.3, or something.  That way, you still get the 'fadeout' effect
  //  at the edge of an actor's vision.
  float fogVals[][] ;
  MipMap fogMap ;
  
  private Texture fogTex, oldTex ;
  private byte fogBytes[], newBytes[] ;
  private float lastFogTime = -1 ;
  
  
  
  public IntelMap(Base base) {
    this.base = base ;
  }
  
  
  public void initFog(World world) {
    this.world = world ;
    final int size = world.size ;
    fogVals = new float[size][size] ;
    fogMap = new MipMap(size) ;
    
    fogTex = Texture.createTexture(size, size) ;
    oldTex = Texture.createTexture(size, size) ;
    fogBytes = new byte[size * size * 4] ;
    newBytes = new byte[size * size * 4] ;
    
    for (int i = fogBytes.length ; i-- > 0 ;) {
      fogBytes[i] = newBytes[i] = (i % 4 == 3) ? (byte) 0xff : 0 ;
    }
    fogTex.putBytes(newBytes) ;
    oldTex.putBytes(fogBytes) ;
  }
  
  
  public void loadState(Session s) throws Exception {
    initFog(world = s.world()) ;
    for (Coord c : Visit.grid(0,  0, world.size, world.size, 1)) {
      fogVals[c.x][c.y] = s.loadFloat() ;
    }
    s.loadByteArray(fogBytes) ;
    s.loadByteArray(newBytes) ;
    oldTex.putBytes(fogBytes) ;
    fogTex.putBytes(newBytes) ;
    fogMap.loadFrom(s.input()) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    final int size = world.size ;
    for (Coord c : Visit.grid(0,  0, size, size, 1)) {
      s.saveFloat(fogVals[c.x][c.y]) ;
    }
    s.saveByteArray(fogBytes) ;
    s.saveByteArray(newBytes) ;
    fogMap.saveTo(s.output()) ;
  }
  
  
  public MipMap fogMap() {
    return fogMap ;
  }
  
  
  public World world() {
    return world ;
  }
  
  
  
  /**  Visual refreshment-
    */
  public void updateFogBuffers(float fogTime) {
    final boolean needsUpdate = ((int) fogTime) != ((int) lastFogTime) ;
    lastFogTime = fogTime ;
    if (! needsUpdate) return ;
    //
    //  You need to make the current fogTex into oldTex.
    Texture heldTex = oldTex ;
    oldTex = fogTex ;
    fogTex = heldTex ;
    //
    //  Then, you need to stuff newBytes into fogTex.
    fogTex.putBytes(fogBytes) ;
    //
    //  Then clear newBytes.
    byte heldBytes[] = newBytes ;
    fogBytes = newBytes ;
    newBytes = new byte[world.size * world.size * 4] ;
    for (int i = newBytes.length ; i-- > 0 ;) {
      newBytes[i] = heldBytes[i] ;
    }
  }
  
  
  public Texture fogTex() {
    return fogTex ;
  }
  
  
  public Texture oldFogTex() {
    return oldTex ;
  }
  
  
  private void stuffDisplayVal(final float val, final int x, final int y) {
    int off = ((y * world.size) + x) * 4 ;
    final byte
      tone  =  0,//(byte) (val * 255),
      alpha = (byte) ((1 - val) * 255) ;
    newBytes[off++] = tone ;
    newBytes[off++] = tone ;
    newBytes[off++] = tone ;
    newBytes[off] = alpha ;
  }
  
  
  public float displayFog(Tile t) {
    if (GameSettings.noFog) return 1 ;
    float time = world.currentTime() ;
    time += PlayLoop.frameTime() / PlayLoop.UPDATES_PER_SECOND ;
    time /= 2 ;
    time %= 1 ;
    //
    //  Blend the old and new fog values here.
    final int off = ((t.y * world.size) + t.x) * 4 ;
    final float
      oldVal = (fogBytes[off + 3] & 0xff) / 255f,
      newVal = (newBytes[off + 3] & 0xff) / 255f ;
    //
    //  TODO:  This all seems a mite finicky and byte-order dependant.  Try to
    //  ensure this stays viable cross-platform.
    //
    //  TODO:  Better yet, create a dedicated FogBuffer class, and stick it in
    //  there.
    return 1 - ((oldVal * (1 - time)) + (time * newVal)) ;
  }
  
  
  
  /**  Queries and modifications-
    */
  public void updateFog() {
    
  }
  
  
  public float fogAt(Tile t) {
    if (GameSettings.noFog) return 1 ;
    return fogVals[t.x][t.y] ;
  }
  
  
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
    for (Tile t : world.tilesIn(area, true)) {
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
        if (newVal == 1) fogMap.set((byte) 1, t.x, t.y) ;
        stuffDisplayVal(newVal, t.x, t.y) ;
      }
      tilesSeen += lift - oldVal ;
    }
    return (int) tilesSeen ;
  }
}

