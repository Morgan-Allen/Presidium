/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.graphics.jointed ;
import src.util.* ;
import src.graphics.common.* ;
import java.io.* ;



/**  ...Loads a model from the MS3D file-format.
  *  The format is little-endian, so specialised loading routines are required.
  */
public class MS3DModel extends JointModel {
  
  
  
  public static JointModel loadMS3D(
    Class modelClass, String pathName, String fileName, float scale
  ) {
    final String modelName = "MS3D-"+fileName ;
    Object cached = LoadService.getResource(modelName) ;
    if (cached != null) return (JointModel) cached ;
    final MS3DModel modelD = new MS3DModel(modelName, modelClass) ;
    try { modelD.loadFrom(pathName, fileName) ; }
    catch (Exception e) { I.report(e) ; return null  ; }
    LoadService.cacheResource(modelD, modelName) ;
    modelD.type = TYPE_JOINT ;
    modelD.format = FORMAT_MS3D ;
    modelD.scale = scale ;
    return modelD ;
  }
  
  
  static boolean verbose = false ;
  
  
  private MS3DModel(String modelName, Class modelClass) {
    super(modelName, modelClass) ;
  }
  
  
  final static Tran3D
    ROT_X = new Tran3D(),
    INV_R = new Tran3D() ;
  static {
    ROT_X.rotation.set(  //-90 degrees about the X axis.
      01, 00, 00,
      00, 00, -1,
      00, 01, 00
    ) ;
    INV_R.setInverse(ROT_X) ;
  }
  
  
  public void saveModel(String pathName, String fileName) throws Exception {
    final DataOutputStream output = new DataOutputStream(
      new BufferedOutputStream(new FileOutputStream(
        new File(LoadService.safePath(pathName+fileName)
    )))) ;
    if (verbose) I.say("SAVING MS3D FILE: "+pathName+fileName) ;
    //
    //  Write header information-
    saveMS3DString("MS3D000000", output, 10) ;
    output.write(new byte[] { 4, 0, 0, 0 }) ;
    //
    //  Save the vertices.
    final short numV = (short) vertRel.length ;
    if (verbose) I.say("__TOTAL VERTS: "+numV) ;
    saveLEShort(numV, output) ;
    final Vec3D v = new Vec3D() ;
    for (int n = 0 ; n < numV ; n++) {
      v.setTo(vertAbs[n]) ;
      INV_R.trans(v, v) ;
      output.writeByte(0) ;  //vertex flags.
      saveLEFloat(v.x, output) ;
      saveLEFloat(v.y, output) ;
      saveLEFloat(v.z, output) ;
      output.writeByte(0) ;  //parent joint index!
      output.writeByte(0) ;  //vertex refCount.
    }
    //
    //  Save the polygons.  To do so, we have to get the cumulative transforms
    //  associated with the model's joints, so that normals can be saved in
    //  absolute, rather than relative coordinates-
    final Tran3D jointTrans[] = getJointTransforms(
      root, new Tran3D[joints.length], false
    ) ;
    final short numP = (short) (vertID.length / 3) ;
    if (verbose) I.say("__TOTAL POLYS: "+numP) ;
    saveLEShort(numP, output) ;
    final Vec3D norm = new Vec3D() ;
    for (int n = 0 , i = 0, l, o ; n < numP ; n++) {
      output.writeShort(0) ;  //polygon flags.
      saveLEShort((short) vertID[i++], output) ;
      saveLEShort((short) vertID[i++], output) ;
      saveLEShort((short) vertID[i++], output) ;
      o = n * 3 ;  //normal data.
      for (l = 3 ; l-- > 0 ; o++) {
        norm.setTo(norms[o]) ;
        final Joint j = joints[joinID[vertID[o]]] ;
        jointTrans[j.ID].rotation.trans(norm, norm) ;
        INV_R.trans(norm, norm) ;
        saveLEFloat(norm.x, output) ;
        saveLEFloat(norm.y, output) ;
        saveLEFloat(norm.z, output) ;
      }
      o = n * 6 ;  //texture coord data.
      for (l = 0 ; l < 5 ; l += 2) saveLEFloat(UV[l + o], output) ;
      for (l = 1 ; l < 6 ; l += 2) saveLEFloat(UV[l + o], output) ;
      output.writeByte(0) ;  //poly smooth group.
      output.writeByte(0) ;  //poly group index.
    }
    //
    //  Here, we save group information.
    final short numG = (short) groups.length ;
    saveLEShort(numG, output) ;
    for (int n = 0 ; n < numG ; n++) {
      final Group g = groups[n] ;
      output.writeByte(0) ;  //group flags.
      saveMS3DString(g.name, output, 32) ;
      saveLEShort((short) g.polyID.length, output) ;
      for (int ID : g.polyID) saveLEShort((short) ID, output) ;
      output.writeByte(-1) ;  //material index.
    }
    //
    //  Strictly speaking, that should be adequate for now.  We'll fill in the
    //  rest with blanks.
    //
    //  ...No materials-
    saveLEShort((short) 0, output) ;
    //
    //  And no animation data-
    saveLEFloat(25, output) ;  //frames per second
    saveLEFloat(0, output) ;  //current animation frame
    saveLEInt(0, output) ;  //total frames of animation
    saveLEShort((short) 0, output) ;  //number of joints.
    //
    //  Tidy up afterwards-
    output.close() ;
  }
  
  
  private void loadFrom(String pathName, String fileName) throws Exception {
    final DataInputStream input = new DataInputStream(
      new BufferedInputStream(new FileInputStream(
        new File(LoadService.safePath(pathName+fileName)
    )))) ;
    if (verbose) I.say("LOADING MS3D FILE: "+pathName+fileName) ;
    //
    //  First, the header data (name and version.)
    final String program = loadMS3DString(input, 10) ;
    final int version = loadLEInt(input) ;
    if (verbose) I.say("__HEADER: "+program+", version: "+version) ;
    //
    //  Secondly, the vertices:
    final int numV = loadLEShort(input) ;
    if (verbose) I.say("__VERTS TO LOAD: "+numV) ;
    vertRel = new Vec3D[numV] ;
    vertAbs = new Vec3D[vertRel.length] ;
    joinID = new byte[numV] ;
    float x, y, z ;
    //
    for (int n = 0; n < numV ; n++) {
      vertRel[n] = new Vec3D() ;
      vertAbs[n] = new Vec3D() ;
      input.read() ;  //vertex flags.  useless information.
      x = loadLEFloat(input) ;
      y = loadLEFloat(input) ;
      z = loadLEFloat(input) ;
      vertAbs[n].set(x, y, z) ;  // need to rotate axes.
      joinID[n] += input.read() ;  //parent joint index.
      input.read() ;  //vertex refCount.  useless information.
    }
    if (verbose) I.say("__VERTICES LOADED.") ;
    //
    //  Thirdly, the polygon data.
    final int numP = loadLEShort(input) ;
    if (verbose) I.say("__POLYS TO LOAD: "+numP) ;
    norms = new Vec3D[numP * 3] ;
    vertID = new int[numP * 3] ;
    UV = new float[numP * 6] ;
    
    for (int n = 0, i = 0, l, o ; n < numP ; n++) {
      input.readShort() ;  //poly flags.  useless information.
      vertID[i++] = loadLEShort(input) ;
      vertID[i++] = loadLEShort(input) ;
      vertID[i++] = loadLEShort(input) ;
      o = n * 3 ;  //normal data.
      for (l = 3 ; l-- > 0 ;) {
        x = loadLEFloat(input) ;
        y = loadLEFloat(input) ;
        z = loadLEFloat(input) ;
        (norms[o++] = new Vec3D()).set(x, y, z) ;  // need to rotate axes.
      }
      o = n * 6 ;  //texture coord data.
      for (l = 0 ; l < 5 ; l += 2) UV[l + o] = loadLEFloat(input) ;
      for (l = 1 ; l < 6 ; l += 2) UV[l + o] = loadLEFloat(input) ;
      input.read() ;  //poly smooth group.  useless infor mation.
      input.read() ;  //poly group index.  Not useful at present.
    }
    if (verbose) I.say("__POLYGONS LOADED.") ;
    //
    //  Fourthly, the group data.
    final int numG = loadLEShort(input) ;
    groups = new Group[numG] ;
    int matID[] = new int[numG] ;  //temporary storage for later assignment.
    
    for (int n = 0, p, i, pID[] ; n < numG ; n++) {
      groups[n] = new Group() ;
      groups[n].model = this ;
      input.read() ;  //group flags.  useless information.
      groups[n].name = loadMS3DString(input, 32) ;  //group name.
      groups[n].polyID = pID = new int[p = loadLEShort(input)] ;
      for (i = 0 ; i < p ; i++) pID[i] = loadLEShort(input) ;  //poly indices.
      matID[n] = input.read() ;  //material indices.
      //I.say("Group name is: "+groups[n].name) ;
    }
    //
    //  Fifthly, the material data...
    final int numM = loadLEShort(input) ;
    //
    //  In the event that no material is provided, make one.
    if (numM < 1) {
      materials = new Material[1] ;
      Material mat = materials[0] = new Material() ;
      //mat.model = this ;
      mat.name = "DEFAULT_MATERIAL" ;
      final float colours[] =
        { 1, 1, 1, 1,  1, 1, 1, 1,  0, 0, 0, 0,  0, 0, 0, 0 } ;
      mat.colours = colours ;
      mat.shine = 0 ;
      mat.opacity = 1 ;
      mat.texture = Texture.WHITE_TEX ;
      for (int n = numG ; n-- > 0 ;)
        groups[n].material = mat ;
    }
    //
    //  ...Otherwise, load them all from file.
    else {
      materials = new Material[numM] ;
      String textF, alphF ;
      
      for (int n = 0, c ; n < numM ; n++) {
        materials[n] = new Material() ;
        materials[n].ID = n ;
        materials[n].name = loadMS3DString(input, 32) ;
        for (c = 0 ; c < 16 ; c++)
          materials[n].colours[c] = loadLEFloat(input) ;
        materials[n].shine = loadLEFloat(input) ;
        materials[n].opacity = loadLEFloat(input) ;
        input.read() ;  //group mode.  useless information.
        textF = loadMS3DString(input, 128)  ;  //colour data file,
        alphF = loadMS3DString(input, 128)  ;  //...and optional alpha data.
        if (alphF.length() > 0) { //two different loading procedures are used:
          materials[n].texture = Texture.loadTexture(
            pathName+textF, pathName+alphF
          ) ;
        }
        else {
          materials[n].texture = Texture.loadTexture(pathName+textF) ;
        }
      }
      //This then assigns the right materials to the groups previously made..
      for (int n = numG ; n-- > 0 ;) {
        if (matID[n] == 0xff) groups[n].material = materials[0] ;
        else groups[n].material = materials[matID[n]] ;
      }
    }
    
    loadLEFloat(input) ;  //frames per second, not used right now.
    loadLEFloat(input) ;  //current animation frame, also unused...
    animLength = loadLEInt(input) ;  //-total frames of animation...
    //
    //  Last- but by no means least- the joint data, required for animation:
    final int numJ = loadLEShort(input) ;
    joints = new Joint[numJ + 1] ;  //(plus root.)
    String namePJ[] = new String[joints.length] ;  //parent joint names...
    int
      nRot,
      nPos ;
    Keyframe frames[] ;
    
    for (int n = 0, k ; n < numJ ; n++) {
      joints[n] = new Joint() ;
      
      input.read() ;  //joint flags.  useless information.
      joints[n].name = loadMS3DString(input, 32) ;
      //I.say("Joint name is: "+joints[n].name) ;
      namePJ[n] = loadMS3DString(input, 32) ;  //"" if joint has no parent.
      x = loadLEFloat(input) ;
      y = loadLEFloat(input) ;
      z = loadLEFloat(input) ;
      joints[n].rotation.setEuler(x, y, z) ;
      x = loadLEFloat(input) ;
      y = loadLEFloat(input) ;
      z = loadLEFloat(input) ;
      joints[n].position.set(x, y, z) ;
      
      nRot = loadLEShort(input) ;  //the number of keyframe rotations.
      nPos = loadLEShort(input) ;  //...and positioning(s).
      frames = joints[n].rotFrames = new Keyframe[nRot] ;
      for (k = 0 ; k < nRot ; k++) {
        (frames[k] = new Keyframe()).time = loadLEFloat(input) ;
        x = loadLEFloat(input) ;
        y = loadLEFloat(input) ;
        z = loadLEFloat(input) ;
        frames[k].setEuler(x, y, z) ;
      }
      frames = joints[n].posFrames = new Keyframe[nPos] ;
      for (k = 0 ; k < nPos ; k++) {
        (frames[k] = new Keyframe()).time = loadLEFloat(input) ;
        x = loadLEFloat(input) ;
        y = loadLEFloat(input) ;
        z = loadLEFloat(input) ;
        frames[k].set(x, y, z, 0) ;
      }
    }
    
    input.close() ;
    //
    //  All neccesary data has now been read.  What follows is just
    //  post-processing:
    //  The root is always the last joint in the array:
    root = joints[numJ] = new Joint() ;
    root.rotFrames = root.posFrames = new Keyframe[0] ;  //no keyframes...
    root.name = "" ;  //all orphan joints have this as 'parent name'.
    //
    //  This assigns the correct parent/child-list to each joint previously
    //  loaded.
    int numKids[] = new int[numJ + 1] ;
    for (int n = numJ, p ; n-- > 0 ;)
      for (p = numJ + 1 ; p-- > 0 ;)
        if (namePJ[n].equals(joints[p].name)) {
          joints[n].parent = joints[p] ;
          numKids[p]++ ;
        }
    for (int n = numJ + 1 ; n-- > 0 ;) {
      joints[n].children = new Joint[numKids[n]] ;
      joints[n].ID = n ;
    }
    for (int n = numJ ; n-- > 0 ;)
      joints[n].parent.children[--numKids[joints[n].parent.ID]] = joints[n] ;
    //
    //  Now we must build the initial joint tree (from root,) and set model
    //  vertices and normals as relative to their parent joints.  But because
    //  Milkshape considers the Y axis to be vertical, some rotations en route
    //  are necessary...
    for (Joint j : root.children) ROT_X.trans(j, j) ;
    final Tran3D inverts[] = getJointTransforms(
      root, new Tran3D[numJ + 1], true
    ) ;
    for (int n = 0 ; n < numV ; n++) {
      //  If the vertex is an orphan, make the root it's parent:
      byte jID = joinID[n] ;
      if (jID == -1) joinID[n] = jID = (byte) numJ ;
      ROT_X.trans(vertAbs[n], vertAbs[n]) ;
      inverts[jID].trans(vertAbs[n], vertRel[n]) ;
    }
    //  Similarly with normals:
    for (int n = norms.length ; n-- > 0 ;) {
      ROT_X.trans(norms[n], norms[n]) ;
      inverts[joinID[vertID[n]]].rotation.trans(norms[n], norms[n]) ;
    }
  }
  
  
  
