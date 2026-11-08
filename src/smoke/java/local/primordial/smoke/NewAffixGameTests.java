package local.primordial.smoke;

import java.util.ArrayList;
import java.util.EnumSet;
import local.primordial.affix.AffixCombat;
import local.primordial.affix.AffixEligibility;
import local.primordial.affix.AffixExtras;
import local.primordial.affix.AffixGrowth;
import local.primordial.affix.AffixMaterials;
import local.primordial.affix.AffixState;
import local.primordial.affix.AffixStatuses;
import local.primordial.affix.MobAffix;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class NewAffixGameTests {
    private static Zombie zombie(GameTestHelper helper,int x,int z,MobAffix... affixes) {
        var zombie=helper.spawn(EntityType.ZOMBIE,x,2,z);
        var set=EnumSet.noneOf(MobAffix.class);for(var a:affixes)set.add(a);
        AffixState.assign(zombie,100,set,null);
        return zombie;
    }
    private static void cleanup(GameTestHelper helper) {
        for(var e:helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Entity.class,new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40),
            e->!(e instanceof Player)))e.discard();
    }

    @GameTest(template="empty") public static void shatterBreaksTheOffhandShield(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));
        var breaker=zombie(helper,1,1,MobAffix.SHATTER);
        int hits=0;
        while(!player.getOffhandItem().isEmpty()&&hits<200){
            MinecraftForge.EVENT_BUS.post(new LivingAttackEvent(player,breaker.damageSources().mobAttack(breaker),2));hits++;}
        helper.assertTrue(player.getOffhandItem().isEmpty(),"level 100: 30% per hit, shield still there after 200 hits");
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));
        var plain=zombie(helper,2,1,MobAffix.VOID);
        for(int i=0;i<100;i++)MinecraftForge.EVENT_BUS.post(new LivingAttackEvent(player,plain.damageSources().mobAttack(plain),2));
        helper.assertTrue(!player.getOffhandItem().isEmpty(),"without Shatter the shield survives");
        cleanup(helper);helper.succeed();
    }

    @GameTest(template="empty") public static void undyingSavesAlliesOnce(GameTestHelper helper) {
        var level=helper.getLevel();
        var bearer=zombie(helper,1,1,MobAffix.UNDYING);
        var ally=helper.spawn(EntityType.ZOMBIE,3,2,1);
        AffixExtras.auras(AffixState.get(bearer),level);
        helper.assertTrue(bearer.hasEffect(MobEffects.GLOWING),"the bearer glows");
        helper.assertTrue(AffixStatuses.undying(ally)>0&&AffixStatuses.undying(bearer)==0,"allies get Undying, the bearer does not");
        ally.hurt(ally.damageSources().generic(),1000);
        helper.assertTrue(ally.isAlive()&&ally.getHealth()>=1&&AffixStatuses.undyingUsed(ally),"the ally comes back like a totem");
        helper.assertTrue(ally.hasEffect(MobEffects.ABSORPTION)&&ally.hasEffect(MobEffects.FIRE_RESISTANCE),"with the totem's effects");
        helper.assertTrue(Math.abs(ally.getHealth()-ally.getMaxHealth()/3)<0.01,"undead come back with a third of their health, got "+ally.getHealth());
        AffixExtras.auras(AffixState.get(bearer),level);
        helper.assertTrue(AffixStatuses.undying(ally)==0,"only once per mob");
        ally.hurt(ally.damageSources().generic(),1000);
        ally.invulnerableTime=0;ally.hurt(ally.damageSources().generic(),1000);
        helper.assertTrue(!ally.isAlive(),"the second death sticks");
        cleanup(helper);helper.succeed();
    }

    @GameTest(template="empty") public static void bloodPactCallsMonsters(GameTestHelper helper) {
        var server=helper.getLevel().getServer();var difficulty=helper.getLevel().getDifficulty();
        server.setDifficulty(Difficulty.NORMAL,true);
        var player=helper.makeMockSurvivalPlayer();
        var at=helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5,61,1.5));player.setPos(at.x,at.y,at.z);   // high above the other tests
        var biter=zombie(helper,1,1,MobAffix.BLOOD_PACT);
        AffixCombat.landed(AffixState.get(biter),null,player,biter.damageSources().mobAttack(biter),4);
        helper.assertTrue(AffixStatuses.bloodPact(player)==AffixStatuses.BLOOD_PACT_TICKS,"a hit gives 60 s of Blood Pact");
        // a floor for the called monsters to stand on
        var origin=helper.absolutePos(new BlockPos(0,61,0));
        // the test world is solid rock up here: a stone floor with open space above it
        for(int x=-18;x<=18;x++)for(int z=-18;z<=18;z++){helper.getLevel().setBlockAndUpdate(origin.offset(x,-1,z).offset(1,0,1),Blocks.STONE.defaultBlockState());
            for(int y=0;y<4;y++)helper.getLevel().setBlockAndUpdate(origin.offset(x,y,z).offset(1,0,1),Blocks.AIR.defaultBlockState());}
        var called=new ArrayList<net.minecraft.world.entity.Mob>();
        for(int i=0;i<10;i++){var mob=AffixExtras.callMonster(player);if(mob!=null)called.add(mob);}
        helper.assertTrue(called.size()==AffixExtras.BLOOD_PACT_MAX,"up to 6 monsters at once, got "+called.size());
        for(var mob:called){helper.assertTrue(mob.getTarget()==player,"called monsters hunt the player");
            double d=Math.sqrt(mob.distanceToSqr(player));helper.assertTrue(d>=6&&d<=18,"called 8-16 blocks away, got "+d);}
        for(int x=-18;x<=18;x++)for(int z=-18;z<=18;z++)helper.getLevel().setBlockAndUpdate(origin.offset(x,-1,z).offset(1,0,1),Blocks.AIR.defaultBlockState());
        server.setDifficulty(difficulty,true);
        cleanup(helper);helper.succeed();
    }

    @GameTest(template="empty") public static void growthDoublesSizeAttackAndHealth(GameTestHelper helper) {
        var mob=zombie(helper,1,1,MobAffix.GROWTH);
        float health=mob.getMaxHealth(),width=mob.getBbWidth();double attack=mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for(int i=0;i<200;i++)AffixGrowth.step(AffixState.get(mob));
        helper.assertTrue(Math.abs(AffixGrowth.growth(mob)-2)<1e-4,"stops at double, got "+AffixGrowth.growth(mob));
        helper.assertTrue(Math.abs(mob.getMaxHealth()-health*2)<0.01,"health doubled");
        helper.assertTrue(Math.abs(mob.getAttributeValue(Attributes.ATTACK_DAMAGE)-attack*2)<0.01,"attack doubled");
        helper.assertTrue(Math.abs(mob.getBbWidth()-width*2)<0.01,"hitbox doubled, got "+mob.getBbWidth());
        helper.assertTrue(Math.abs(mob.getHealth()-mob.getMaxHealth())<0.01,"kept full health while growing");
        AffixState.assign(mob,100,EnumSet.of(MobAffix.VOID),null);AffixGrowth.step(AffixState.get(mob));
        helper.assertTrue(AffixGrowth.growth(mob)==1&&Math.abs(mob.getBbWidth()-width)<0.01,"losing Growth shrinks it back");
        cleanup(helper);helper.succeed();
    }

    @GameTest(template="empty") public static void enlightenedMobsDigAndBuild(GameTestHelper helper) {
        var level=helper.getLevel();
        var sage=zombie(helper,1,1,MobAffix.ENLIGHTENMENT);
        var near=helper.spawn(EntityType.SKELETON,4,2,1);
        AffixExtras.auras(AffixState.get(sage),level);
        helper.assertTrue(AffixStatuses.enlightened(sage)>0&&AffixStatuses.enlightened(near)>0,"the bearer and nearby monsters are enlightened");
        var wall=helper.absolutePos(new BlockPos(2,2,2));
        level.setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
        for(int i=0;i<30&&!level.getBlockState(wall).isAir();i++)AffixExtras.dig(sage,level,wall);
        helper.assertTrue(level.getBlockState(wall).isAir(),"digs through stone");
        level.setBlockAndUpdate(wall,Blocks.OBSIDIAN.defaultBlockState());
        for(int i=0;i<40;i++)AffixExtras.dig(sage,level,wall);
        helper.assertTrue(level.getBlockState(wall).is(Blocks.OBSIDIAN),"never breaks obsidian");
        level.setBlockAndUpdate(wall,Blocks.AIR.defaultBlockState());
        AffixExtras.place(level,wall);
        helper.assertTrue(level.getBlockState(wall).is(local.primordial.Primordial.ENLIGHTENED_STONE.get()),"builds with enlightened stone");
        helper.assertTrue(level.getBlockTicks().hasScheduledTick(wall,local.primordial.Primordial.ENLIGHTENED_STONE.get()),"which is set to crumble away");
        local.primordial.Primordial.ENLIGHTENED_STONE.get().defaultBlockState().tick(level,wall,level.random);
        helper.assertTrue(level.getBlockState(wall).isAir(),"and when its time comes it is gone");
        level.setBlockAndUpdate(wall,Blocks.AIR.defaultBlockState());
        cleanup(helper);helper.succeed();
    }

    @GameTest(template="empty") public static void befuddlementShufflesControls(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        var trickster=zombie(helper,1,1,MobAffix.BEFUDDLEMENT);
        // being hit by it
        AffixCombat.landed(AffixState.get(trickster),null,player,trickster.damageSources().mobAttack(trickster),2);
        helper.assertTrue(AffixStatuses.befuddled(player)==AffixStatuses.BEFUDDLED_TICKS,"hit: befuddled for 15 s");
        int seed=AffixStatuses.befuddleSeed(player);
        // hitting it renews the time, the same shuffle stays
        AffixCombat.landed(null,AffixState.get(trickster),trickster,player.damageSources().playerAttack(player),2);
        helper.assertTrue(AffixStatuses.befuddleSeed(player)==seed,"renewing keeps the shuffle");
        for(int s=0;s<200;s++){int[] to=AffixStatuses.shuffledControls(s);boolean[] seen=new boolean[4];
            for(int i=0;i<4;i++){helper.assertTrue(to[i]!=i,"every direction moves");seen[to[i]]=true;}
            for(boolean b:seen)helper.assertTrue(b,"a permutation");}
        cleanup(helper);helper.succeed();
    }

    @GameTest(template="empty") public static void amplifyLendsAnAffix(GameTestHelper helper) {
        var level=helper.getLevel();
        var amp=zombie(helper,1,1,MobAffix.AMPLIFY);
        var ally=zombie(helper,4,1,MobAffix.VOID);
        float before=ally.getMaxHealth();
        AffixExtras.auras(AffixState.get(amp),level);
        var state=AffixState.get(ally);
        helper.assertTrue(state.affixes.size()==2&&state.temporary.size()==1&&!state.temporary.contains(MobAffix.AMPLIFY),"one lent affix, never Amplify");
        helper.assertTrue(ally.getMaxHealth()>before,"the lent affix counts for the stat bonus");
        helper.assertTrue(AffixState.get(amp).temporary.isEmpty(),"the bearer lends to others only");
        var lucky=AffixMaterialGameTests.player(helper,100,50);
        for(int i=0;i<50;i++){var drops=new ArrayList<ItemEntity>();AffixMaterials.drop(state,AffixMaterialGameTests.SURE,lucky,drops);
            helper.assertTrue(drops.size()==1&&drops.get(0).getItem().is(AffixMaterials.ITEMS.get(MobAffix.VOID).get()),"lent affixes drop no materials");}
        state.expireLent(AffixExtras.AMPLIFY_TICKS);
        var after=AffixState.get(ally);
        helper.assertTrue(after.affixes.size()==1&&after.temporary.isEmpty(),"the loan runs out");
        cleanup(helper);helper.succeed();
    }

    @GameTest(template="empty") public static void masteryShootsAndStrikes(GameTestHelper helper) {
        var level=helper.getLevel();
        helper.assertTrue(!AffixEligibility.usable(EntityType.GHAST.create(level),MobAffix.MASTERY),"a ghast cannot strike in melee");
        helper.assertTrue(AffixEligibility.usable(EntityType.ZOMBIE.create(level),MobAffix.MASTERY),"a zombie can shoot");
        var archer=zombie(helper,1,1,MobAffix.MASTERY);
        var target=helper.spawn(EntityType.VILLAGER,1,2,9);archer.setTarget(target);
        AffixExtras.mastery(AffixState.get(archer),level);
        var arrows=level.getEntitiesOfClass(AbstractArrow.class,new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(20),a->a.getOwner()==archer);
        helper.assertTrue(arrows.size()==1,"a melee mob fires an arrow at range, got "+arrows.size());
        AffixExtras.mastery(AffixState.get(archer),level);
        helper.assertTrue(level.getEntitiesOfClass(AbstractArrow.class,new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(20),a->a.getOwner()==archer).size()==1,"then waits 3 s");
        var skeleton=helper.spawn(EntityType.SKELETON,3,2,3);
        skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
        AffixState.assign(skeleton,100,EnumSet.of(MobAffix.MASTERY),null);
        var close=helper.spawn(EntityType.VILLAGER,3,2,4);skeleton.setTarget(close);
        float health=close.getHealth();
        AffixExtras.mastery(AffixState.get(skeleton),level);
        helper.assertTrue(close.getHealth()<health,"a shooter strikes up close");
        cleanup(helper);helper.succeed();
    }
}
