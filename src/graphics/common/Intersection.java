/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.graphics.common ;
import src.util.* ;
import src.graphics.common.* ;


public class Intersection {

  
  
  /**  I'm using a whole bunch of static fields and methods here for the sake
    *  of efficiency.
    */
  private static float
    mx,
    my,
    depth ;
  private static Vec3D
    worldPos = new Vec3D() ;
  private static boolean
    found ;
  private static float
    foundD,  //depth
    foundU,  //texture u coord.
    foundV ;  //texture v coord.
  private static float
    linex,
    liney,
    cornx,
    corny,
    dotc,
    dotm,
    lineL,
    outV,
    outU,
    midU,
    midV ;
  private static Vert3D
    c1 = new Vert3D(),
    c2 = new Vert3D(),
    c3 = new Vert3D() ;
  private static Vec2D
    line = new Vec2D(),
    out = new Vec2D(),
    midP = new Vec2D() ;
  
  
  /**  Determines the point of intersection (if any) between the current mouse position and the
    *  given geometry and texture data.
    */
  public static Vec3D getPick(
      Viewport v, Vec2D mousePos,
      float[] verts,
      float texUV[],
      Texture alpha,
      boolean alphaTest
  ) {
    //
    //  final Vec2D mousePos = GameLoop.currentUI().mousePos() ;
    mx = mousePos.x ;
    my = mousePos.y ;
    found = false ;
    depth = Float.NEGATIVE_INFINITY ;
    //
    //  Essentially, we iterate through the list of polygons, check each for a
    //  point of intersection, and return the closest found.
    for (int i = 0, t = 0 ; i < verts.length ;) {
      c1.x = verts[i++] ;
      c1.y = verts[i++] ;
      c1.z = verts[i++] ;
      c2.x = verts[i++] ;
      c2.y = verts[i++] ;
      c2.z = verts[i++] ;
      c3.x = verts[i++] ;
      c3.y = verts[i++] ;
      c3.z = verts[i++] ;
      if (alphaTest) {
        c1.u = texUV[t++] ;
        c1.v = texUV[t++] ;
        c2.u = texUV[t++] ;
        c2.v = texUV[t++] ;
        c3.u = texUV[t++] ;
        c3.v = texUV[t++] ;
      }
      //if (HUD.isMouseState(HUD.CLICKED))
        //I.say("\n  Checking corners of poly: " + c1 + c2 + c3) ;
      v.isoToScreen(c1) ;
      v.isoToScreen(c2) ;
      v.isoToScreen(c3) ;
      if (
        onSide(c1, c2, c3) &&
        onSide(c2, c3, c1) &&
        onSide(c3, c1, c2) &&
        (
          foundInfo(c1, c2, c3) ||
          foundInfo(c2, c3, c1) ||
          foundInfo(c3, c1, c2)
        )
        //&& (alphaTest ? (alpha.getA(foundU, foundV) > 0.5f) : true)
        && (foundD > depth)
      ) {
        depth = foundD ;
        found = true ;
      }
    }
    if (found) return v.screenToIso(worldPos.set(mx, my, depth)) ;
    return null ;
  }
  
  
  /**  Basically, this method constructs a line from two vertices of the
    *  current triangle, then checks whether the third vertex is on the 'same
    *  side' as the current mouse position.
    *  Performing this check from all 3 sides determines whether the mouse
    *  position is within the given triangle (i.e, on the 'right side' of all 3
    *  edges.)
    */
  private final static boolean onSide(
      final Vert3D l1,
      final Vert3D l2,
      final Vert3D c
  ) {
    linex = l2.x - l1.x ;
    liney = l2.y - l1.y ;
    cornx = c.x - l1.x ;
    corny = c.y - l1.y ;
    //
    //  Get the dot product of both the corner vertex and the current mouse
    //  position with respect to a line perpendicular to that connecting the
    //  first two vertices.
    dotc = (linex * corny) - (liney * cornx) ;
    dotm = (linex * (my - l1.y)) - (liney * (mx - l1.x)) ;
    if (dotc == 0) return (dotm == 0) ;
    return (dotc * dotm >= 0) ;
    //
    //  i.e.- both corner and mouse point are on the same side of, or on, the
    //  line.
  }
  
  
  /**  This method acquires depth and UV coordinates for a given point within
    *  an intersecting triangle.  The method returns false if no accurate
    *  information could be found.
    */
  private static boolean foundInfo(
      final Vert3D orig,
      final Vert3D axis,
      final Vert3D corn
  ) {
    //
    //  First, we establish (and normalise) the 'V' axis.
    line.set(axis.x - orig.x, axis.y - orig.y) ;
    if ((lineL = line.length()) <= 0) return false ;
    line.x /= lineL ;
    line.y /= lineL ;
    //
    //  The 'U' axis is automatically perpendicular.  What are the UV
    //  coordinates of the  corner vertex on that basis?  We'll be comparing
    //  the mouse coordinates to this.
    out.set(corn.x - orig.x, corn.y - orig.y) ;
    outU = line.dist(out) ;
    outV = line.dot(out) ;
    if (outU <= 0) return false ;
    //  So, acquire those mouse coordinates:
    midP.set(mx - orig.x, my - orig.y) ;
    midU = line.dist(midP) ;
    midV = line.dot(midP) ;
    //
    //  Now, we'll need to scale and adjust those relative to the coordinates
    //  of the corner vertex.
    //  The closer the mouse point is to the corner vertex, the more the 'V
    //  axis' will 'shrink' (as it becomes length 0 at the corner itself.)
    //  '(1 - midU) * lineL' is the length of the 'V axis' at this point, while
    //  '(outV * midU)' represents the base V offset of the 'axis' itself at
    //  that distance.
    midU /= outU ;
    if (midU > 1 || midU < 0) return false ;
    midV = (midV - (outV * midU)) / ((1 - midU) * lineL) ;
    //  ...Then we just interpolate the depth and true UV values.
    foundD = (midV * axis.z) + ((1 - midV) * orig.z) ;
    foundD = (midU * corn.z) + ((1 - midU) * foundD) ;
    foundU = (midV * axis.u) + ((1 - midV) * orig.u) ;
    foundU = (midU * corn.u) + ((1 - midU) * foundU) ;
    foundV = (midV * axis.v) + ((1 - midV) * orig.v) ;
    foundV = (midU * corn.v) + ((1 - midU) * foundV) ;
    return true ;
  }
  
  
  private static class Vert3D extends Vec3D { protected float u, v ; }
  
}
