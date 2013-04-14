/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.graphics.terrain ;
import src.graphics.common.* ;
import src.util.* ;


public class TestTerrain extends ViewLoop {
  

  public static void main(String a[]) {
    final TestTerrain test = new TestTerrain() ;
    test.run() ;
  }
  
  private TerrainMesh meshes[] ;
  
  public void setup() {
    final Texture textures[] = Texture.loadTextures(new String[] {
      "media/Terrain/dense_grass.gif",
      "media/Terrain/stone3.gif"
    }) ;
    final byte heightMap[][] = new byte[][] {
      { 4, 4, 3, 3, 1 },
      { 4, 3, 3, 2, 1 },
      { 3, 2, 2, 2, 2 },
      { 2, 3, 3, 2, 2 },
      { 3, 4, 3, 2, 2 }
    } ;
    final byte typeIndex[][] = new byte[][] {
      { 0, 0, 1, 1 },
      { 0, 1, 0, 1 },
      { 1, 0, 1, 1 },
      { 0, 1, 1, 1 },
    } ;
    final byte varsIndex[][] = new byte[][] {
      { 2, 2, 1, 0 },
      { 2, 1, 0, 1 },
      { 1, 0, 1, 2 },
      { 0, 1, 2, 0 },
    } ;
    meshes = TerrainMesh.genMeshes(
      0, 0, 4, 4, textures,
      heightMap, typeIndex, varsIndex
    ) ;
  }

  protected void update() {
    for (TerrainMesh mesh : meshes) {
      rendering.addClient(mesh) ;
    }
    rendering.lighting.direct(
      rendering.port.viewInvert(new Vec3D(0, 0, 1))
    ) ;
  }
}





