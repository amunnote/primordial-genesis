package local.primordial.effect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import local.primordial.Primordial;
import local.primordial.affix.AffixCombat;
import local.primordial.affix.AffixEvents;
import local.primordial.affix.AffixState;
import local.primordial.affix.BossAffix;
import local.primordial.affix.Imbuing;
import local.primordial.affix.MobAffix;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Armor imbues: stacking powers and an independent golden blood layer. */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class ArmorImbueEvents {
    static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    static final String COOLDOWN="primordial:armor_cooldown_",SUMMONED="primordial:armor_summoned_until";
    public static final String GOLDEN_BLOOD="primordial:golden_blood";
    private static final String GOLDEN_HIT="primordial:golden_blood_hit";
    public static float goldenCapacity(Player player) {
        float max=player.getMaxHealth();
        return Math.min(max*1.2F,pieces(player,MobAffix.GROWTH)*(4+max*0.05F)+pieces(player,BossAffix.GLUTTONY)*(2+max*0.02F));
    }
    public static float goldenBlood(Player player) {return Math.max(0,Math.min(goldenCapacity(player),player.getPersistentData().getFloat(GOLDEN_BLOOD)));}
    private static void setGoldenBlood(Player player,float value) {
        float blood=Math.max(0,Math.min(goldenCapacity(player),value));
        float previous=player.getPersistentData().getFloat(GOLDEN_BLOOD);
        player.getPersistentData().putFloat(GOLDEN_BLOOD,blood);
        if((previous!=blood||player.tickCount%20==0)&&player instanceof net.minecraft.server.level.ServerPlayer server)
            local.primordial.network.MenuDropNetwork.syncGoldenBlood(server,blood);
    }
    /** more golden blood from elsewhere (the bond 长生): held to the same capacity as all of it */
    public static void addGoldenBlood(Player player,float amount) {if(amount>0)setGoldenBlood(player,goldenBlood(player)+amount);}
    private ArmorImbueEvents() {}

    // ------------------------------------------------------------------ what is worn
    /** how many worn pieces of armor carry this affix */
    public static int pieces(LivingEntity entity,String affix) {
        if(!(entity instanceof Player player))return 0;
        int n=0;
        for(var slot:SLOTS){var stack=player.getItemBySlot(slot);if(Imbuing.isArmor(stack)&&Imbuing.imbues(stack).contains(affix))n++;}
        return n;
    }
    public static int pieces(LivingEntity entity,MobAffix affix) {return pieces(entity,affix.id());}
    public static int pieces(LivingEntity entity,BossAffix affix) {return pieces(entity,Imbuing.BOSS_PREFIX+affix.id());}
    /** a power with a cooldown: true (and the cooldown starts) if it is ready */
    static boolean ready(Player player,String power,int ticks) {
        long now=player.level().getGameTime();var data=player.getPersistentData();
        if(now<data.getLong(COOLDOWN+power))return false;
        data.putLong(COOLDOWN+power,now+ticks);return true;
    }
    static boolean roll(Player player,float chance) {return player.getRandom().nextFloat()<chance;}
    public static boolean boss(LivingEntity entity) {return AffixEvents.isBoss(entity)||entity instanceof local.primordial.boss.SinEntity;}
    static List<LivingEntity> hostiles(Player player,double radius,LivingEntity except) {
        return player.level().getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(radius),
            e->e!=player&&e!=except&&e.isAlive()&&e instanceof Enemy&&e.distanceToSqr(player)<=radius*radius);
    }

    // ------------------------------------------------------------------ attributes the armor gives
    private static final Map<String,UUID> IDS=new HashMap<>();
    static UUID id(String power,EquipmentSlot slot) {return IDS.computeIfAbsent(power+"/"+slot.getName(),k->UUID.nameUUIDFromBytes(("primordial.armor_imbue."+k).getBytes()));}
    @SubscribeEvent public static void armorAttributes(ItemAttributeModifierEvent event) {
        var stack=event.getItemStack();
        if(!Imbuing.isArmor(stack)||Mob.getEquipmentSlotForItem(stack)!=event.getSlotType())return;
        var imbues=Imbuing.imbues(stack);if(imbues.isEmpty())return;
        var slot=event.getSlotType();
        for(var id:imbues)switch(id){
            case "strength"->add(event,Attributes.ATTACK_DAMAGE,"strength",slot,2,AttributeModifier.Operation.ADDITION);
            case "resistance"->add(event,Primordial.REDUCTION.get(),"resistance",slot,0.05,AttributeModifier.Operation.ADDITION);
            case "mastery"->add(event,Attributes.ATTACK_SPEED,"mastery",slot,0.08,AttributeModifier.Operation.MULTIPLY_BASE);
            default->{}
        }
    }
    static void add(ItemAttributeModifierEvent event,Attribute attribute,String power,EquipmentSlot slot,double amount,AttributeModifier.Operation operation) {
        event.addModifier(attribute,new AttributeModifier(id(power,slot),"primordial.armor_"+power,amount,operation));
    }

    // ------------------------------------------------------------------ the wearer struck
    /** 虚无: the blow misses altogether */
    @SubscribeEvent(priority=EventPriority.HIGH) public static void armorDodge(LivingAttackEvent event) {
        if(!(event.getEntity() instanceof Player player)||player.level().isClientSide||AffixCombat.secondary())return;
        if(event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        boolean core=WorldCores.wears(player,WorldCores.Realm.END);
        long now=player.level().getGameTime();var data=player.getPersistentData();
        if(core&&data.contains(WorldCores.DODGE_KEY)&&now-data.getLong(WorldCores.DODGE_KEY)<WorldCores.DODGE_COOLDOWN)return;
        boolean bossAttack=event.getSource().getEntity() instanceof LivingEntity attacker&&boss(attacker);
        double armor=pieces(player,MobAffix.VOID)*0.03*(bossAttack?0.5:1);
        double accessory=event.getSource().getEntity() instanceof LivingEntity&&local.primordial.affix.AffixAccessories.wears(player,MobAffix.VOID)
            ?(bossAttack?0.05:0.10)*local.primordial.affix.AffixAccessories.power(player):0;
        double coreChance=core?WorldCores.DODGE_CHANCE*(WorldCores.atHome(player,WorldCores.Realm.END)?2:1):0;
        if(player.getRandom().nextDouble()>=armor+accessory+coreChance)return;
        event.setCanceled(true);
        if(accessory>0)player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,40,0,false,false));
        if(core){data.putLong(WorldCores.DODGE_KEY,now);WorldCores.blink(player);}
        else if(player.level() instanceof ServerLevel level)level.sendParticles(ParticleTypes.PORTAL,player.getX(),player.getY()+1,player.getZ(),16,0.4,0.6,0.4,0.2);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void goldenHit(LivingHurtEvent event) {
        if(event.getEntity() instanceof Player player&&!player.level().isClientSide&&event.getAmount()>0)
            player.getPersistentData().putLong(GOLDEN_HIT,player.level().getGameTime());
    }
    @SubscribeEvent public static void armorStruck(LivingHurtEvent event) {
        if(!(event.getEntity() instanceof Player player)||player.level().isClientSide||AffixCombat.secondary())return;
        var source=event.getSource();if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        float amount=event.getAmount();int n;
        boolean physical=AffixCombat.physical(source);
        // true and fixed damage pass the resistances; true damage is still capped, fixed is not
        if(local.primordial.affix.AffixDamage.mitigable(source)){
            if((n=pieces(player,MobAffix.PHYSICAL_RESISTANCE))>0&&physical)amount*=1-0.08F*n;
            if((n=pieces(player,MobAffix.MAGIC_RESISTANCE))>0&&!physical)amount*=1-0.07F*n;
            if((n=pieces(player,BossAffix.WRATH))>0){int steps=(int)((1-player.getHealth()/player.getMaxHealth())*10);amount*=Math.max(0.60F,1-0.015F*n*Math.max(0,steps));}
            if((n=pieces(player,BossAffix.SLOTH))>0&&player.getDeltaMovement().horizontalDistanceSqr()<1e-4)amount*=1-0.08F*n;
        }
        if((n=pieces(player,MobAffix.DAMAGE_CAP))>0&&local.primordial.affix.AffixDamage.cappable(source)&&!local.primordial.affix.AffixBonds.uncapped(player,source))
            amount=Math.min(amount,player.getMaxHealth()*(0.8F-0.1F*n));
        if((n=pieces(player,MobAffix.SHARED))>0){
            var foes=hostiles(player,6,null);
            if(!foes.isEmpty()){float part=amount*0.08F*n;amount-=part;float each=part/foes.size();
                AffixCombat.secondary(()->{for(var foe:foes)foe.hurt(player.damageSources().indirectMagic(player,player),each);});}
        }
        event.setAmount(amount);
        final float taken=amount;
        if(source.getEntity() instanceof LivingEntity attacker&&attacker!=player&&attacker.isAlive()){
            if((n=pieces(player,MobAffix.REFLECTION))>0&&physical){float back=taken*0.08F*n;AffixCombat.secondary(()->attacker.hurt(player.damageSources().thorns(player),back));}
            if((n=pieces(player,MobAffix.CURSE))>0&&roll(player,0.1F*n))attacker.addEffect(new MobEffectInstance(randomHarm(player),100,0),player);
            if((n=pieces(player,MobAffix.SEAL))>0&&roll(player,0.1F*n)&&attacker instanceof Mob mob){var state=AffixState.get(mob);if(state!=null)state.seal(100);}
            if((n=pieces(player,MobAffix.THUNDER))>0&&roll(player,0.1F*n)&&player.level() instanceof ServerLevel level){
                var bolt=EntityType.LIGHTNING_BOLT.create(level);
                if(bolt!=null){bolt.setVisualOnly(true);bolt.moveTo(attacker.position());level.addFreshEntity(bolt);}
                AffixCombat.secondary(()->attacker.hurt(player.damageSources().indirectMagic(player,player),4+Math.min(40,attacker.getMaxHealth()*0.02F)));
            }
            if((n=pieces(player,MobAffix.SLOWNESS))>0&&roll(player,0.25F*n))attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,1),player);
            if((n=pieces(player,MobAffix.BEFUDDLEMENT))>0&&roll(player,0.1F*n)&&attacker instanceof Mob mob&&!boss(mob)){
                mob.setTarget(null);mob.getNavigation().stop();mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,0),player);}
            if((n=pieces(player,BossAffix.ENVY))>0&&roll(player,0.1F*n)){
                boolean stole=false;
                for(var effect:new ArrayList<>(attacker.getActiveEffects()))if(effect.getEffect().getCategory()==MobEffectCategory.BENEFICIAL){
                    attacker.removeEffect(effect.getEffect());player.addEffect(new MobEffectInstance(effect));stole=true;break;}
                if(!stole)player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,100,0));
            }
            if((n=pieces(player,BossAffix.LUST))>0&&!boss(attacker)&&!(attacker instanceof Player)
                &&attacker.getHealth()<attacker.getMaxHealth()*0.2F&&roll(player,0.08F*n)){
                float lethal=attacker.getHealth()+attacker.getAbsorptionAmount()+1;
                AffixCombat.secondary(()->attacker.hurt(player.damageSources().indirectMagic(player,player),lethal));
            }
        }
        if((n=pieces(player,MobAffix.SPACE))>0&&source.getEntity()!=null&&roll(player,0.08F*n)&&ready(player,"space",100))blink(player);
    }
    public static MobEffect randomHarm(Player player) {
        MobEffect[] harms={MobEffects.WEAKNESS,MobEffects.MOVEMENT_SLOWDOWN,MobEffects.POISON,MobEffects.WITHER,MobEffects.BLINDNESS,MobEffects.DIG_SLOWDOWN};
        return harms[player.getRandom().nextInt(harms.length)];
    }
    /** a short step away, as with a chorus fruit */
    static void blink(Player player) {
        double x0=player.getX(),y0=player.getY(),z0=player.getZ();
        for(int i=0;i<16;i++){
            double a=player.getRandom().nextDouble()*Math.PI*2,r=5+player.getRandom().nextDouble()*3;
            if(player.randomTeleport(x0+Math.cos(a)*r,y0+player.getRandom().nextInt(5)-2,z0+Math.sin(a)*r,true)){
                player.level().playSound(null,x0,y0,z0,SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,0.8F,1.4F);
                player.resetFallDistance();return;}
        }
    }
    /** thresholds crossed by what the blow actually takes: 召唤 at half health, 分裂 at under 30% */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void armorThresholds(LivingDamageEvent event) {
        if(!(event.getEntity() instanceof Player player)||player.level().isClientSide||event.getAmount()<=0)return;
        player.getPersistentData().putLong(GOLDEN_HIT,player.level().getGameTime());
        if(!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)){
            float blood=goldenBlood(player),used=Math.min(blood,event.getAmount());
            setGoldenBlood(player,blood-used);event.setAmount(event.getAmount()-used);
        }
        float before=player.getHealth(),after=before-event.getAmount(),max=player.getMaxHealth();int n;
        if(after<=0)return;
        if((n=pieces(player,MobAffix.SUMMON))>0&&before>=max*0.5F&&after<max*0.5F&&ready(player,"summon",1200)&&player.level() instanceof ServerLevel level){
            for(int i=0;i<n;i++){
                var wolf=EntityType.WOLF.create(level);if(wolf==null)continue;
                double a=Math.PI*2*i/n;
                wolf.moveTo(player.getX()+Math.cos(a)*1.5,player.getY(),player.getZ()+Math.sin(a)*1.5,player.getYRot(),0);
                wolf.tame(player);wolf.getPersistentData().putLong(SUMMONED,level.getGameTime()+600);
                if(event.getSource().getEntity() instanceof LivingEntity attacker&&attacker!=player)wolf.setTarget(attacker);
                level.addFreshEntity(wolf);
                level.sendParticles(ParticleTypes.CLOUD,wolf.getX(),wolf.getY()+0.5,wolf.getZ(),8,0.3,0.3,0.3,0.02);
            }
        }
        if((n=pieces(player,MobAffix.SPLIT))>0&&before>=max*0.3F&&after<max*0.3F&&ready(player,"split",1200/n)){
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,60,0,false,false));
            for(var mob:player.level().getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(24),m->m.getTarget()==player&&!boss(m)))mob.setTarget(null);
            if(player.level() instanceof ServerLevel level)level.sendParticles(ParticleTypes.LARGE_SMOKE,player.getX(),player.getY()+1,player.getZ(),20,0.4,0.6,0.4,0.02);
        }
    }

    // ------------------------------------------------------------------ the wearer striking
    @SubscribeEvent public static void armorStrikes(LivingHurtEvent event) {
        var victim=event.getEntity();
        if(victim.level().isClientSide||AffixCombat.secondary()||!(event.getSource().getEntity() instanceof Player player)||victim==player)return;
        float amount=event.getAmount();int n;
        if((n=pieces(player,MobAffix.ARMOR_PIERCE))>0)amount+=victim.getArmorValue()*0.1F*n;
        if((n=pieces(player,MobAffix.CAUSALITY))>0&&event.getSource().getDirectEntity() instanceof Projectile)amount*=1+0.15F*n;
        if((n=pieces(player,BossAffix.PRIDE))>0&&player.getHealth()>=player.getMaxHealth()*0.8F)amount*=1+0.12F*n;
        event.setAmount(amount);
        if((n=pieces(player,MobAffix.SHATTER))>0&&roll(player,0.1F*n)){
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,20,3),player);victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,20,1),player);
            if(victim instanceof Player shielded&&shielded.isBlocking())shielded.disableShield(true);
        }
        if((n=pieces(player,MobAffix.MALICE))>0&&roll(player,0.1F*n))
            for(var foe:hostiles(player,8,victim))if(foe instanceof Mob mob&&!boss(mob))mob.setTarget(victim);
    }
    @SubscribeEvent(priority=EventPriority.LOW) public static void armorDealt(LivingDamageEvent event) {
        var victim=event.getEntity();
        if(victim.level().isClientSide||AffixCombat.secondary()||event.getAmount()<=0||!(event.getSource().getEntity() instanceof Player player)||victim==player)return;
        int n;
        // 魔法: the extra comes after armor, so it passes it by
        if((n=pieces(player,MobAffix.MAGIC))>0)event.setAmount(event.getAmount()*(1+0.08F*n));
        if((n=pieces(player,MobAffix.BLOOD_PACT))>0)player.heal(event.getAmount()*0.03F*n);
    }
    @SubscribeEvent public static void armorKill(LivingDeathEvent event) {
        var dead=event.getEntity();
        if(dead.level().isClientSide||AffixCombat.secondary()||dead instanceof Player||!(event.getSource().getEntity() instanceof Player player))return;
        int n=pieces(player,MobAffix.RUIN);
        if(n<=0||!(dead.level() instanceof ServerLevel level))return;
        float burst=dead.getMaxHealth()*0.15F*n;
        var near=level.getEntitiesOfClass(LivingEntity.class,dead.getBoundingBox().inflate(3),e->e!=dead&&e!=player&&e.isAlive()&&e instanceof Enemy);
        AffixCombat.secondary(()->{for(var e:near)e.hurt(player.damageSources().indirectMagic(player,player),burst);});
        level.sendParticles(ParticleTypes.EXPLOSION,dead.getX(),dead.getY()+dead.getBbHeight()*0.5,dead.getZ(),1,0,0,0,0);
        level.playSound(null,dead.getX(),dead.getY(),dead.getZ(),SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,0.6F,1.4F);
    }
    @SubscribeEvent public static void armorExperience(LivingExperienceDropEvent event) {
        var player=event.getAttackingPlayer();if(player==null)return;
        int n=pieces(player,BossAffix.GREED);
        if(n>0)event.setDroppedExperience(Math.round(event.getDroppedExperience()*(1+0.2F*n)));
    }
    @SubscribeEvent public static void armorPickup(PlayerXpEvent.PickupXp event) {
        int n=pieces(event.getEntity(),MobAffix.ENLIGHTENMENT);
        if(n>0)event.getOrb().value=Math.max(event.getOrb().value,Math.round(event.getOrb().value*(1+0.15F*n)));
    }
    /** 增幅: blessings last longer */
    @SubscribeEvent public static void armorAmplify(MobEffectEvent.Added event) {
        if(!(event.getEntity() instanceof Player player)||player.level().isClientSide)return;
        var effect=event.getEffectInstance();
        if(effect.getEffect().getCategory()!=MobEffectCategory.BENEFICIAL||effect.isInfiniteDuration())return;
        int n=pieces(player,MobAffix.AMPLIFY);
        if(n>0)((local.primordial.mixin.SageEffectAccess)(Object)effect).sageDuration((int)Math.min(Integer.MAX_VALUE,(long)(effect.getDuration()*(1+0.15F*n))));
    }

    // ------------------------------------------------------------------ cheating death: 时间, then 永生
    private static final Map<UUID,float[]> HISTORY=new HashMap<>();
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void armorLethal(LivingDeathEvent event) {
        if(!(event.getEntity() instanceof Player player)||player.level().isClientSide)return;
        if(event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        int n;float health=0;
        if((n=pieces(player,MobAffix.TIME))>0&&ready(player,"time",7200/n)){
            var history=HISTORY.get(player.getUUID());
            health=history==null?0:history[0];
            if(health<=0)health=player.getMaxHealth()*0.3F;
            health=Math.min(health,player.getMaxHealth()*0.5F);
            if(player.level() instanceof ServerLevel level)level.sendParticles(ParticleTypes.REVERSE_PORTAL,player.getX(),player.getY()+1,player.getZ(),40,0.5,0.8,0.5,0.05);
            player.level().playSound(null,player.blockPosition(),SoundEvents.BEACON_POWER_SELECT,SoundSource.PLAYERS,1F,0.6F);
        }else if((n=pieces(player,MobAffix.UNDYING))>0&&ready(player,"undying",18000/n)){
            health=1;
            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,900,1));player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,100,1));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,800,0));
            player.level().broadcastEntityEvent(player,(byte)35);
        }
        if(health<=0)return;
        event.setCanceled(true);
        ((local.primordial.mixin.HealthAccessor)player).angelSetDead(false);
        player.deathTime=0;player.setHealth(Math.min(player.getMaxHealth(),health));
    }

    // ------------------------------------------------------------------ over time
    @SubscribeEvent public static void armorTick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof net.minecraft.server.level.ServerPlayer player)||!player.isAlive())return;
        int tick=player.tickCount,n;
        float blood=goldenBlood(player);
        if(tick%20==0&&player.level().getGameTime()-player.getPersistentData().getLong(GOLDEN_HIT)>=100)
            blood+=2+pieces(player,BossAffix.GLUTTONY);
        setGoldenBlood(player,blood);
        // the health of the last five seconds, for 时间
        if(tick%20==0){
            if(pieces(player,MobAffix.TIME)>0){
                var history=HISTORY.computeIfAbsent(player.getUUID(),k->new float[5]);
                System.arraycopy(history,1,history,0,4);history[4]=player.getHealth();
            }else HISTORY.remove(player.getUUID());
        }
        if(tick%60==0&&(n=pieces(player,MobAffix.REGENERATION))>0)player.heal(0.5F*n);
        if(tick%100==0&&(n=pieces(player,MobAffix.HEALING))>0){
            final float heal=n+player.getMaxHealth()*0.01F*n;
            for(var other:player.level().getEntitiesOfClass(Player.class,player.getBoundingBox().inflate(8),p->p.isAlive()))other.heal(heal);
            player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER,player.getX(),player.getY()+1,player.getZ(),6,1.5,0.5,1.5,0);
        }
        if(tick%20==0&&(n=pieces(player,MobAffix.DESATURATION))>0)
            for(var foe:hostiles(player,8+4*n,null))foe.addEffect(new MobEffectInstance(MobEffects.GLOWING,40,0,false,false));
    }
    /** the wolves 召唤 calls go after their half minute */
    @SubscribeEvent public static void summonedWolves(LivingEvent.LivingTickEvent event) {
        if(!(event.getEntity() instanceof Wolf wolf)||wolf.level().isClientSide||!wolf.getPersistentData().contains(SUMMONED))return;
        if(wolf.level().getGameTime()>wolf.getPersistentData().getLong(SUMMONED)){
            ((ServerLevel)wolf.level()).sendParticles(ParticleTypes.CLOUD,wolf.getX(),wolf.getY()+0.5,wolf.getZ(),8,0.3,0.3,0.3,0.02);
            wolf.discard();
        }
    }
}
