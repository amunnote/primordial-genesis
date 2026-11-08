package local.primordial.qinglong;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

/**
 * The ground of the four lands. These rules come before the overworld's own (see NoiseSurfaceMixin), and each holds
 * only in its own biome, so they shape the lands wherever GuardianAnchors lays them and nowhere else.
 */
public final class GuardianSurface {
    private static volatile SurfaceRules.RuleSource rules;
    private GuardianSurface() {}

    /** built the first time a world asks, when the blocks are long registered */
    public static SurfaceRules.RuleSource rules() {
        var made=rules;
        if(made==null)rules=made=build();
        return made;
    }

    private static SurfaceRules.RuleSource build() {
        var shell=local.primordial.xuanwu.XuanwuContent.ROCK.get().defaultBlockState();
        var bladestone=local.primordial.baihu.BaihuContent.BLADESTONE.get().defaultBlockState();
        return SurfaceRules.sequence(
            // the Black Tortoise's sea floor: shell rock in the shallows, gravel lower, deepslate in the deeps
            SurfaceRules.ifTrue(SurfaceRules.isBiome(local.primordial.xuanwu.XuanwuWorldgen.BIOME),SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(55),0),SurfaceRules.state(shell)),
                    SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(35),0),SurfaceRules.state(Blocks.GRAVEL.defaultBlockState())),
                    SurfaceRules.state(Blocks.DEEPSLATE.defaultBlockState()))),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR,SurfaceRules.state(shell)))),
            SurfaceRules.ifTrue(SurfaceRules.isBiome(local.primordial.zhuque.ZhuqueWorldgen.BIOME),SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.steep(),SurfaceRules.state(Blocks.RED_SANDSTONE.defaultBlockState())),
                    SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(120),0),SurfaceRules.state(Blocks.RED_SANDSTONE.defaultBlockState())),
                    SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(100),0),SurfaceRules.state(Blocks.ORANGE_TERRACOTTA.defaultBlockState())),
                    // red sand in drifts through sunset-coloured grass, where the fire flowers grow
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE,0.15),SurfaceRules.state(Blocks.RED_SAND.defaultBlockState())),
                    SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1,0),SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState())),
                    SurfaceRules.state(Blocks.DIRT.defaultBlockState()))),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR,SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE,0.15),SurfaceRules.state(Blocks.RED_SANDSTONE.defaultBlockState())),
                    SurfaceRules.state(Blocks.DIRT.defaultBlockState()))))),
            SurfaceRules.ifTrue(SurfaceRules.isBiome(local.primordial.baihu.BaihuWorldgen.BIOME),SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.steep(),SurfaceRules.state(bladestone)),
                    SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(140),0),SurfaceRules.state(Blocks.CALCITE.defaultBlockState())),
                    // bare tuff and gravel scars through pale, wind-cut grass
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE,0.2),SurfaceRules.state(Blocks.TUFF.defaultBlockState())),
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE,-1.0,-0.45),SurfaceRules.state(Blocks.GRAVEL.defaultBlockState())),
                    SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1,0),SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState())),
                    SurfaceRules.state(Blocks.DIRT.defaultBlockState()))),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR,SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE,0.2),SurfaceRules.state(Blocks.ANDESITE.defaultBlockState())),
                    SurfaceRules.state(Blocks.DIRT.defaultBlockState()))))),
            SurfaceRules.ifTrue(SurfaceRules.isBiome(QinglongWorldgen.BIOME),SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.steep(),SurfaceRules.state(Blocks.CALCITE.defaultBlockState())),
                    SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(140),0),SurfaceRules.state(Blocks.CALCITE.defaultBlockState())),
                    SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1,0),SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState())),
                    SurfaceRules.state(Blocks.DIRT.defaultBlockState()))),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR,SurfaceRules.state(Blocks.DIRT.defaultBlockState())))));
    }
}
