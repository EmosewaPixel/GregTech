package gregtech.api.worldgen.config;

import com.google.gson.JsonObject;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.world.IBiome;
import gregtech.api.GTValues;
import gregtech.api.unification.material.type.DustMaterial.MatFlags;
import gregtech.api.unification.material.type.IngotMaterial;
import gregtech.api.unification.ore.StoneType;
import gregtech.api.unification.ore.StoneTypes;
import gregtech.api.worldgen.filler.BlockFiller;
import gregtech.api.worldgen.shape.ShapeGenerator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Optional.Method;
import org.apache.commons.lang3.ArrayUtils;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.function.Function;
import java.util.function.Predicate;

@ZenClass("mods.gregtech.ore.OreDepositDefinition")
@ZenRegister
public class OreDepositDefinition {

    public static final Function<Biome, Integer> NO_BIOME_INFLUENCE = biome -> 0;
    public static final Predicate<WorldProvider> PREDICATE_SURFACE_WORLD = WorldProvider::isSurfaceWorld;
    public static final Predicate<IBlockState> PREDICATE_STONE_TYPE = state -> StoneType.computeStoneType(state) != StoneTypes._NULL;

    private final String depositName;

    private int priority = 0;
    private int weight;
    private float density;
    private int[] heightLimit = new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE};
    private IngotMaterial surfaceStoneMaterial;

    private Function<Biome, Integer> biomeWeightModifier = NO_BIOME_INFLUENCE;
    private Predicate<WorldProvider> dimensionFilter = PREDICATE_SURFACE_WORLD;
    private Predicate<IBlockState> generationPredicate = PREDICATE_STONE_TYPE;

    private BlockFiller blockFiller;
    private ShapeGenerator shapeGenerator;

    public OreDepositDefinition(String depositName) {
        this.depositName = depositName;
    }

    public void initializeFromConfig(JsonObject configRoot) {
        if(configRoot.has("priority")) {
            this.priority = configRoot.get("priority").getAsInt();
        }
        this.weight = configRoot.get("weight").getAsInt();
        this.density = configRoot.get("density").getAsFloat();
        if(configRoot.has("min_height")) {
            this.heightLimit[0] = configRoot.get("min_height").getAsInt();
        }
        if(configRoot.has("max_height")) {
            this.heightLimit[1] = configRoot.get("max_height").getAsInt();
        }
        if(configRoot.has("biome_modifier")) {
            this.biomeWeightModifier = OreConfigUtils.createBiomeWeightModifier(configRoot.get("biome_modifier"));
        }
        if(configRoot.has("dimension_filter")) {
            this.dimensionFilter = OreConfigUtils.createWorldPredicate(configRoot.get("dimension_filter"));
        }
        if(configRoot.has("generation_predicate")) {
            this.generationPredicate = OreConfigUtils.createBlockStatePredicate(configRoot.get("generation_predicate"));
        }
        if(configRoot.has("surface_stone_material")) {
            this.surfaceStoneMaterial = (IngotMaterial) OreConfigUtils.getMaterialByName(configRoot.get("surface_stone_material").getAsString());
            if(!surfaceStoneMaterial.hasFlag(MatFlags.GENERATE_ORE)) {
                throw new IllegalArgumentException("Material " + surfaceStoneMaterial + " doesn't have surface rock variant");
            }
        }
        this.blockFiller = WorldGenRegistry.INSTANCE.createBlockFiller(configRoot.get("filler").getAsJsonObject());
        this.shapeGenerator = WorldGenRegistry.INSTANCE.createShapeGenerator(configRoot.get("generator").getAsJsonObject());
    }

    @ZenGetter("depositName")
    public String getDepositName() {
        return depositName;
    }

    @ZenGetter("priority")
    public int getPriority() {
        return priority;
    }

    @ZenGetter("weight")
    public int getWeight() {
        return weight;
    }

    @ZenGetter("density")
    public float getDensity() {
        return density;
    }

    @ZenGetter("surfaceRockMaterial")
    public IngotMaterial getSurfaceStoneMaterial() {
        return surfaceStoneMaterial;
    }

    @ZenMethod
    public boolean checkInHeightLimit(int yLevel) {
        return yLevel >= heightLimit[0] && yLevel <= heightLimit[1];
    }

    @ZenMethod
    public int[] getHeightLimit() {
        return heightLimit;
    }

    @ZenGetter("minimumHeight")
    public int getMinimumHeight() {
        return heightLimit[0];
    }

    @ZenGetter("maximumHeight")
    public int getMaximumHeight() {
        return heightLimit[1];
    }

    public Function<Biome, Integer> getBiomeWeightModifier() {
        return biomeWeightModifier;
    }

    public Predicate<WorldProvider> getDimensionFilter() {
        return dimensionFilter;
    }

    public Predicate<IBlockState> getGenerationPredicate() {
        return generationPredicate;
    }

    @ZenMethod("getBiomeWeightModifier")
    @Method(modid = GTValues.MODID_CT)
    public int ctGetBiomeWeightModifier(IBiome biome) {
        int biomeIndex = ArrayUtils.indexOf(CraftTweakerMC.biomes, biome);
        Biome mcBiome = Biome.REGISTRY.getObjectById(biomeIndex);
        return mcBiome == null ? 0 : getBiomeWeightModifier().apply(mcBiome);
    }

    @ZenMethod("checkDimension")
    @Method(modid = GTValues.MODID_CT)
    public boolean ctCheckDimension(int dimensionId) {
        WorldProvider worldProvider = DimensionManager.getProvider(dimensionId);
        return worldProvider != null && getDimensionFilter().test(worldProvider);
    }

    @ZenMethod("canGenerateIn")
    @Method(modid = GTValues.MODID_CT)
    public boolean ctCanGenerateIn(crafttweaker.api.block.IBlockState blockState) {
        IBlockState mcBlockState = CraftTweakerMC.getBlockState(blockState);
        return getGenerationPredicate().test(mcBlockState);
    }

    @ZenGetter("filter")
    public BlockFiller getBlockFiller() {
        return blockFiller;
    }

    @ZenGetter("shape")
    public ShapeGenerator getShapeGenerator() {
        return shapeGenerator;
    }
}
