package local.primordial.effect;

import java.util.UUID;
import local.primordial.Primordial;
import local.primordial.affix.AffixEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 月潮 The moon's tide: in the Overworld the monsters wax and wane with the moon. At full moon they are strongest
 * (health +40%, attack +25%, and one more affix when they are rolled); the gibbous moons +20% / +12%; the quarters as
 * they are; the crescents -10% / -5%; the new moon -15% / -10%. Monsters already about follow the moon as it changes,
 * their health kept in proportion.
 * <p>
 * On a full moon's night every player in the Overworld is filled with Inspiration (灵感) while the night lasts.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class MoonTide {
    static final UUID HEALTH=UUID.fromString("3e9f1c20-6a4b-4d2e-9f81-0c5d7a6b2e01"),ATTACK=UUID.fromString("3e9f1c20-6a4b-4d2e-9f81-0c5d7a6b2e02");
    /** by moon phase (0 full ... 4 new ... 7): the monsters' extra health and attack */
    static final double[] HEALTH_BY_PHASE={0.40,0.20,0,-0.10,-0.15,-0.10,0,0.20},ATTACK_BY_PHASE={0.25,0.12,0,-0.05,-0.10,-0.05,0,0.12};
    /** how long a full moon's Inspiration lasts once given; it is given again while the night lasts */
    public static final int INSPIRATION_TICKS=400;
    private static int lastPhase=-1;
    private static boolean announced;
    private MoonTide() {}

    public static boolean tidal(Level level) {return level.dimension()==Level.OVERWORLD;}
    public static int phase(Level level) {return level.getMoonPhase();}
    public static boolean fullMoon(Level level) {return tidal(level)&&phase(level)==0;}
    public static double health(Level level) {return tidal(level)?HEALTH_BY_PHASE[phase(level)]:0;}
    public static double attack(Level level) {return tidal(level)?ATTACK_BY_PHASE[phase(level)]:0;}
    /** one more affix for monsters rolled under a full moon */
    public static int extraAffixes(Level level) {return fullMoon(level)?1:0;}
    /** a monster the moon moves: what can carry affixes, but no boss of this mod */
    static boolean moved(LivingEntity entity) {
        return entity instanceof Mob mob&&!(entity instanceof local.primordial.boss.SinEntity)&&AffixEvents.eligible(mob,AffixEvents.isBoss(mob));
    }

    /** set the moon's strength on a monster, keeping its share of health */
    public static void apply(LivingEntity entity) {
        var level=entity.level();
        float share=entity.getMaxHealth()>0?entity.getHealth()/entity.getMaxHealth():1;
        boolean changed=set(entity,Attributes.MAX_HEALTH,HEALTH,health(level))|set(entity,Attributes.ATTACK_DAMAGE,ATTACK,attack(level));
        if(changed&&entity.isAlive())entity.setHealth(Math.max(1,entity.getMaxHealth()*share));
    }
    static boolean set(LivingEntity entity,Attribute attribute,UUID id,double amount) {
        var instance=entity.getAttribute(attribute);if(instance==null)return false;
        var old=instance.getModifier(id);
        if(old!=null&&Math.abs(old.getAmount()-amount)<1e-9)return false;
        if(old!=null)instance.removeModifier(id);
        if(amount!=0)instance.addTransientModifier(new AttributeModifier(id,"primordial.moon_tide",amount,AttributeModifier.Operation.MULTIPLY_TOTAL));
        return old!=null||amount!=0;
    }
    @SubscribeEvent(priority=EventPriority.LOW) public static void moonJoin(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide||!(event.getEntity() instanceof LivingEntity living)||!moved(living))return;
        apply(living);
    }
    @SubscribeEvent public static void moonTick(TickEvent.LevelTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level)||!tidal(level)||level.getGameTime()%100!=0)return;
        // the moon has changed: every monster about follows it
        int phase=phase(level);
        if(phase!=lastPhase){
            lastPhase=phase;announced=false;
            for(var entity:level.getAllEntities())if(entity instanceof LivingEntity living&&moved(living))apply(living);
        }
        // a full moon's night fills everyone with Inspiration
        if(phase==0&&level.isNight()){
            if(!announced){announced=true;
                for(var player:level.players())player.displayClientMessage(Component.translatable("message.primordial.full_moon").withStyle(ChatFormatting.LIGHT_PURPLE),false);}
            for(var player:level.players())if(player.isAlive()&&!player.isSpectator())
                player.addEffect(new MobEffectInstance(local.primordial.spirit.SpiritRealm.INSPIRATION.get(),INSPIRATION_TICKS,0,true,true));
        }
    }
}
