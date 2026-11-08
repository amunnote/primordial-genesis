package local.primordial.qinglong;

import local.primordial.Primordial;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * What each guardian's land does to those in it:
 * <ul>
 * <li>Azure Dragon (苍雷云林), living thunder: a player there who is not starving heals half a heart every ten seconds;
 *     in rain the storm walks the grove, lightning flashing down every half minute or so near each player.</li>
 * <li>White Tiger (肃金刃原), the killing autumn: every melee blow struck there, by anyone, is 15% heavier.</li>
 * <li>Vermilion Bird (赤霞梧桐原), the scorched field: a melee blow there sets the struck alight one time in seven,
 *     and no one there freezes.</li>
 * <li>Black Tortoise (玄潮甲海), the cold tide: under its water the breath runs out twice as fast, unless water
 *     breathing or a conduit holds it.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class GuardianLandEffects {
    // the numbers are in WorldConfig (primordial-world.toml)
    private GuardianLandEffects() {}

    static boolean in(LivingEntity entity,ResourceKey<Biome> biome) {return entity.level().getBiome(entity.blockPosition()).is(biome);}
    private static boolean melee(LivingHurtEvent event) {
        var source=event.getSource();return source.getEntity() instanceof LivingEntity&&source.getDirectEntity()==source.getEntity();
    }

    @SubscribeEvent public static void blows(LivingHurtEvent event) {
        var victim=event.getEntity();
        if(victim.level().isClientSide||!melee(event))return;
        if(in(victim,local.primordial.baihu.BaihuWorldgen.BIOME))event.setAmount(event.getAmount()*(float)(1+WorldConfig.get(WorldConfig.TIGER_MELEE_BONUS)));
        else if(in(victim,local.primordial.zhuque.ZhuqueWorldgen.BIOME)&&!victim.fireImmune()&&victim.getRandom().nextDouble()<WorldConfig.get(WorldConfig.BIRD_IGNITE_CHANCE))victim.setSecondsOnFire(3);
    }

    @SubscribeEvent public static void lands(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||!player.isAlive()||player.isSpectator())return;
        var level=player.serverLevel();
        // the cold tide: the breath goes twice as fast (the game takes one a tick; this takes another)
        if(player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)&&!player.hasEffect(MobEffects.WATER_BREATHING)&&!player.hasEffect(MobEffects.CONDUIT_POWER)
            &&!player.getAbilities().invulnerable&&player.getAirSupply()>0&&WorldConfig.get(WorldConfig.TORTOISE_EXTRA_AIR)>0&&in(player,local.primordial.xuanwu.XuanwuWorldgen.BIOME))
            player.setAirSupply(Math.max(0,player.getAirSupply()-WorldConfig.get(WorldConfig.TORTOISE_EXTRA_AIR)));
        if(player.tickCount%20!=0)return;
        if(in(player,local.primordial.zhuque.ZhuqueWorldgen.BIOME))player.setTicksFrozen(0);
        if(!in(player,QinglongWorldgen.BIOME))return;
        int heal=WorldConfig.get(WorldConfig.DRAGON_HEAL_SECONDS)*20;
        if(heal>0&&player.tickCount%heal==0&&player.getFoodData().getFoodLevel()>=6&&player.getHealth()<player.getMaxHealth())player.heal(1);
        if(level.isRaining()&&player.getRandom().nextInt(30)==0){
            double a=player.getRandom().nextDouble()*Math.PI*2,d=12+player.getRandom().nextDouble()*28;
            int x=(int)(player.getX()+Math.cos(a)*d),z=(int)(player.getZ()+Math.sin(a)*d);
            var at=new BlockPos(x,level.getHeight(Heightmap.Types.MOTION_BLOCKING,x,z),z);
            if(!level.getBiome(at).is(QinglongWorldgen.BIOME))return;
            var bolt=EntityType.LIGHTNING_BOLT.create(level);
            if(bolt!=null){bolt.moveTo(at.getX()+0.5,at.getY(),at.getZ()+0.5);bolt.setVisualOnly(true);level.addFreshEntity(bolt);
                level.playSound(null,at,SoundEvents.LIGHTNING_BOLT_THUNDER,SoundSource.WEATHER,6F,0.8F+player.getRandom().nextFloat()*0.2F);}
        }
    }
}
