/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.actors ;
import src.game.common.* ;
import src.util.* ;



public class Activities {
  
  
  
  /**  Fields, constructors, and save/load methods.
    */
  final World world ;
  
  final Table <Target, List <Action>> actions = new Table(1000) ;
  final Table <Behaviour, Behaviour> behaviours = new Table(1000) ;
  
  
  public Activities(World world) {
    this.world = world ;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Target t = s.loadTarget() ;
      final List <Action> l = new List <Action> () ;
      s.loadObjects(l) ;
      actions.put(t, l) ;
    }
    for (int n = s.loadInt() ; n-- > 0 ;) {
      final Behaviour b = (Behaviour) s.loadObject() ;
      behaviours.put(b, b) ;
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(actions.size()) ;
    ///I.say("Saving "+actions.size()+" actions in activity table.") ;
    for (Target t : actions.keySet()) {
      s.saveTarget(t) ;
      s.saveObjects(actions.get(t)) ;
    }
    s.saveInt(behaviours.size()) ;
    ///I.say("Saving "+behaviours.size()+" behaviours in behaviour table.") ;
    for (Behaviour b : behaviours.keySet()) {
      s.saveObject(b) ;
    }
  }
  
  
  
  /**  Asserting and asking after action registrations-
    */
  public void toggleActive(Behaviour b, boolean is) {
    if (is) behaviours.put(b, b) ;
    else behaviours.remove(b) ;
  }
  
  
  public boolean includes(Behaviour b) {
    return behaviours.get(b) != null ;
  }
  
  
  public void toggleActive(Action a, boolean is) {
    if (a == null) return ;
    final Target t = a.target() ;
    List <Action> forT = actions.get(t) ;
    if (is) {
      if (forT == null) actions.put(t, forT = new List <Action> ()) ;
      forT.add(a) ;
    }
    else if (forT != null) {
      forT.remove(a) ;
      if (forT.size() == 0) actions.remove(t) ;
    }
  }
  
  
  public boolean includes(Target t, String methodName) {
    final List <Action> onTarget = actions.get(t) ;
    if (onTarget == null) return false ;
    for (Action a : onTarget) {
      if (a.methodName().equals(methodName)) return true ;
    }
    return false ;
  }
  
  
  public boolean includes(Target t, Class behaviourClass) {
    final List <Action> onTarget = actions.get(t) ;
    if (onTarget == null) return false ;
    for (Action a : onTarget) {
      for (Behaviour b : a.actor.AI.agenda()) {
        if (b.getClass() == behaviourClass) return true ;
      }
    }
    return false ;
  }
  
  
  public Batch <Behaviour> targeting(Target t) {
    final Batch <Behaviour> batch = new Batch <Behaviour> () ;
    final List <Action> onTarget = actions.get(t) ;
    if (onTarget == null) return batch ;
    for (Action a : onTarget) for (Behaviour b : a.actor.AI.agenda()) {
      batch.add(b) ;
    }
    return batch ;
  }
}



/*
public boolean assigned(Target t, String methodName) {
  final List <Action> onTarget = actions.get(t) ;
  if (onTarget == null) return false ;
  for (Action a : onTarget) {
    if (a.methodName().equals(methodName)) return true ;
  }
  return false ;
}


public Batch <Action> matches(Target t, String methodName) {
  final Batch <Action> found = new Batch <Action> () ;
  final List <Action> onTarget = actions.get(t) ;
  if (onTarget != null) for (Action a : onTarget) {
    if (a.methodName().equals(methodName)) found.add(a) ;
  }
  return found ;
}


public boolean assigned(Target t, Behaviour behaviour) {
  final List <Action> onTarget = actions.get(t) ;
  if (onTarget == null) return false ;
  for (Action a : onTarget) for (Behaviour b : a.actor.currentBehaviours()) {
    if (b.equals(behaviour)) return true ;
  }
  return false ;
}


public boolean contains(Target t, Class behaviourClass) {
  final List <Action> onTarget = actions.get(t) ;
  if (onTarget == null) return false ;
  for (Action a : onTarget) for (Behaviour b : a.actor.currentBehaviours()) {
    if (b.getClass() == behaviourClass) return true ;
  }
  return false ;
}


public Batch <Behaviour> matches(Target t, Class behaviourClass) {
  final Batch <Behaviour> found = new Batch <Behaviour> () ;
  final List <Action> onTarget = actions.get(t) ;
  if (onTarget == null) return found ;
  for (Action a : onTarget) for (Behaviour b : a.actor.currentBehaviours()) {
    if (b.getClass() == behaviourClass) found.add(b) ;
  }
  return found ;
}
//*/






