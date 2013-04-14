/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.util ;


public abstract class SortTree <K> {
  
  public static enum Side { L, R, BOTH, NEITHER } ;
  
  int size ;
  Node root ;
  
  
  /**  Definition for a single node within the tree.
    */
  class Node {
    
    Node parent, kidL, kidR ;
    Side side ;
    int height = 0 ;
    
    final K value ;
    
    Node(K value) {
      this.value = value ;
      if (value == null) throw new RuntimeException("NODE VALUE IS NULL!") ;
    }
    
    void setParent(Node p, Side s) {
      if (p == null) { root = this ; parent = null ; }
      else p.setKid(this, s) ;
    }
    
    void setKid(Node k, Side s) {
      if (k != null) { k.side = s ; k.parent = this ; }
      if (s == Side.L) kidL = k ;
      else kidR = k ;
      calcHeight() ;
    }
    
    Node kid(Side s) {
      return (s == Side.L) ? kidL : kidR ;
    }
    
    void calcHeight() {
      final int hL = heightAfter(kidL), hR = heightAfter(kidR) ;
      height = hL > hR ? hL : hR ;
    }
    
    boolean tryBalance() {
      final int hL = heightAfter(kidL), hR = heightAfter(kidR) ;
      if (hL > hR + 1) { rotate(this, Side.L) ; return true ; }
      if (hR > hL + 1) { rotate(this, Side.R) ; return true ; }
      return false ;
    }
  }
  
  
  static Side opposite(Side S) { return S == Side.L ? Side.R : Side.L ; }
  
  int heightAfter(Node n) { return (n == null) ? 0 : n.height + 1 ; }
  
  void tryBalanceFrom(Node node) {
    for (; node != null ; node = node.parent) {
      node.tryBalance() ;
    }
  }
  
  
  
  
  
  /**  Code for handling a hierarchical descent through the tree.
    */
  public static abstract class Descent <K> {
    protected abstract Side descentFrom(K k) ;
    protected abstract boolean process(Object node, K k) ;
  }
  
  public void applyDescent(Descent descent) {
    process(descent, root) ;
  }
  
  private void process(Descent descent, Node node) {
    if (node == null || ! descent.process(node, node.value)) return ;
    final Side S = descent.descentFrom(node.value) ;
    if (S == Side.L || S == Side.BOTH) process(descent, node.kidL) ;
    if (S == Side.R || S == Side.BOTH) process(descent, node.kidR) ;
  }

  protected abstract boolean greater(K a, K b) ;
  protected abstract boolean match(K a, K b) ;
  
  public Node greatest() {
    if (root == null) return null ;
    Node node = root ;
    while (node.kidR != null) node = node.kidR ;
    return node ;
  }
  
  public Node least() {
    if (root == null) return null ;
    Node node = root ;
    while (node.kidL != null) node = node.kidL ;
    return node ;
  }
  
  public int size() { return size ; }
  
  public K valueFor(Object ref) { return ((Node) ref).value ; }
  

