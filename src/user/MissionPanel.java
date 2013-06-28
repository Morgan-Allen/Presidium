


package src.user ;
import src.game.common.* ;
import src.game.actors.* ;
import src.util.* ;




public class MissionPanel extends InfoPanel {
  
  
  
  float missionPriority ;
  
  class Role {
    Actor applicant ;
    int roleType ;
    
    Object reward = null ;
    int rewardAmount = -1 ;
    boolean approved ;
  }
  
  List <Role> roles = new List <Role> () ;
  
  
  
  
  public MissionPanel(BaseUI UI, Selectable selected) {
    super(UI, selected, 0) ;
  }
  
  

  protected void updateText() {
    //
    //  You need the ability to set overall urgency, see volunteers, and screen
    //  those who seem suitable.  
    detailText.append("Priority: "+Behaviour.descFor(missionPriority)) ;
    //
    //  Here, you can approve the mission, cancel the mission, or visit your
    //  personnel listings (full household.)
    
    
    
    for (Role role : roles) {
      detailText.append("\n\n  ") ;
      detailText.append(role.applicant) ;
      if (role.reward != null) {
        String response = Voicelines.response(role.applicant, role.reward) ;
        detailText.append("\n  "+response) ;
        detailText.append("\n  ") ;
        detailText.append(role.reward) ;
      }
      //
      //  You can approve, reject, or negotiate over the offer.
    }
  }
}

















