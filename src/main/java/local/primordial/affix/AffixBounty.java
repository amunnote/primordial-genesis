package local.primordial.affix;

import java.util.ArrayList;
import local.primordial.Primordial;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The more affixes a mob carries, the richer its spoils when a player brings it down. Its weight is one for each
 * affix, two more for a boss affix: each point adds 10% to its drops, a 25% chance of another roll of its loot, and
 * 15% to its experience. Recipes and affix materials are left alone: they keep their own odds.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class AffixBounty {
    public static final float COUNT_PER_POINT=0.1F,ROLL_PER_POINT=0.25F,EXPERIENCE_PER_POINT=0.15F;
    private AffixBounty() {}
    /** how much richer a mob's spoils are: one a point */
    public static int weight(AffixState state) {return state==null?0:state.affixes.size()+(state.originalBoss!=null?2:0);}
    static boolean special(ItemStack stack) {
        if(stack.getItem() instanceof local.primordial.item.AlchemyRecipeItem)return true;
        for(var material:AffixMaterials.ITEMS.values())if(stack.is(material.get()))return true;
        for(var material:AffixMaterials.BOSS_ITEMS.values())if(stack.is(material.get()))return true;
        return false;
    }
    @SubscribeEvent(priority=EventPriority.LOW) public static void bountyDrops(LivingDropsEvent event) {
        if(!(event.getEntity() instanceof Mob mob)||!(mob.level() instanceof ServerLevel level)||mob instanceof local.primordial.boss.SinEntity)return;
        if(!(event.getSource().getEntity() instanceof Player player))return;
        int weight=weight(AffixState.get(mob));if(weight<=0)return;
        var random=mob.getRandom();
        // more of what it dropped
        var extra=new ArrayList<ItemEntity>();
        for(var drop:event.getDrops()){
            var stack=drop.getItem();if(stack.isEmpty()||special(stack))continue;
            float more=stack.getCount()*COUNT_PER_POINT*weight;
            int add=(int)more+(random.nextFloat()<more-(int)more?1:0);
            if(add<=0)continue;
            int room=stack.getMaxStackSize()-stack.getCount();
            if(add<=room){stack.grow(add);drop.setItem(stack);}
            else{if(room>0){stack.grow(room);drop.setItem(stack);}extra.add(new ItemEntity(level,drop.getX(),drop.getY(),drop.getZ(),stack.copyWithCount(add-room)));}
        }
        event.getDrops().addAll(extra);
        // and more rolls of its loot
        int rolls=0;for(int i=0;i<weight;i++)if(random.nextFloat()<ROLL_PER_POINT)rolls++;
        if(rolls==0)return;
        var table=level.getServer().getLootData().getLootTable(mob.getLootTable());
        var params=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,mob).withParameter(LootContextParams.ORIGIN,mob.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE,event.getSource()).withOptionalParameter(LootContextParams.KILLER_ENTITY,player)
            .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY,event.getSource().getDirectEntity())
            .withParameter(LootContextParams.LAST_DAMAGE_PLAYER,player).withLuck(player.getLuck()).create(LootContextParamSets.ENTITY);
        for(int i=0;i<rolls;i++)for(var stack:table.getRandomItems(params)){
            if(stack.isEmpty()||special(stack))continue;
            var item=new ItemEntity(level,mob.getX(),mob.getY(),mob.getZ(),stack);item.setDefaultPickUpDelay();event.getDrops().add(item);
        }
    }
    @SubscribeEvent public static void bountyExperience(LivingExperienceDropEvent event) {
        if(!(event.getEntity() instanceof Mob mob)||mob instanceof local.primordial.boss.SinEntity||event.getAttackingPlayer()==null)return;
        int weight=weight(AffixState.get(mob));
        if(weight>0)event.setDroppedExperience(Math.round(event.getDroppedExperience()*(1+EXPERIENCE_PER_POINT*weight)));
    }
}