  /**  Inserts a single value into the tree as a leaf node, rebalancing as
    *  necessary.
    */
  public Object insert(K value) {
    Node node = root, last = null ; Side S = Side.L ;
    while (node != null) {
      last = node ;
      S = greater(node.value, value) ? Side.L : Side.R ;
      node = node.kid(S) ;
    }
    size++ ;
    final Node made = new Node(value) ;
    made.setParent(last, S) ;
    tryBalanceFrom(made) ;
    return made ;
  }
  
  
  /**  'Shortens' the node's branch on the given side.
    */
  private void rotate(Node node, Side S) {
    final Node oldParent = node.parent ;
    final Side O = opposite(S) ;
    Node
      kidS = node.kid(S),       //kid on the branch being 'pulled up'.
      grandO = kidS.kid(O) ;    //(grand)kid of kidS on opposite side.
    //
    //  This step is needed to prevent a loop condition based on an imbalanced
    //  'limb' of the tree simply being switched from one side to another.
    if (kidS.height > 0 && heightAfter(grandO) > heightAfter(kidS.kid(S))) {
      rotate(kidS, O) ;
      //
      //  If balancing has occurred, these nodes will have changed-
      kidS = node.kid(S) ;
      grandO = kidS.kid(O) ;
    }
    //
    //  kidS assumes the 'place' of the old node (adopts it's parent.)
    //  grandO becomes kid(S) of node.
    final Side pS = node.side ;
    kidS.setKid(node, O) ;
    node.setKid(grandO, S) ;
    kidS.setParent(oldParent, pS) ;
    node.calcHeight() ;
    kidS.calcHeight() ;
  }
  
  
  /**  Returns the S-most descendant of the given node.
    */
  private Node closest(Node from, Side S) {
    Node close = from, kid ;
    while ((kid = close.kid(S)) != null) close = kid ;
    return close ;
  }
  
  
  /**  Deletes the given Node from the tree.
    */
  public void delete(Object ref) {
    //
    //  The purpose of the 'from' reference is to keep track of the point in
    //  the tree from which rebalancing should be propagated.
    if (ref == null) return ;
    final Node node = (Node) ref, from ;
    size-- ;
    //
    //  If the node is already a leaf, replacement is trivial.
    if (node.height <= 0) {
      if (node == root) { root = null ; return ; }
      node.parent.setKid(null, node.side) ;
      from = node.parent ;
    }
    //
    //  Otherwise, we need to find the most suitable replacement that can 'fill
    //  the void' left by the deletion of this node.  So, we try to find the
    //  single descendant node closest in value:
    else {
      final Side S = heightAfter(node.kidL) > heightAfter(node.kidR) ?
        Side.L :
        Side.R ;
      if (node.kidL == null && node.kidR == null) {
        I.say(this.toString()) ;
        throw new RuntimeException("BOTH KIDS OF "+node.value+" ARE NULL!") ;
      }
      final Node closest = closest(node.kid(S), opposite(S)) ;
      //
      //  Now, this node, by definition, has at most one child, and that child
      //  must be on the 'wrong' side (otherwise it would be a closer match.)
      if (closest.height > 0) {
        //
        //  Then replace the closest node with it's sole child.
        final Node kid = closest.kid(S) ;
        kid.setParent(closest.parent, closest.side) ;
        from = kid ;
      }
      else {
        //
        //  Otherwise, it's a leaf node, and can be deleted trivially.
        closest.parent.setKid(null, closest.side) ;
        if (closest.parent == node) from = closest ;
        else from = closest.parent ;
      }
      //
      //  Finally, replace the deleted node with it's closest match-
      closest.setKid(node.kidL == closest ? null : node.kidL, Side.L) ;
      closest.setKid(node.kidR == closest ? null : node.kidR, Side.R) ;
      closest.setParent(node.parent, node.side) ;
    }
    tryBalanceFrom(from) ;
  }
  
  
  
  /**  Printout routines follow-
    */
  public String toString() {
    final StringBuffer sB = new StringBuffer() ;
    sB.append("\n\nTREE CONTENTS ARE:\n") ;
    reportNode(null, root, "\n  ", sB) ;
    sB.append("\n  Total Size: "+size) ;
    return sB.toString() ;
  }
  
  private void reportNode(
    Side S, Node node,
    String indent, StringBuffer sB
  ) {
    if (node == null) return ;
    else {
      final String tick = (S == null) ? "" : ((S == Side.L) ? " \\" : " /") ;
      final String cross = (S == null) ?
        ">-|- " :
        ((node.height > 0)) ? "|- " : ">- " ;
      reportNode(Side.R, node.kidR, indent + indentor(Side.R, node), sB) ;
      sB.append(indent+tick+cross+node.value+" (height: "+node.height+" ") ;
      sB.append((node.side == Side.L) ? "[L])" : "[R])") ;
      reportNode(Side.L, node.kidL, indent + indentor(Side.L, node), sB) ;
    }
  }
  
  private String indentor(Side K, Node node) {
    if (node.parent == null) return "  " ;
    return (K == node.side) ? "  " : "| " ;
  }
  

  /**  Testing routine.
    *  TODO:  Mix in deletions with insertions in future versions...
    */
  public static void main(String args[]) {
    final SortTree <Integer> testTree = new SortTree <Integer> () {
      protected boolean greater(Integer a, Integer b) { return a > b ; }
      protected boolean match(Integer a, Integer b) { return ((int) a) == b ; }
    } ;
    final int size = 10 ;
    Stack nodes = new Stack () ;
    for (int n = size ; n-- > 0 ;) {
      final int val = (int) (Math.random() * size) ;
      nodes.addLast(testTree.insert(val)) ;
    }
    I.add(testTree.toString()) ;
    for (int n = nodes.size() ; n-- > 0 ;) {
      final Object gone = nodes.removeIndex((int) (Math.random() * n)) ;
      testTree.delete(gone) ;
    }
    I.add(testTree.toString()) ;
  }
}
