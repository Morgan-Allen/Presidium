

package src.user ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.util.* ;



public class Composite extends Image {
  
  
  final Stack <Layer> layers = new Stack <Layer> () ;
  
  
  public Composite(HUD myHUD) {
    super(myHUD, Texture.BLACK_TEX) ;
    addLayer(this.texture, 0, 0, 1, 1) ;
  }
  

  public Composite(HUD myHUD, Texture tex) {
    super(myHUD, tex) ;
    addLayer(this.texture, 0, 0, 1, 1) ;
  }
  
  
  public Composite(HUD myHUD, String backName) {
    super(myHUD, backName) ;
    addLayer(this.texture, 0, 0, 1, 1) ;
  }
  
  
  class Layer {
    Box2D UV ;
    Texture tex ;
  }
  
  
  public void addLayer(Texture tex, int offX, int offY, int gridW, int gridH) {
    if (tex == null) return ;
    final Layer layer = new Layer() ;
    layer.tex = tex ;
    final float sX = tex.maxU() * 1f / gridW, sY = tex.maxV() * 1f / gridH ;
    layer.UV = new Box2D().set(offX * sX, offY * sY, sX, sY) ;
    layers.add(layer) ;
  }
  
  
  protected void render() {
    for (Layer layer : layers) {
      renderIn(bounds, layer.tex, layer.UV) ;
    }
  }
}





