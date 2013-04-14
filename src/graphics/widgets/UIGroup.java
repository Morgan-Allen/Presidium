/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.widgets ;
import src.util.* ;


public class UIGroup extends UINode {
  
  
  
  final List <UINode> kids = new List <UINode> () ;
  protected float alpha = 1.0f ;
  //protected Texture texture ;
  protected int margin ;
  
  
  public UIGroup(HUD myHUD) {
    super(myHUD) ;
    if (myHUD == null && ! (this instanceof HUD)) I.complain("No HUD!") ;
  }
  
  //Box2D fullBounds() { return childBounds ; }
  
  public void render() {
    //Test.report("border position " + xpos + " " + ypos) ;
    for (UINode kid : kids) if (! kid.hidden) kid.render() ;
  }
  
  protected UINode selectionAt(Vec2D mousePos) {
    UINode selected = null ;
    for (UINode kid : kids) if (! kid.hidden) {
      final UINode kidSelect = kid.selectionAt(mousePos) ;
      if (kidSelect != null) selected = kidSelect ;
    }
    //  Return children, if possible.
    return selected ;
  }

  
  public void updateAsBase(Box2D bound) {
    updateState() ;
    updateRelativeParent(bound) ;
    updateAbsoluteBounds(bound) ;
  }
  
  protected void updateState() {
    super.updateState() ;
    for (UINode kid : kids) kid.updateState() ;
  }

  void updateRelativeParent(Box2D base) {
    super.updateRelativeParent(base) ;
    for (UINode kid : kids) if (! kid.hidden) kid.updateRelativeParent() ;
  }
  
  void updateAbsoluteBounds(Box2D base) {
    super.updateAbsoluteBounds(base) ;
    for (UINode kid : kids) if (! kid.hidden) kid.updateAbsoluteBounds() ;
  }
}



/*
if (centreX) bounds.xpos(bounds.xpos() +
  (0 - childBounds.xpos()) - (childBounds.xdim() / 2)) ;
if (centreY) bounds.ypos(bounds.ypos() +
  (0 - childBounds.ypos()) - (childBounds.ydim() / 2)) ;
//*/
//final protected Box2D childBounds = new Box2D() ;
/*
public boolean
  centreX = false,
  centreY = false ;
//*/
//if (selected != null) return selected ;// || ! bordered) return selected ;
//return (bounds.contains(mousePos.x, mousePos.y)) ? this : null ;
//boolean bordered = false ;
/*
public void setBordered(int margin, Texture t, float alpha) {
  //bordered = true ;
  texture = t ;
  this.margin = margin ; this.alpha = alpha ;
}
//*/
/*
public void setUnbordered() {
  bordered = false ;
}
//*/

/*
final private static float
  UV_SEQ[] = { 0, 0.25f, 0.75f, 1 } ;  //margin texture coordinates.
private static float
  X_SEQ[] = new float[4],
  Y_SEQ[] = new float[4] ;
//*/


/*
if (bordered) {
  X_SEQ[0] = childBounds.xpos() ;
  X_SEQ[1] = childBounds.xpos() + margin ;
  X_SEQ[2] = childBounds.xmax() - margin ;
  X_SEQ[3] = childBounds.xmax() ;
  Y_SEQ[0] = childBounds.ypos() ;
  Y_SEQ[1] = childBounds.ypos() + margin ;
  Y_SEQ[2] = childBounds.ymax() - margin ;
  Y_SEQ[3] = childBounds.ymax() ;
  
  texture.bindTex() ;
  GL11.glColor4f(1, 1, 1, alpha) ;
  GL11.glBegin(GL11.GL_QUADS) ;
  for (int y = 0, x ; y < 3 ; y++)
    for (x = 0 ; x < 3 ; x++) {
      drawQuad(
        X_SEQ[x],      Y_SEQ[y],
        X_SEQ[x + 1],  Y_SEQ[y + 1],
        UV_SEQ[x],     UV_SEQ[y + 1],
        UV_SEQ[x + 1], UV_SEQ[y]
      ) ;
    }
  GL11.glEnd() ;
}
//*/