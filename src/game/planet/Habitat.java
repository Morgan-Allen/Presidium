/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.planet ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.util.* ;






public class Habitat {
  
  
  private static Batch <Habitat> allHabs = new Batch <Habitat> () ;
  private static int nextID = 0 ;
  final public int ID = nextID++ ;
  
  
  final static String
    TERRAIN_PATH = "media/Terrain/" ;
  final static ImageModel
    DESERT_FLORA_MODELS[][] = ImageModel.fromTextureGrid(
      Habitat.class,
      Texture.loadTexture(TERRAIN_PATH+"desert_flora.png"),
      4, 4, 1.5f
    ),
    FOREST_FLORA_MODELS[][] = ImageModel.fromTextureGrid(
      Habitat.class,
      Texture.loadTexture(TERRAIN_PATH+"forest_flora.png"),
      4, 4, 1.5f
    ),
    WASTES_FLORA_MODELS[][] = ImageModel.fromTextureGrid(
      Habitat.class,
      Texture.loadTexture(TERRAIN_PATH+"wastes_flora.png"),
      4, 4, 1.5f
    ),
    PLANKTON_MODELS[][] = null,
    //ANNUALS_MODELS[][] = null,
    NO_FLORA[][] = null ;
  
  final private static String
    MOISTURE   = "moisture",
    INSOLATION = "insolation",
    MINERALS   = "minerals",
    IS_OCEAN   = "is ocean",
    IS_WASTE  = "is wastes" ;
  
  
  final public static Habitat
    //
    //  Ocean habitats, which occur at or below current sea levels.
    OCEAN = new Habitat("Ocean",
      Texture.loadTextures(
        TERRAIN_PATH+"ocean.gif",
        TERRAIN_PATH+"ocean.2.gif",
        TERRAIN_PATH+"ocean.3.gif"
      ), PLANKTON_MODELS,
      2, false, IS_OCEAN
    ),
    SHALLOWS = new Habitat("Shallows",
      Texture.loadTextures(
        TERRAIN_PATH+"shallows.gif",
        TERRAIN_PATH+"shallows.2.gif",
        TERRAIN_PATH+"shallows.3.gif"
      ), PLANKTON_MODELS,
      1, false, IS_OCEAN
    ),
    SHORELINE = new Habitat("Shore",
      "shoreline.png", NO_FLORA,
      0, true, IS_OCEAN
    ),
    OCEAN_HABITATS[] = { SHORELINE, SHALLOWS, OCEAN },
    //
    //  Forest habitats, which occur in equatorial regions with adequate rain-
    SWAMPLANDS = new Habitat("Swamplands",
      "swamplands_ground.gif", FOREST_FLORA_MODELS,
      2, true, MOISTURE, 9, INSOLATION, 6, MINERALS, 0
    ),
    RAIN_FOREST = new Habitat("Rain Forest",
      "rain_forest_ground.gif", FOREST_FLORA_MODELS,
      1, true, MOISTURE, 7, INSOLATION, 7, MINERALS, 2
    ),
    MEADOW = new Habitat("Meadow",
      "meadows_ground.gif", FOREST_FLORA_MODELS,
      0, true, MOISTURE, 6, INSOLATION, 5, MINERALS, 3
    ),
    FOREST_HABITATS[] = { MEADOW, RAIN_FOREST, SWAMPLANDS },
    //
    //  Desert habitats, which occur under hotter conditions-
    SAVANNAH = new Habitat("Savannah",
      "savannah_ground.gif", DESERT_FLORA_MODELS,
      2, true, MOISTURE, 5, INSOLATION, 7, MINERALS, 3
    ),
    BARRENS = new Habitat("Barrens",
      "barrens_ground.gif", DESERT_FLORA_MODELS,
      1, true, MOISTURE, 3, INSOLATION, 8, MINERALS, 6
    ),
    DESERT = new Habitat("Desert",
      "desert_ground.gif", NO_FLORA,
      0, true, MOISTURE, 1, INSOLATION, 9, MINERALS, 5
    ),
    DESERT_HABITATS[] = { DESERT, BARRENS, SAVANNAH },
    //
    //  Waste habitats, which have special rules governing their introduction,
    //  related to extreme temperature, slope, pollution or volcanism-
    MESA = new Habitat("Mesa",
      "mesa_ground.gif", NO_FLORA,
      -1, false, MOISTURE, 1, INSOLATION, 5, MINERALS, 5,
      IS_WASTE
    ),
    BLACK_WASTES = new Habitat("Mesa",
      "black_wastes_ground.gif", WASTES_FLORA_MODELS,
      -1, true, MOISTURE, 3, INSOLATION, 3, MINERALS, 7,
      IS_WASTE
    ),
    GEOTHERMAL = new Habitat("Mesa",
      "geothermal_ground.gif", WASTES_FLORA_MODELS,
      -1, true, MOISTURE, 5, INSOLATION, 7, MINERALS, 9,
      IS_WASTE
    ),
    //
    //  This is the gradient of habitats going from most to least insolation-
    INSOLATION_GRADIENT[] = {
      SWAMPLANDS,
      RAIN_FOREST,
      MEADOW,
      SAVANNAH,
      BARRENS,
      DESERT,
    } ;
  final public static Habitat
    ALL_HABITATS[] = (Habitat[]) allHabs.toArray(Habitat.class) ;
  final public static Texture
    BASE_TEXTURES[] = new Texture[ALL_HABITATS.length],
    ROAD_TEXTURE = Texture.loadTexture(TERRAIN_PATH+"road_tiles.gif") ;
  static {
    for (Habitat h : ALL_HABITATS) BASE_TEXTURES[h.ID] = h.baseTex ;
  }
  
  
  
  
  final public String name ;
  final public Texture animTex[], baseTex ;
  final ImageModel floraModels[][] ;
  final public boolean pathClear ;
  
  final int biosphere ;
  float moisture, insolation, minerals ;
  boolean isOcean, isWaste ;
  
  
  Habitat(
    String name, String texName, ImageModel fM[][],
    int biosphere, boolean pathClear, Object... traits
  ) {
    this(
      name, new Texture[] {Texture.loadTexture(TERRAIN_PATH+texName)}, fM,
      biosphere, pathClear, traits
    ) ;
  }
  
  Habitat(
    String name, Texture groundTex[], ImageModel fM[][],
    int biosphere, boolean pathClear, Object... traits
  ) {
    allHabs.add(this) ;
    this.name = name ;
    this.animTex = groundTex ;
    this.baseTex = animTex[0] ;
    this.floraModels = fM ;
    this.biosphere = biosphere ;
    this.pathClear = pathClear ;
    for (int i = 0 ; i < traits.length ; i++) {
      if (traits[i] == MOISTURE  ) moisture   = (Integer) traits[i + 1] ;
      if (traits[i] == MINERALS  ) minerals   = (Integer) traits[i + 1] ;
      if (traits[i] == INSOLATION) insolation = (Integer) traits[i + 1] ;
      if (traits[i] == IS_OCEAN) isOcean = true ;
      if (traits[i] == IS_WASTE) isWaste = true ;
    }
  }
  
  public float moisture() { return moisture ; }
  public float insolation() { return insolation ; }
  public float minerals() { return minerals ; }
  public boolean isOcean() { return isOcean ; }
  public boolean isWaste() { return isWaste ; }
  
}











