

package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.graphics.common.* ;
import src.util.* ;
import src.user.* ;



//
//  TODO:  These need to actually test the psyonic skills in question, and
//  (at least for testing purposes) work without a caster.
//
//  TODO:  Make this available to actors, once the feedback mechanism is in
//  place(?)


public class Power implements Abilities {
  
  
  final static String IMG_DIR = "media/GUI/Powers/" ;
  
  final public static int
    NONE        = 0,
    PLAYER_ONLY = 1,
    MAINTAINED  = 2 ;
  
  final public String name, helpInfo ;
  final public Texture buttonTex ;

  final int properties ;
  static String saveFile = "saves/main_session.rep" ;  //GET FROM SESSION CLASS
  
  
  Power(String name, int properties, String imgFile, String helpInfo) {
    this.name = name ;
    this.helpInfo = helpInfo ;
    this.buttonTex = Texture.loadTexture(IMG_DIR+imgFile) ;
    this.properties = properties ;
  }
  
  
  //
  //  List of options.  List of target classes, with options for further
  //  refinement.
  //  Pass in caster, option, targets after culling, and allow for regular
  //  updates with a particular binding.
  
  //  ...I think I'm overthinking this.  Just invoke directly with the current
  //  selection and mouse state.
  
  
  public boolean finishedWith(
    Actor caster, String option,
    Target selected, boolean clicked
  ) {
    return false ;
  }
  
  
  public String[] options() {
    return null ;
  }
  
  
  
