package local.primordial.smoke;

import java.util.EnumSet;
import local.primordial.Primordial;
import local.primordial.affix.AffixEvents;
import local.primordial.affix.AffixState;
import local.primordial.affix.AffixStatuses;
import local.primordial.affix.MobAffix;
import local.primordial.sage.SageHeart;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class FixRoundGameTests {
    @GameTest(template="empty") public static void sageCurseHalvesPotionsOnceAndSparesStatuses(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new).setEquippedCurio(Primordial.SAGE_SLOT,0,new ItemStack(Primordial.SAGE_HEART.get()));
        TestSlots.tick(player);
        helper.assertTrue(SageHeart.active(player),"wearing Sage's Gift");
        // a 4 minute potion becomes 2 minutes on the server; a harmful one doubles
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,4800));
        helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SPEED).getDuration()==2400,"4 min Speed lasts 2 min, got "+player.getEffect(MobEffects.MOVEMENT_SPEED).getDuration());
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,600));
        helper.assertTrue(player.getEffect(MobEffects.WEAKNESS).getDuration()==1200,"30 s Weakness lasts 60 s");
        // what the server sends to the client is that same instance; the client must not rescale it again
        var sent=player.getEffect(MobEffects.MOVEMENT_SPEED);
        helper.assertTrue(SageHeart.effect(player,sent).getDuration()==1200,"(server side would rescale an incoming effect)");
        // the independent statuses are not potion effects: the curse leaves their length alone
        AffixStatuses.applyMalice(player);AffixStatuses.applyBloodPact(player);AffixStatuses.applyBefuddled(player);AffixStatuses.applyFeeble(player);
        helper.assertTrue(AffixStatuses.malice(player)==AffixStatuses.MALICE_TICKS&&AffixStatuses.bloodPact(player)==AffixStatuses.BLOOD_PACT_TICKS
            &&AffixStatuses.befuddled(player)==AffixStatuses.BEFUDDLED_TICKS&&AffixStatuses.feeble(player)==AffixStatuses.FEEBLE_TICKS,"statuses keep their full length");
        helper.succeed();
    }

    @GameTest(template="empty") public static void alchemistGiftTeachesBelowLevelTen(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        var gift=new ItemStack(Primordial.ALCHEMIST_GIFT.get());var item=(local.primordial.item.AlchemistGiftItem)gift.getItem();
        var slot=new top.theillusivec4.curios.api.SlotContext(Primordial.ALCHEMICAL_ACCESSORY_SLOT,player,0,false,true);
        var second=new top.theillusivec4.curios.api.SlotContext(Primordial.ALCHEMICAL_ACCESSORY_SLOT,player,1,false,true);
        player.tickCount=39;item.curioTick(slot,gift);
        helper.assertTrue(player.totalExperience==0,"nothing between beats");
        player.tickCount=40;item.curioTick(slot,gift);item.curioTick(second,gift);
        helper.assertTrue(player.totalExperience==1,"1 point every 2 s, and two gifts teach no faster: "+player.totalExperience);
        player.experienceLevel=10;int before=player.totalExperience;
        player.tickCount=80;item.curioTick(slot,gift);
        helper.assertTrue(player.totalExperience==before,"nothing from level 10 on");
        var cosmetic=new top.theillusivec4.curios.api.SlotContext(Primordial.ALCHEMICAL_ACCESSORY_SLOT,player,0,true,true);
        player.experienceLevel=0;player.tickCount=120;item.curioTick(cosmetic,gift);
        helper.assertTrue(player.totalExperience==before,"not from a cosmetic slot");
        helper.succeed();
    }

    @GameTest(template="empty") public static void shieldsBlockMagicAffixAttacks(GameTestHelper helper) throws Exception {
        var player=helper.makeMockSurvivalPlayer();
        var at=helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5,2,1.5));player.moveTo(at.x,at.y,at.z,0,0);   // facing +z
        var mage=helper.spawn(EntityType.ZOMBIE,1,2,3);
        AffixState.assign(mage,30,EnumSet.of(MobAffix.MAGIC),null);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(net.minecraft.world.item.Items.SHIELD));
        float health=player.getHealth();
        // not blocking: the magic hit goes through
        player.hurt(mage.damageSources().mobAttack(mage),4);
        helper.assertTrue(player.getHealth()<health,"an unblocked magic hit lands");
        player.setHealth(player.getMaxHealth());player.invulnerableTime=0;health=player.getHealth();
        // raise the shield (held for more than the 5 tick wind-up)
        player.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
        var remaining=net.minecraft.world.entity.LivingEntity.class.getDeclaredField("useItemRemaining");remaining.setAccessible(true);
        remaining.setInt(player,player.getUseItem().getUseDuration()-10);
        helper.assertTrue(player.isBlocking(),"shield raised");
        player.hurt(mage.damageSources().mobAttack(mage),4);
        helper.assertTrue(player.getHealth()==health,"the shield stops a Magic affix hit, health "+player.getHealth()+" of "+health);
        player.stopUsingItem();mage.discard();helper.succeed();
    }

    @GameTest(template="empty") public static void splitSlimesKeepTheirParentsAffixes(GameTestHelper helper) {
        var level=helper.getLevel();
        for(var type:new EntityType<?>[]{EntityType.SLIME,EntityType.MAGMA_CUBE}){
            var parent=(Slime)type.create(level);parent.setSize(4,true);
            parent.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2,2,2)));level.addFreshEntity(parent);
            var affixes=EnumSet.of(MobAffix.VOID,MobAffix.REGENERATION);
            AffixState.assign(parent,20,affixes,null);
            parent.setHealth(0);parent.remove(Entity.RemovalReason.KILLED);
            var pieces=level.getEntitiesOfClass(Slime.class,new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8),s->s!=parent&&s.isAlive()&&s.getType()==type);
            helper.assertTrue(pieces.size()>=2,"split into pieces, got "+pieces.size());
            for(var piece:pieces){
                var state=AffixState.get(piece);
                helper.assertTrue(state!=null&&state.affixes.equals(affixes),type+": the piece keeps exactly its parent's affixes, got "+(state==null?null:state.affixes));
                helper.assertTrue(state.tag.getBoolean("clone"),"pieces are split copies (no drops)");
                var name=piece.getCustomName();
                long tags=name==null?0:name.getSiblings().stream().filter(c->c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("affix.primordial.tag")).count();
                helper.assertTrue(tags==2,"the name shows its 2 affixes once, got "+tags+" in "+name);
                helper.assertTrue(Math.abs(piece.getHealth()-piece.getMaxHealth())<0.01,"pieces start at full health");
                piece.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template="empty") public static void thunderOnlyAtTheHuntedPlayerAndRarely(GameTestHelper helper) {
        var level=helper.getLevel();
        var player=helper.makeMockSurvivalPlayer();var at=helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5,2,6.5));player.setPos(at.x,at.y,at.z);
        var mob=helper.spawn(EntityType.ZOMBIE,1,2,1);mob.setNoAi(true);
        var state=AffixState.assign(mob,20,EnumSet.of(MobAffix.THUNDER),null);
        long now=level.getGameTime();
        java.util.function.IntSupplier bolts=()->level.getEntitiesOfClass(LightningBolt.class,new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(20)).size();
        int before=bolts.getAsInt();
        AffixEvents.thunder(state,mob,null,false,now);
        AffixEvents.thunder(state,mob,player,false,now+1000);
        helper.assertTrue(bolts.getAsInt()==before,"no bolts without hunting a player");
        AffixEvents.thunder(state,mob,player,true,now);
        AffixEvents.thunder(state,mob,player,true,now+60);
        helper.assertTrue(bolts.getAsInt()==before,"no bolt in the first 5 s");
        AffixEvents.thunder(state,mob,player,true,now+100);
        helper.assertTrue(bolts.getAsInt()==before+1,"first bolt after 5 s");
        AffixEvents.thunder(state,mob,player,true,now+200);AffixEvents.thunder(state,mob,player,true,now+399);
        helper.assertTrue(bolts.getAsInt()==before+1,"then nothing for 15 s");
        AffixEvents.thunder(state,mob,player,true,now+400);
        helper.assertTrue(bolts.getAsInt()==before+2,"the next one 15 s later");
        // out of range: no bolt
        player.setPos(at.x,at.y,at.z+40);AffixEvents.thunder(state,mob,player,true,now+800);
        helper.assertTrue(bolts.getAsInt()==before+2,"not beyond 32 blocks");
        level.getEntitiesOfClass(LightningBolt.class,new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(20)).forEach(Entity::discard);
        mob.discard();helper.succeed();
    }
}
