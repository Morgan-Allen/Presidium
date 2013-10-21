/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.widgets.HUD;
import src.user.* ;
import src.util.* ;



public class SecurityMission extends Mission implements Abilities {
  
  
  /**  Field definitions, constants and save/load methods-
    */
  final static int DURATION_LENGTHS[] = {
    World.STANDARD_DAY_LENGTH,
    World.STANDARD_DAY_LENGTH * 2,
    World.STANDARD_DAY_LENGTH * 4,
  } ;
  final static String DURATION_NAMES[] = {
    "Short, (1 day)",
    "Medium, (2 days)",
    "Long (4 days)",
  } ;
  
  int durationSetting = 0 ;
  float inceptTime = -1 ;
  
  
  
  public SecurityMission(Base base, Target subject) {
    super(
      base, subject,
      MissionsTab.SECURITY_MODEL.makeSprite(),
      "Securing "+subject
    ) ;
  }
  
  
  public SecurityMission(Session s) throws Exception {
    super(s) ;
    durationSetting = s.loadInt() ;
    inceptTime = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(durationSetting) ;
    s.saveFloat(inceptTime) ;
  }
  
  
  public float duration() {
    return DURATION_LENGTHS[durationSetting] ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    float impetus = actor.mind.greedFor(rewardAmount(actor)) * ROUTINE ;
    impetus -= Plan.dangerPenalty(subject, actor) ;
    impetus -= duration() * 0.5f / World.STANDARD_DAY_LENGTH ;
    //
    //  Modify by possession of combat and surveillance skills-
    float ability = 1 ;
    ability *= actor.traits.useLevel(SURVEILLANCE) / 10f ;
    ability *= Combat.combatStrength(actor, null) * 1.5f ;
    ability = Visit.clamp(ability, 0.5f, 1.5f) ;
    
    //I.say("Impetus for "+actor+" is "+impetus) ;
    return impetus * ability ;
  }
  
  
  public boolean finished() {
    if (inceptTime == -1) return false ;
    return (base.world.currentTime() - inceptTime) > duration() ;
  }
  
  
  protected void beginMission() {
    super.beginMission() ;
    inceptTime = base.world.currentTime() ;
  }


  public Behaviour nextStepFor(Actor actor) {
    //
    //  TODO:  You'll also have to allow for sleeping and resting behaviours.
    return Patrolling.securePerimeter(actor, (Element) subject, base.world) ;
  }
  
  
  
  /**  Rendering and interface methods-
    */

  public void writeInformation(Description d, int categoryID, HUD UI) {
    super.writeInformation(d, categoryID, UI) ;
    d.append("\n\nDuration: ") ;
    d.append(new Description.Link(DURATION_NAMES[durationSetting]) {
      public void whenClicked() {
        durationSetting = (durationSetting + 1) % DURATION_LENGTHS.length ;
      }
    }) ;
  }
  
  public void describeBehaviour(Description d) {
    d.append("On ") ;
    d.append("Security Mission", this) ;
    d.append(" around ") ;
    d.append(subject) ;
  }
}






