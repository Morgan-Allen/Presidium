/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.graphics.jointed ;
import java.nio.FloatBuffer ;
import org.lwjgl.BufferUtils ;
import src.util.* ;
import src.graphics.common.* ;
import java.io.* ;



public abstract class JointModel extends Model {
  
  
  Vec3D
    vertRel[],  //3 floats per vertex, relative to parent joints.
    vertAbs[],  //absolute positions of said vertices.
    norms[]  ;  //3 normals per poly, always relative to parent joints.
  float
    UV[] ;  //2 UV coords (6 floats) per poly.
  byte
    joinID[] ;  //1 per vertex (indices of joint ownership.)
  int
    vertID[] ;  //3 per poly (indices used to allow vertex sharing.)
  int
    animLength ;  //total frames of animation.
  
  Material
    materials[] ;
  Group
    groups[] ;
  Joint
    joints[],
    root ;
  
  
  protected JointModel(String modelName, Class modelClass) {
    super(modelName, modelClass) ;
  }
  
  
  public Sprite makeSprite() {
    return new JointSprite(this) ;
  }
  
  
  public Colour averageHue() {
    return Colour.GREY ;
  }
  
  
  public void reportJoints() {
    for (Joint joint : joints) {
      I.say("Joint name is: "+joint.name) ;
    }
  }
  
  public void reportGroups() {
    for (Group group : groups) {
      I.say("group name is: "+group.name) ;
    }
  }
  
  
  /**  Returns the cumulative joint transforms associated with this model
    *  (possibly inverted.)
    */
  protected Tran3D[] getJointTransforms(
    Joint j, Tran3D trans[], boolean invert
  ) {
    final Tran3D t = trans[j.ID] = new Tran3D().setTo(j) ;
    if (j.parent != null) trans[j.parent.ID].trans(j, t) ;
    for (int n = j.children.length ; n-- > 0 ;)
      getJointTransforms(j.children[n], trans, invert) ;
    //  Transforms are all returned in an inverted state:
    if (invert) t.setInverse(t) ;
    return trans ;
  }
  
  
  /**  Interpolates between two vectors: origin and target.
    */
  private final static void terp(final Vec3D o, final Vec3D t, final float w) {
    o.scale(1 - w) ;
    o.add(t, w, o) ;
  }
  
  
  
  /**  Methods used to match groups and joints-
    */
  public String[] groupNames() {
    final String gN[] = new String[groups.length] ;
    for (int n = gN.length ; n-- > 0 ;)
      gN[n] = groups[n].name ;
    return gN ;
  }
  
  public Object[] groupGeometry(String gName) {
    return groups[groupID(gName)].getGeometry() ;
  }
  
  public int groupID(String gName) {
    for (int n = groups.length ; n-- > 0 ;)
      if (groups[n].name.equals(gName)) return n ;
    return -1 ;
  }
  
  public int jointID(String jName) {
    for (int n = joints.length ; n-- > 0 ;)
      if (joints[n].name.equals(jName)) return n ;
    return -1 ;
  }
  
  
  
  static class Material {
    
    String name ;
    float
      colours[] = new float[16] ;
    float
      shine,
      opacity ;
    
    Texture texture ;
    int ID ;
  }
  
  
  public static class Group {
    
    JointModel model ;
    Material material ;
    String name ;
    
    int polyID[] ;
    
    final static public int
      VERT_GI    = 0,
      NORM_GI    = 1,
      TEXTURE_GI = 2,
      VERTB_GI   = 3,
      NORMB_GI   = 4,
      UVB_GI     = 5,
      VERTA_GI   = 6,
      NORMA_GI   = 7,
      UVA_GI     = 8 ;
    private Object[] geometry = null ;
    private FloatBuffer
      vertB,
      normB,
      textB ;
    static int totalBytesUsed = 0 ;
    
    
    /**  Returns all data pertinent to the given group (selected by index)-
      *  first an array of vertex data, then normals, then UV mapping, and
      *  finally material texture, within an Object array.  i.e:
      *  result[0] = vertices (as vectors.)
      *  result[1] = normals (as vectors.)
      *  results[2] = UV.
      *  results[3] = texture.
      *  (Note: shared vertices will be duplicated for each face.)
      */
    public Object[] getGeometry() {
      if (geometry != null)
        return geometry ;
      int numP = polyID.length, v = 0, n = 0, vI = 0, nI = 0, tI = 0, ID ;
      Vec3D
        vD[] = new Vec3D[numP * 3],
        nD[] = new Vec3D[numP * 3] ;
      float
        vA[] = new float[numP * 9],
        nA[] = new float[numP * 9],
        tA[] = new float[numP * 6] ;
      vertB = BufferUtils.createFloatBuffer(numP * 9) ;
      normB = BufferUtils.createFloatBuffer(numP * 9) ;
      textB = BufferUtils.createFloatBuffer(numP * 6) ;
      totalBytesUsed += numP * (9 + 9 + 6) * 4 ;
      //I.say("Allocated geometry buffers of total byte size: "+totalBytesUsed) ;
      final Object genGeom[] = {
        vD, nD, material.texture,
        vertB, normB, textB,
        vA, nA, tA
      } ;
      
      Vec3D vert, norm ;
      for (int p = 0, i ; p < numP ; p++) {
        ID = polyID[p] * 3 ;
        for (i = 3 ; i-- > 0 ; ID++) {
          vert = vD[v++] = model.vertRel[model.vertID[ID]] ;
          norm = nD[n++] = model.norms[ID] ;
          vA[vI++] = vert.x ; vA[vI++] = vert.y ; vA[vI++] = vert.z ;
          nA[nI++] = norm.x ; nA[nI++] = norm.y ; nA[nI++] = norm.z ;
          tA[tI++] = model.UV[ID * 2] ;
          tA[tI++] = model.UV[(ID * 2) + 1] ;
        }
      }
      vertB.put(vA) ;
      normB.put(nA) ;
      textB.put(tA) ;
      vertB.flip() ;
      normB.flip() ;
      textB.flip() ;
      return (geometry = genGeom) ;
    }
  }
  
  //
  //  TODO:  Make this a form of SpriteBase?  It would simplify things.
  static class Joint extends Tran3D {
    
    Joint
      parent,
      children[] ;
    int ID ;
    String name ;
    Keyframe
      rotFrames[],
      posFrames[] ;
  }
  
  static class Keyframe extends Quat { float time ; }
}
