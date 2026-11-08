package local.primordial.alchemy;

import java.util.ArrayList;
import java.util.List;
import local.primordial.Primordial;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Right-clicking a mob with a Source of Transmutation turns it into a random other kind of mob (one source used).
 * Handled before the mob's own interaction, so villagers etc. are transmuted rather than opening their screens.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class Transmutation {
    private Transmutation() {}

    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event) {
        var stack=event.getItemStack();
        if(!stack.is(Primordial.TRANSMUTATION_SOURCE.get())||!(event.getTarget() instanceof Mob mob)||!canTransmute(mob.getType()))return;
        if(event.getLevel() instanceof ServerLevel level&&transmute(level,mob)!=null&&!event.getEntity().getAbilities().instabuild)stack.shrink(1);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }

    /** bosses and a few mobs too dangerous or odd to hand out are neither transmuted nor produced */
    static boolean canTransmute(EntityType<?> type) {
        return !type.is(Tags.EntityTypes.BOSSES)&&type!=EntityType.WARDEN&&type!=EntityType.ELDER_GUARDIAN&&type!=EntityType.GIANT;
    }
    static List<EntityType<?>> candidates(EntityType<?> except) {
        List<EntityType<?>> out=new ArrayList<>();
        for(EntityType<?> type:ForgeRegistries.ENTITY_TYPES.getValues())
            if(type!=except&&type.getCategory()!=MobCategory.MISC&&type.canSummon()&&canTransmute(type))out.add(type);
        return out;
    }
    /** @return the new mob, or null if nothing could be made */
    public static Mob transmute(ServerLevel level,Mob mob) {
        var pool=candidates(mob.getType());
        for(int attempt=0;attempt<8&&!pool.isEmpty();attempt++){
            EntityType<?> type=pool.remove(level.random.nextInt(pool.size()));
            if(!(type.create(level) instanceof Mob made))continue;
            made.moveTo(mob.getX(),mob.getY(),mob.getZ(),mob.getYRot(),mob.getXRot());
            made.setYHeadRot(mob.getYHeadRot());
            made.finalizeSpawn(level,level.getCurrentDifficultyAt(mob.blockPosition()),MobSpawnType.CONVERSION,null,null);
            // a name a player gave carries over; an affixed mob's 【...】 name does not (the new creature rolls its own)
            var given=local.primordial.affix.AffixState.givenName(mob);
            if(given!=null){made.setCustomName(given);made.setCustomNameVisible(local.primordial.affix.AffixState.givenNameVisible(mob));}
            if(mob.isPersistenceRequired())made.setPersistenceRequired();
            level.addFreshEntity(made);
            mob.discard();
            level.sendParticles(ParticleTypes.WITCH,made.getX(),made.getY()+made.getBbHeight()/2,made.getZ(),30,0.4,0.5,0.4,0.05);
            level.sendParticles(ParticleTypes.END_ROD,made.getX(),made.getY()+made.getBbHeight()/2,made.getZ(),10,0.3,0.4,0.3,0.03);
            level.playSound(null,made.blockPosition(),SoundEvents.ZOMBIE_VILLAGER_CONVERTED,SoundSource.NEUTRAL,1F,1.2F);
            return made;
        }
        return null;
    }
}
