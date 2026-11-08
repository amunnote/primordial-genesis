package local.primordial.smoke;

import local.primordial.fauna.FaunaContent;
import local.primordial.fauna.FlameCrystalItem;
import local.primordial.fauna.ThrownFlameCrystal;
import local.primordial.fauna.WhistlingArrow;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The guardians' creatures and what comes of them (FaunaContent). */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class FaunaGameTests {
    @GameTest(template="empty") public static void creaturesLiveAndLeaveTheirSpoils(GameTestHelper helper) {
        var level=helper.getLevel();
        for(var type:java.util.List.<EntityType<?>>of(FaunaContent.BIFANG.get(),FaunaContent.FIREFLY.get(),FaunaContent.SPARROW.get())){
            var e=helper.spawn(type,2,2,2);helper.assertTrue(e.isAlive(),"it lives: "+type);e.discard();
        }
        helper.assertTrue(FaunaContent.BIFANG.get().fireImmune(),"fire does not touch the Bifang");
        for(var id:new String[]{"entities/bifang","entities/golden_bell_sparrow","entities/netherlamp_fish","blocks/thunder_firefly_jar","blocks/netherlamp"})
            helper.assertTrue(level.getServer().getLootData().getLootTable(new ResourceLocation("primordial",id))!=LootTable.EMPTY,"a loot table: "+id);
        for(var id:new String[]{"whistling_arrow","netherlamp"})
            helper.assertTrue(level.getRecipeManager().byKey(new ResourceLocation("primordial",id)).isPresent(),"a recipe: "+id);
        helper.assertTrue(new ItemStack(FaunaContent.WHISTLING_ARROW_ITEM.get()).is(net.minecraft.tags.ItemTags.ARROWS),"the whistling arrow is an arrow to bows");
        helper.succeed();
    }

    @GameTest(template="empty") public static void lampfishTakenInABucket(GameTestHelper helper) {
        for(int x=1;x<=3;x++)for(int z=1;z<=3;z++)for(int y=1;y<=3;y++)helper.setBlock(new BlockPos(x,y,z),Blocks.WATER);
        var fish=helper.spawn(FaunaContent.LAMPFISH.get(),2,2,2);
        helper.assertTrue(fish.getBucketItemStack().is(FaunaContent.LAMPFISH_BUCKET.get()),"a bucket of netherlamp fish");
        // a real server player: taking a fish in a bucket fires an advancement trigger
        var player=new net.minecraft.server.level.ServerPlayer(helper.getLevel().getServer(),helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"bucket_test"));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
        fish.interact(player,InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().is(FaunaContent.LAMPFISH_BUCKET.get())&&fish.isRemoved(),"a water bucket takes it, got "+player.getMainHandItem());
        helper.succeed();
    }

    @GameTest(template="empty") public static void fireflyBottledSetDownAndLetGo(GameTestHelper helper) {
        var level=helper.getLevel();
        var firefly=helper.spawn(FaunaContent.FIREFLY.get(),2,3,2);
        var player=helper.makeMockSurvivalPlayer();player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE));
        firefly.interact(player,InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().is(FaunaContent.FIREFLY_BOTTLE.get())&&firefly.isRemoved(),"a glass bottle takes it, got "+player.getMainHandItem());
        // set down on a block: a lamp
        var floor=new BlockPos(1,1,1);helper.setBlock(floor,Blocks.STONE);var abs=helper.absolutePos(floor);
        player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(abs).add(0,0.5,0),Direction.UP,abs,false)));
        helper.assertTrue(level.getBlockState(abs.above()).is(FaunaContent.FIREFLY_JAR.get()),"a lamp set down, got "+level.getBlockState(abs.above()));
        helper.assertTrue(level.getBlockState(abs.above()).getLightEmission(level,abs.above())==12,"that gives light");
        // sneaking: let go, the bottle back
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FaunaContent.FIREFLY_BOTTLE.get()));player.setShiftKeyDown(true);
        int before=level.getEntitiesOfClass(local.primordial.fauna.ThunderFirefly.class,player.getBoundingBox().inflate(8)).size();
        player.getMainHandItem().use(level,player,InteractionHand.MAIN_HAND);
        helper.assertTrue(level.getEntitiesOfClass(local.primordial.fauna.ThunderFirefly.class,player.getBoundingBox().inflate(8)).size()==before+1,"the firefly let go");
        helper.assertTrue(player.getInventory().countItem(Items.GLASS_BOTTLE)==1,"the bottle back");
        helper.succeed();
    }

    @GameTest(template="empty") public static void flameCrystalBurnsLongAndBursts(GameTestHelper helper) {
        helper.assertTrue(net.minecraftforge.common.ForgeHooks.getBurnTime(new ItemStack(FaunaContent.FLAME_CRYSTAL.get()),null)==FlameCrystalItem.BURN_TICKS,"twice a lava bucket's burning");
        var zombie=helper.spawn(EntityType.ZOMBIE,2,2,2);local.primordial.affix.AffixState.suppress(zombie);
        float before=zombie.getHealth();
        var marker=helper.spawn(EntityType.ARMOR_STAND,2,2,3);
        ThrownFlameCrystal.burst(helper.getLevel(),marker,null);
        helper.assertTrue(zombie.getHealth()<before&&zombie.isOnFire(),"what stands near is burned and set alight");
        zombie.discard();marker.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void whistlingArrowSeeksAFoeAhead(GameTestHelper helper) {
        var level=helper.getLevel();
        var zombie=helper.spawn(EntityType.ZOMBIE,2,2,8);local.primordial.affix.AffixState.suppress(zombie);
        var cow=helper.spawn(EntityType.COW,2,2,4);
        var arrow=new WhistlingArrow(FaunaContent.WHISTLING_ARROW.get(),level);
        var from=helper.absoluteVec(new Vec3(3,3,1));arrow.setPos(from.x,from.y,from.z);arrow.setDeltaMovement(0,0,1.5);
        helper.assertTrue(arrow.seek()==zombie,"the hostile ahead, not the cow, got "+arrow.seek());
        arrow.setDeltaMovement(0,0,-1.5);
        helper.assertTrue(arrow.seek()==null,"nothing behind it");
        zombie.discard();cow.discard();
        helper.succeed();
    }
}
