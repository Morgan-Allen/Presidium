


package src.graphics.sfx ;
import src.graphics.common.* ;
import src.graphics.terrain.* ;
import src.util.* ;



/**  This class is intended to accomodate rendering of strip-mined terrain and
  *  underground or hidden passages.
  */
public class PassageFX extends SFX implements TileConstants {

  final static Model
    PASSAGE_MODEL = new Model("passage_model", PassageFX.class) {
      public Sprite makeSprite() { return new PassageFX() ; }
    } ;
  public Model model() { return PASSAGE_MODEL ; }
  
  
  MeshBuffer mesh ;
  
  
  public void setupWithArea(Box2D area, TileMask mask) {
    MeshBuffer.beginRecord() ;
    for (Coord c : Visit.grid(area)) {
      genTileFor(c.x, c.y, mask) ;
    }
    mesh = new MeshBuffer(MeshBuffer.compileRecord()) ;
  }
  
  
  public void renderTo(Rendering rendering) {
    mesh.renderTo(rendering) ;
  }
  
  
  
  
  /**  Geometry compilation helper methods/constants-
    */
  //
  //  Okay.  Depending on tile adjacency, you need to position the walls and
  //  floor to meet up with surrounding terrain.
  //  You have 8 polygons- 2 for each corner of the tile, meeting in the centre
  //  like a paper flower.
  final static Vec3D
    TILE_TEMPLATE[] = constructTileTemplate(),
    FLAT_NORMAL = new Vec3D(0, 0, 1),
    WALL_NORMALS[] = {
      new Vec3D( 0, -2, 1).normalise(),
      new Vec3D(-2,  0, 1).normalise(),
      new Vec3D( 0,  2, 1).normalise(),
      new Vec3D( 2,  0, 1).normalise()
    },
    FLOOR_UV_OFF = new Vec3D(0, 0, 0),
    WALLS_UV_OFF = new Vec3D(1, 0, 0) ;
  
  
  private static Vec3D vertAtIndex(int n) {
    return new Vec3D((N_X[n] + 1) / 2f, (N_Y[n] + 1) / 2f, 0) ;
  }
  
  
  private static Vec3D[] constructTileTemplate() {
    final Vec3D temp[] = new Vec3D[8 * 3] ;
    int i = 0 ; for (int n : N_INDEX) {
      temp[i++] = vertAtIndex(n) ;
      temp[i++] = vertAtIndex((n + 1) % 8) ;
      temp[i++] = new Vec3D(0.5f, 0.5f, 0) ;
    }
    return temp ;
  }
  
  
  private void genTileFor(int x, int y, TileMask mask) {
    final float
      verts[] = new float[8 * 3],
      norms[] = new float[8 * 3],
      texts[] = new float[8 * 2] ;
    
    final boolean near[][] = new boolean[3][3] ;
    for (int n : N_INDEX) {
      near[N_X[n] + 1][N_Y[n] + 1] = mask.maskAt(x + N_X[n], y + N_Y[n]) ;
    }
    near[1][1] = true ;
    
    int iV = 0, iN = 0, iT = 0 ;
    boolean mX, mY, isFlat ;
    //
    //  Firstly, determine the position of vertices-
    for (int i = 0 ; i < TILE_TEMPLATE.length ; i++) {
      final Vec3D v = TILE_TEMPLATE[i] ;
      verts[iV++] = v.x + x - 0.5f ;
      verts[iV++] = v.y + y - 0.5f ;
      mX = mY = true ;
      if (v.x == 0) mX &= near[0][1] ;
      if (v.x == 1) mX &= near[2][1] ;
      if (v.y == 0) mY &= near[1][0] ;
      if (v.y == 1) mY &= near[1][2] ;
      verts[iV++] = (mX && mY) ? -1 : 0 ;
    }
    //
    //  Secondly, determine the proper normals/tex coords-
    for (int i = 0 ; i < TILE_TEMPLATE.length ; i++) {
      isFlat = true ;
      isFlat &= verts[(i * 3) + 2] == -1 ;
      isFlat &= verts[(i * 3) + 5] == -1 ;
      isFlat &= verts[(i * 3) + 8] == -1 ;
      //
      //  We vary the normals and tex coords depending on type...
      final Vec3D
        v = TILE_TEMPLATE[i],
        norm  = isFlat ? FLAT_NORMAL : WALL_NORMALS[(((i / 3) + 1) % 8) / 2],
        offUV = isFlat ? FLOOR_UV_OFF : WALLS_UV_OFF ;
      norms[iN++] = norm.x ;
      norms[iN++] = norm.y ;
      norms[iN++] = norm.z ;
      texts[iT++] = (v.x + offUV.x) / 2f ;
      texts[iT++] = (v.y + offUV.y) / 2f ;
    }
    MeshBuffer.recordGeom(verts, norms, texts) ;
  }
}