  private static final byte
    intB[] = new byte[4],
    shortB[] = new byte[2] ;  //used by read methods (below.)
  
  private static final int loadLEInt(
    DataInputStream in
  ) throws IOException {
    in.read(intB) ;  //little-endian int.
    return
      ((int) (intB[0] & 0xff)) |
      ((intB[1] & 0xff) << 8)  |
      ((intB[2] & 0xff) << 16) |
      ((intB[3] & 0xff) << 24) ;
  }
  
  private static final void saveLEInt(
    int i, DataOutputStream out
  ) throws IOException {
    intB[0] = (byte) (i & 0xff)         ;
    intB[1] = (byte) ((i >> 8)  & 0xff) ;
    intB[2] = (byte) ((i >> 16) & 0xff) ;
    intB[3] = (byte) ((i >> 24) & 0xff) ;
    out.write(intB) ;
  }
  
  
  private static final int loadLEShort(
    DataInputStream in
  ) throws IOException {
    in.read(shortB) ;  //little-endian short.
    return
      ((int) (shortB[0] & 0xff)) |
      ((shortB[1] & 0xff) << 8) ;
  }
  
  private static final void saveLEShort(
    short s, DataOutputStream out
  ) throws IOException {
    shortB[0] = (byte) (s & 0xff) ;
    shortB[1] = (byte) ((s >> 8) & 0xff) ;
    out.write(shortB) ;
  }
  
  
  private static final float loadLEFloat(
    DataInputStream in
  ) throws IOException {
    return Float.intBitsToFloat(loadLEInt(in)) ;  //little-endian IEEE float...
  }
  
  private static final void saveLEFloat(
    float f, DataOutputStream out
  ) throws IOException {
    saveLEInt(Float.floatToRawIntBits(f), out) ;
  }
  
  
  private static final String loadMS3DString(
    DataInputStream in, int len
  ) throws IOException {
    byte chars[] = new byte[len], end = 0 ;
    in.read(chars) ;  //trims the string if a null terminator is found.
    while ((chars[end] != (byte) 0) && (++end < chars.length)) ;
    return new String(chars, 0, end) ;
  }
  
  private static final void saveMS3DString(
    String s, DataOutputStream out, int len
  ) throws IOException {
    byte chars[] = new byte[len] ;
    char sChars[] = s.toCharArray() ;
    int n = 0 ;
    while (n < sChars.length) chars[n] = (byte) sChars[n++] ;
    while (n < chars.length) chars[n++] = 0 ;
    out.write(chars) ;
  }
}


