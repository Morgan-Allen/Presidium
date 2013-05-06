/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.actors.ActorConstants ;
import src.game.base.* ;



//  Include various service-types here?


public interface VenueConstants extends ActorConstants {
  
  
  final public static int
    COMMODITY      = 0,
    PROVISION      = 1,
    UNIQUE         = 2,
    DEVICE         = 3,
    OUTFIT         = 4,
    SERVICE        = 5 ;
  
  final static Class C = VenueConstants.class ;
  final public static Item.Type
    
    STARCHES = new Item.Type(C, COMMODITY, "Starches", 10 ),
    PROTEIN  = new Item.Type(C, COMMODITY, "Protein" , 20 ),
    GREENS   = new Item.Type(C, COMMODITY, "Greens"  , 30 ),
    
    METALS   = new Item.Type(C, COMMODITY, "Metals"  , 15 ),
    CARBONS  = new Item.Type(C, COMMODITY, "Carbons" , 35 ),
    ISOTOPES = new Item.Type(C, COMMODITY, "Isotopes", 60 ),
    
    PARTS    = new Item.Type(C, COMMODITY, "Parts"   , 50 ),
    PLASTICS = new Item.Type(C, COMMODITY, "Plastics", 75 ),
    SOMA     = new Item.Type(C, COMMODITY, "Soma"    , 100),
    
    INSCRIPTION = new Item.Type(C, COMMODITY, "Inscription", 140),
    PRESSFEED   = new Item.Type(C, COMMODITY, "Pressfeed"  , 160),
    MEDICINE    = new Item.Type(C, COMMODITY, "Medicine"   , 200),

    /*
    SPYCE_A = new Item.Type(C, COMMODITY, "Spyce A (Tinerazine)", 400),
    SPYCE_B = new Item.Type(C, COMMODITY, "Spyce B (Halebdynum)", 400),
    SPYCE_C = new Item.Type(C, COMMODITY, "Spyce C (Natrizoral)", 400),
    //*/
    
    WATER        = new Item.Type(C, PROVISION, "Water"       , 0),
    LIFE_SUPPORT = new Item.Type(C, PROVISION, "Life Support", 0),
    POWER        = new Item.Type(C, PROVISION, "Power"       , 0),
    
    //  Plants (young/mature- species?  Flora or animal remains?  Dead/alive?)
    //  Housing.
    
    
    /*
    OUTFITS  = new Item.Type(C, UNIQUE, "Outfits", 0),
    DEVICES  = new Item.Type(C, UNIQUE, "Devices", 0),
    ARMOURS  = new Item.Type(C, UNIQUE, "Armours", 0),
    
    GENE_SAMPLE = new Item.Type(C, UNIQUE, "Gene Sample", 50 ),
    NEURAL_SCAN = new Item.Type(C, UNIQUE, "Neural Scan", 500),
    TROPHY      = new Item.Type(C, UNIQUE, "Trophy"     , 250)
    //*/
    ALL_ITEM_TYPES[] = Item.Type.allTypes() ;
  
  
  
  final public static Object TO = new Object() ;
  
  final public static Conversion
    
    METALS_TO_PARTS = new Conversion(
      2, METALS, TO, 1, PARTS,
      Artificer.class, TRICKY_DC, ASSEMBLY, SIMPLE_DC, CHEMISTRY
    ),
    
    PITCHES_TO_PLASTICS = new Conversion(
      2, CARBONS, TO, 1, PLASTICS,
      Fabricator.class, TRICKY_DC, CHEMISTRY, SIMPLE_DC, GRAPHIC_MEDIA
    ),
    
    NONE_TO_SOMA = new Conversion(
      TO, 1, SOMA,
      CultureVats.class, ROUTINE_DC, CHEMISTRY, ROUTINE_DC, PHARMACY
    ) ;
}














