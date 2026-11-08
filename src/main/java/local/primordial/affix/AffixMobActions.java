package local.primordial.affix;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.ForgeEventFactory;

public final class AffixMobActions {
    public static final String OWNER="primordial:affix_owner";
    private AffixMobActions() {}
    private static boolean position(Mob mob,LivingEntity around) {
        var level=(ServerLevel)around.level();
        for(int attempt=0;attempt<24;attempt++) {
            double angle=mob.getRandom().nextDouble()*Math.PI*2,radius=1.8+mob.getRandom().nextDouble()*2;
            double x=around.getX()+Math.cos(angle)*radius,z=around.getZ()+Math.sin(angle)*radius;
            int vertical=attempt/8-1;mob.setPos(x,around.getY()+vertical,z);
            if(level.hasChunkAt(mob.blockPosition())&&level.getWorldBorder().isWithinBounds(mob.getBoundingBox())
                &&level.noCollision(mob,mob.getBoundingBox())&&!level.containsAnyLiquid(mob.getBoundingBox()))return true;
        }
        return false;
    }
    public static boolean teleport(AffixState state,LivingEntity target) {
        var old=state.mob.position();if(position(state.mob,target)) {
            state.mob.teleportTo(state.mob.getX(),state.mob.getY(),state.mob.getZ());state.mob.getNavigation().stop();
            ((ServerLevel)state.mob.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,state.mob.getX(),state.mob.getY()+1,state.mob.getZ(),24,0.5,0.5,0.5,0.1);
            return true;
        }
        state.mob.setPos(old);return false;
    }
    public static void thunder(AffixState state,Player target) {
        var level=(ServerLevel)state.mob.level();
        var bolt=EntityType.LIGHTNING_BOLT.create(level);
        if(bolt!=null){bolt.moveTo(target.position());level.addFreshEntity(bolt);}
    }
    public static void ruin(AffixState state){
        if(state.tag.getBoolean("ruinUsed"))return;
        state.tag.putBoolean("ruinUsed",true);
        var mob=state.mob;var level=(ServerLevel)mob.level();
        var nearby=level.getEntitiesOfClass(LivingEntity.class,mob.getBoundingBox().inflate(4),
            other->other!=mob&&other.isAlive()&&mob.distanceToSqr(other)<=16&&!AffixAccessories.blocks(other,state,MobAffix.RUIN));
        float damage=mob.getMaxHealth()*0.5F;
        // The vanilla explosion provides its sound, particles and Forge-compatible terrain
        // handling. Its variable entity damage is suppressed, then our fixed damage is applied.
        // only a boss's blast breaks the ground (not the Primordial Lord's, in its temple); anything else only hurts
        boolean boss=state.originalBoss!=null||AffixEvents.isBoss(mob);
        boolean grief=boss&&!state.primordial()&&level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
            &&ForgeEventFactory.getMobGriefingEvent(level,mob);
        AffixCombat.ruinTerrain(()->level.explode(mob,mob.getX(),mob.getY(),mob.getZ(),4F,
            grief?net.minecraft.world.level.Level.ExplosionInteraction.MOB:net.minecraft.world.level.Level.ExplosionInteraction.NONE));
        AffixCombat.secondary(()->{for(var other:nearby)if(other.isAlive())other.hurt(mob.damageSources().mobAttack(mob),damage);});
    }
    public static int healNearby(AffixState state) {
        var mob=state.mob;var level=(ServerLevel)mob.level();int healed=0;
        var bounds=mob.getBoundingBox().inflate(10);
        for(var nearby:level.getEntitiesOfClass(Mob.class,bounds,other->other.isAlive()
            &&(other instanceof Enemy||AffixEvents.isBoss(other)))){
            if(mob.distanceToSqr(nearby)>100||AffixAccessories.foeBlocks(nearby,state,MobAffix.HEALING))continue;
            if(nearby==mob&&state.primordial())continue;   // it heals what fights beside it, not itself
            nearby.heal(nearby.getMaxHealth()*AffixScaling.healingPercent(state.level)/100F);
            healed++;
        }
        return healed;
    }
    public static Mob summon(AffixState state) {
        var level=(ServerLevel)state.mob.level();
        var ledger=AffixLedger.get(level);long alive=ledger.summonCount(state.mob.getUUID());
        if(alive>=3)return null;
        // the Primordial Lord calls up alchemical creations, not more of itself
        var types=local.primordial.entity.alchemical.AlchemicalCreations.types();
        var created=state.primordial()?types.get(state.mob.getRandom().nextInt(types.size())).create(level):state.mob.getType().create(level);
        if(!(created instanceof Mob child))return null;
        AffixState.suppress(child);child.getPersistentData().putUUID(OWNER,state.mob.getUUID());child.getPersistentData().putBoolean("primordial:summoned",true);
        if(!position(child,state.mob))return null;
        child.finalizeSpawn(level,level.getCurrentDifficultyAt(child.blockPosition()),MobSpawnType.MOB_SUMMONED,null,null);
        child.setTarget(state.mob.getTarget());if(!level.addFreshEntity(child))return null;
        ledger.trackChild(state.mob.getUUID(),child,true);return child;
    }
    public static int split(AffixState state) {
        // the Primordial Lord casts off two faint echoes of itself instead of whole copies
        if(state.mob instanceof local.primordial.boss.PrimordialLord lord)return lord.splitEchoes();
        var level=(ServerLevel)state.mob.level();int added=0;
        for(int index=0;index<2;index++) {
            var created=state.mob.getType().create(level);if(!(created instanceof Mob child))continue;
            CompoundTag saved=state.mob.saveWithoutId(new CompoundTag());saved.remove("UUID");saved.remove("Passengers");
            child.load(saved);child.getPersistentData().putUUID(OWNER,state.mob.getUUID());
            var cloned=child.getPersistentData().getCompound(AffixState.KEY);cloned.putBoolean("clone",true);cloned.putBoolean("splitUsed",true);
            if(!position(child,state.mob))continue;
            child.setHealth(state.mob.getHealth());child.setTarget(state.mob.getTarget());
            if(level.addFreshEntity(child)){AffixLedger.get(level).trackChild(state.mob.getUUID(),child,false);added++;}
        }
        return added;
    }
    public static void cleanup(ServerLevel level,UUID owner) {
        AffixLedger.get(level).cleanupChildren(level.getServer(),owner);
        // Cleanup crosses chunks and is only done at encounter end, not each tick.
        var children=new java.util.ArrayList<Mob>();for(var entity:level.getAllEntities())
            if(entity instanceof Mob mob&&mob.getPersistentData().hasUUID(OWNER)&&mob.getPersistentData().getUUID(OWNER).equals(owner))children.add(mob);
        children.forEach(net.minecraft.world.entity.Entity::discard);
    }
    public static void consume(AffixState state) {
        var mob=state.mob;var level=(ServerLevel)mob.level();
        if(!AffixConfig.BLOCK_CONSUMPTION.get()||!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)||!ForgeEventFactory.getMobGriefingEvent(level,mob))return;
        if(mob.getHealth()>=mob.getMaxHealth())return;
        for(var position:BlockPos.betweenClosed(mob.blockPosition().offset(-1,-1,-1),mob.blockPosition().offset(1,1,1))) {
            var block=level.getBlockState(position);
            boolean terrain=block.is(BlockTags.BASE_STONE_OVERWORLD)||block.is(BlockTags.BASE_STONE_NETHER)||block.is(BlockTags.DIRT)||block.is(BlockTags.SAND)
                ||block.is(net.minecraft.world.level.block.Blocks.GRAVEL)||block.is(net.minecraft.world.level.block.Blocks.END_STONE);
            if(!terrain||block.hasBlockEntity()||block.getDestroySpeed(level,position)<0)continue;
            if(!ForgeEventFactory.onEntityDestroyBlock(mob,position,block))continue;
            if(level.destroyBlock(position,false,mob)){mob.heal(mob.getMaxHealth()*0.05F);return;}
        }
    }
    public static Zombie smallPlayer(AffixState state,Player victim) {
        var level=(ServerLevel)victim.level();var child=EntityType.ZOMBIE.create(level);if(child==null)return null;
        AffixState.suppress(child);child.getPersistentData().putUUID(OWNER,state.mob.getUUID());child.getPersistentData().putBoolean("primordial:small_player",true);
        child.setBaby(true);child.setPos(victim.position());child.setCustomName(victim.getName());child.setCustomNameVisible(true);
        child.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(1,state.mob.getMaxHealth()*0.2));
        child.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(0,(state.mob.getAttribute(Attributes.ATTACK_DAMAGE)==null?state.affixes.size()*3:state.mob.getAttributeValue(Attributes.ATTACK_DAMAGE))*0.2));
        child.getAttribute(Attributes.ARMOR).setBaseValue(Math.max(0,state.mob.getArmorValue()*0.2));child.setHealth(child.getMaxHealth());
        for(var slot:EquipmentSlot.values()){
            var visual=victim.getItemBySlot(slot).copy();if(!visual.isEmpty()){visual.getOrCreateTag().putBoolean("primordial:visual_equipment",true);
                visual.getTag().remove("Enchantments");visual.getTag().remove("AttributeModifiers");}
            child.setItemSlot(slot,visual);child.setDropChance(slot,0);
        }
        var skull=new ItemStack(Items.PLAYER_HEAD);var profile=new CompoundTag();net.minecraft.nbt.NbtUtils.writeGameProfile(profile,victim.getGameProfile());
        skull.getOrCreateTag().put("SkullOwner",profile);child.setItemSlot(EquipmentSlot.HEAD,skull);
        child.setCanPickUpLoot(false);if(!level.addFreshEntity(child))return null;
        AffixLedger.get(level).trackChild(state.mob.getUUID(),child,false);return child;
    }
}
