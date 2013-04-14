/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import src.game.common.* ;
import src.game.planet.* ;
import src.util.* ;
import src.graphics.common.* ;
import src.graphics.widgets.UINode;

import org.lwjgl.opengl.* ;


//
//  TODO:  Update minimap when terrain type is changed!
public class Minimap extends UINode {
  
  
  final BaseUI UI ;
  final World world ;
  final Texture mapImage ;
  private Base realm ;
  
  
  public Minimap(BaseUI UI, World world, Base realm) {
    super(UI) ;
    this.UI = UI ;
    this.world = world ;
    this.realm = realm  ;
    //
    final int texSize = world.size ;
    mapImage = Texture.createTexture(texSize, texSize) ;
    byte RGBA[] = new byte[texSize * texSize * 4] ;
    for (int y = 0, m = 0 ; y < texSize ; y++) {
      for (int x = 0 ; x < texSize ; x++) {
        final Habitat h = world.terrain().habitatAt(x, y) ;
        final Colour avg = h.baseTex.averaged() ;
        avg.storeByteValue(RGBA, m) ;
        m += 4 ;
        RGBA[m - 1] = (byte) 0xff ;
      }
    }
    mapImage.putBytes(RGBA) ;
  }
  
  public void updateAt(int x, int y) {
    //  This has to be ARGB, rather than RGBA-
    /*
    final byte[] avg = world.tileAt(x, y).habitat().overlay.colourAverage() ;
    final int val =
      (((int) avg[0] & 0xff) << 8 ) |
      (((int) avg[1] & 0xff) << 16) |
      (((int) avg[2] & 0xff) << 24) |
      (0xff << 0) ;
    final float size = world.size ;
    mapImage.putVal(x / size, y / size, val) ;
    //  Bugger.  Everything has to be complicated...
    //*/
  }
  
  
  protected UINode selectionAt(Vec2D mousePos) {
    if (super.selectionAt(mousePos) == null) return null ;
    return (getMapPosition(mousePos) == null) ? null : this ;
  }
  
  
  protected void whenClicked() {
    final Tile pos = getMapPosition(UI.mousePos()) ;
    if (pos == null) return ;
    UI.camera.lockOn(pos) ;
  }
  
  
  protected Tile getMapPosition(final Vec2D pos) {
    final float
      cX = (pos.x -  xpos()) / bounds.xdim(),
      cY = (pos.y - (ypos() + (bounds.ydim() * 0.5f))) / bounds.ydim() ;
    final Vec2D mapPos = new Vec2D(
      (cX - cY) * world.size,
      (cY + cX) * world.size
    ) ;
    return world.tileAt(mapPos.x, mapPos.y) ;
  }
  
  
  protected void render() {
    GL11.glColor4f(1, 1, 1, 1) ;
    mapImage.bindTex() ;
    renderTex() ;
    if (realm != null) {
      realm.fogMap().bindTex() ;
      renderTex() ;
    }
  }
  
  private void renderTex() {
    //
    //You draw a diamond-shaped area around the four points-
    final float
      w = xdim(),
      h = ydim(),
      x = xpos(),
      y = ypos() ;
    GL11.glBegin(GL11.GL_QUADS) ;
    GL11.glTexCoord2f(0, 0) ;
    GL11.glVertex2f(x, y + (h / 2)) ;
    GL11.glTexCoord2f(0, 1) ;
    GL11.glVertex2f(x + (w / 2), y + h) ;
    GL11.glTexCoord2f(1, 1) ;
    GL11.glVertex2f(x + w, y + (h / 2)) ;
    GL11.glTexCoord2f(1, 0) ;
    GL11.glVertex2f(x + (w / 2), y) ;
    GL11.glEnd() ;
  }
}