  final public static Power
    //
    //  Loads the most recently saved game session.
    WALK_THE_PATH = new Power(
      "Walk The Path", PLAYER_ONLY, "power_remembrance.gif",
      "Accept your vision of events and allow them to be fulfilled.\n(Saves "+
      "current game.)"
    ) {
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (caster == null || GameSettings.psyFree) {
          PlayLoop.saveGame(saveFile) ;
          return true ;
        }
        final float bonus = caster.traits.useLevel(PREMONITION) / 1 ;
        float lastSave = PlayLoop.timeSinceLastSave() ;
        caster.health.adjustPsy(0 - (lastSave / 1000f) * (10 + bonus)) ;
        PlayLoop.saveGame(saveFile) ;
        return true ;
      }
    },
    //
    //  Saves the current game session.
    DENY_THE_VISION = new Power(
      "Deny The Vision", PLAYER_ONLY, "power_foresight.gif",
      "Aborts your precognitive vision and lets you choose a different path."+
      "\n(Loads a previous game.)"
    ) {
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        //
        //  You have to get rid of references to the target/caster in order for
        //  garbage collection to work.
        if (caster == null || GameSettings.psyFree) {
          PlayLoop.loadGame(saveFile) ;
          return true ;
        }
        caster = null ;
        PlayLoop.loadGame(saveFile) ;
        caster = PlayLoop.played().ruler() ;
        if (caster == null) throw new RuntimeException("RULER DOESN'T EXIST!") ;
        
        final float bonus = caster.traits.useLevel(PREMONITION) / 10 ;
        caster.health.adjustPsy(-10f / (0.5f + bonus)) ;
        PlayLoop.saveGame(saveFile) ;
        return true ;
      }
    },
    //
    //  Slows down the current game speed.
    TIME_DILATION = new Power(
      "Time Dilation", PLAYER_ONLY | MAINTAINED, "power_time_dilation.gif",
      "Insulates your experience from temporal passage.\n(Reduces game speed.)"
    ) {
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        final boolean slowed = PlayLoop.gameSpeed() < 1 ;
        if (slowed) {
          PlayLoop.setGameSpeed(1) ;
          return true ;
        }
        
        if (caster == null || GameSettings.psyFree) {
          PlayLoop.setGameSpeed(0.5f) ;
          return true ;
        }
        
        final float bonus = caster.traits.useLevel(PROJECTION) / 2 ;
        PlayLoop.setGameSpeed(1f / (float) (1f + Math.sqrt(bonus))) ;
        caster.health.adjustPsy(-1) ;
        return true ;
      }
    },
    //
    //  Grants vision of a distant portion of the map- drains psych based on
    //  the relative proportion of new terrain revealed and distance from the
    //  caster.
    REMOTE_VIEWING = new Power(
      "Remote Viewing", PLAYER_ONLY, "power_remote_viewing.gif",
      "Grants you an extra-sensory perception of distant places or persons."+
      "\n(Lifts fog around target terrain.)"
    ) {
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (clicked != true || ! (selected instanceof Tile)) return false ;
        final Tile tile = (Tile) selected ;
        
        if (caster == null || GameSettings.psyFree) {
          PlayLoop.played().intelMap.liftFogAround(tile.x, tile.y, 9) ;
          return true ;
        }
        
        final float bonus = caster.traits.useLevel(PROJECTION) / 5 ;
        final float radius = 9 + (bonus * bonus) ;
        float revealed = caster.base().intelMap.liftFogAround(
          tile.x, tile.y, radius
        ) ;
        caster.health.adjustPsy(
          -2f * (revealed / (float) (radius * radius * Math.PI)) *
          (float) Math.sqrt(Spacing.distance(tile, caster))
        ) ;
        
        return true ;
      }
    },
    
    
    //
    //  Flings the subject item or actor in a desired direction.
    TELEKINESIS = new Power(
      "Telekinesis", NONE, "power_telekinesis.gif",
      "Imparts spatial moment to a chosen material object.\n(Hurls or carries "+
      "the target in an indicated direction.)"
    ) {
      /*
      public boolean acceptsTarget(
        Target nextTarget, boolean clicked, Target... args
      ) {
        if (args.length == 0) {
          return clicked && nextTarget instanceof Actor ;
        }
        if (args.length == 1) {
          return clicked && nextTarget instanceof Tile ;
        }
        return false ;
      }
      
      
      public boolean updateWith(Actor caster, Target... args) {
        
        return true ;
      }
      
      
      public boolean finishedWith(Actor caster, Target... args) {
        return true ;
      }
      
      void applyEffect(Target target, Actor caster) {
        final BaseUI UI = (BaseUI) PlayLoop.currentUI() ;
        final Target picked = (Target) UI.selection.hovered() ;
        if (Spacing.distance(target, picked) < 2f) return ;
        final Mobile subject = (Mobile) target ;
        final Vec3D velocity = picked.position(null) ;
        velocity.sub(subject.position(null)) ;
        final float bonus = caster.traits.useLevel(TRANSDUCTION) / 5 ;
        velocity.normalise().scale(2 * (1 + bonus)) ;
        
        I.complain("NOT FINISHED YET!") ;
        //subject.impartVelocity(velocity) ;
        caster.health.adjustPsy(-4) ;
      }
      //*/
    },
    
    
    //
    //  Grants the subject a significant boost to their shield strength,
    //  whether they wear armour or not.
    FORCEFIELD = new Power(
      "Forcefield", MAINTAINED, "power_forcefield.gif",
      "Encloses the subject in a selectively permeable suspension barrier.\n"+
      "(Temporarily raises shields on target.)"
    ) {
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (clicked != true || ! (selected instanceof Actor)) return false ;
        final Actor subject = (Actor) selected ;
        
        if (caster == null || GameSettings.psyFree) {
          subject.gear.boostShields(5) ;
          return true ;
        }
        
        final float bonus = caster.traits.useLevel(TRANSDUCTION) / 2 ;
        subject.gear.boostShields(5 + bonus) ;
        caster.health.adjustPsy(-1.5f) ;
        return true ;
      }
    },
    
    
    //
    //  Stabilises the given subject and suspends all metabolic function,
    //  slowing the effects of bleeding, disease and intoxicants.
    SUSPENSION = new Power(
      "Suspension", MAINTAINED, "power_suspension.gif",
      "Suspends metabolic function in subject.\n(Can be used to arrest "+
      "injury or incapacitate foes.)"
    ) {
      public boolean finishedWith(
        Actor caster, String option,
        Target selected, boolean clicked
      ) {
        if (clicked != true || ! (selected instanceof Actor)) return true ;
        final Actor subject = (Actor) selected ;
        
        if (caster == null || GameSettings.psyFree) {
          if (! subject.health.isState(ActorHealth.STATE_SUSPEND)) {
            subject.health.setState(ActorHealth.STATE_SUSPEND) ;
          }
          return true ;
        }

        final float bonus = caster.traits.useLevel(METABOLISM) / 2 ;
        caster.health.adjustPsy(-0.5f / (1 + bonus)) ;
        //
        //  TODO:  Should be resistable...
        if (! subject.health.isState(ActorHealth.STATE_SUSPEND)) {
          subject.health.setState(ActorHealth.STATE_SUSPEND) ;
        }
        return true ;
      }
    },
    
    
    //
    //  Substantially boosts the subject's reflexes and coordination, allowing
    //  for extraordinary skill in combat and athletics.
    //
    //  One target, type actor, condition effect.
    KINESTHESIA = new Power(
      "Kinesthesia", MAINTAINED, "power_kinesthesia.gif",
      "Augments hand-eye coordination and reflex response.\n(Boosts most "+
      "combat and acrobatic skills.)"
    ) {
      /*
      void applyEffect(Target target, Actor caster) {
        final Actor subject = (Actor) target ;
        final float bonus = caster.traits.useLevel(SYNESTHESIA) / 2 ;
        subject.traits.incBonus(REFLEX, 5 + bonus) ;
        for (Skill s : ALL_SKILLS) if (s.parent == REFLEX) {
          subject.traits.incBonus(s, (5 + bonus) / 2) ;
        }
        caster.health.adjustPsy(-0.2f) ;
      }
      //*/
    },
    
    
    //
    //  Impels the subject to perform a single task, willingly or otherwise-
    //  however, this will not endear you to an unwilling subject.
    //
    //  Two targets, type actor and target, multiple options, further screening.
    VOICE_OF_COMMAND = new Power(
      "Voice of Command", PLAYER_ONLY, "power_voice_of_command.gif",
      "Employs mnemonic triggering to incite specific behavioural response."+
      "\n(Compels subject to fight, flee, help or grab a specified target.)"
    ) {
      /*
      void applyEffect(Target target, Actor caster) {
        final Actor subject = (Actor) target ;
        final float bonus = caster.traits.useLevel(SUGGESTION) / 2 ;
        I.complain("NOT IMPLEMENTED YET!") ;
        
        //
        //  TODO:  Create a panel for commands, and pass along the subject and
        //  power bonus.  Then you select a basic activity- flee, fight, help,
        //  grab or stop.  (Break into sub-commands?  ...Maybe.)
      }
      //*/
    },
    
    
    BASIC_POWERS[] = {
        WALK_THE_PATH, DENY_THE_VISION,
        TIME_DILATION, REMOTE_VIEWING,
        FORCEFIELD, SUSPENSION
        /*
        TELEKINESIS, FORCEFIELD,
        SUSPENSION,
        KINESTHESIA,
        VOICE_OF_COMMAND
        //*/
    }
  ;
}








/*
protected Class[] targetTypes() { return null ; }

public boolean acceptArgs(Target... args) {
  final Class TT[] = targetTypes() ;
  if (args.length > TT.length) return false ;
  for (int i = 0 ; i < args.length ; i++) {
    final Target t = args[i] ;
    if (t == null) return false ;
    if (! TT[i].isAssignableFrom(t.getClass())) return false ;
  }
  return true ;
}


public String[] options() { return null ; }


public boolean finishedWith(Actor caster, String option, Target args[]) {
  return false ;
}
//*/


/*
public boolean hasProperty(int property) {
  return (properties & property) == property ;
}

//
//  ...I don't think the number of targets matters.  But I might want to do
//  things like screen for allegiance of the subject.

public boolean acceptsTarget(
  Target nextTarget, boolean clicked, Target... args
) {
  return clicked && args.length == 0 && nextTarget instanceof Actor ;
}


public boolean updateWith(Actor caster, Target... args) {
  return true ;
}


public boolean finishedWith(Actor caster, Target... args) {
  applyEffect(args[0], caster) ;
  return true ;
}


public String[] options() {
  return null ;
}


void applyEffect (Target target, Actor caster) {}
void cancelEffect(Target target, Actor caster) {}
//*/

