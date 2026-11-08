package local.primordial.guide;
import java.util.*;
import local.primordial.affix.BossAffix;
import local.primordial.boss.SinBosses;
import local.primordial.spirit.*;
import local.primordial.alchemy.AlchemyKnowledge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
/** A view of existing personal records, never a second progress store. */
public final class NextStep {
    public record Target(String id,Component name,BlockPos pos,Component condition,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
        public Target(String id,Component name,BlockPos pos,Component condition){this(id,name,pos,condition,SpiritRealm.LEVEL);}
    }
    public static Target byId(String id){return places().stream().filter(t->t.id().equals(id)).findFirst().orElse(null);}
    public static boolean sideQuest(String id){return Guardian.byId(id)!=null;}
    public record Rule(Component text,boolean done) {}
    public static List<Target> places(){
        var out=new ArrayList<Target>();
        for(var sin:BossAffix.values())out.add(new Target(sin.id(),Component.translatable(SinBosses.lordType(sin).getDescriptionId()),SinSanctums.seat(sin),t("free")));
        for(var g:Guardian.values())out.add(new Target(g.id,Component.translatable(g.type().getDescriptionId()),BlockPos.ZERO,t("four_realms").append(" · ").append(t(g.direction())).append(g==Guardian.BAIHU?t("baihu.details"):g==Guardian.ZHUQUE?t("zhuque.details"):g==Guardian.XUANWU?t("xuanwu.details"):Component.empty()),net.minecraft.world.level.Level.OVERWORLD));
        out.add(new Target("temple",t("temple"),PrimordialTemple.ENTRY,t("seven")));
        out.add(new Target("sage",t("sage"),SageSanctum.SEAT,t("origin")));
        return out;
    }
    public static List<Target> candidates(ServerPlayer p){
        var all=places();var out=new ArrayList<Target>();var sins=PrimordialTemple.defeated(p);
        for(var sin:BossAffix.values())if(!sins.contains(sin))out.add(all.get(sin.ordinal()));
        if(!out.isEmpty()){
            out.sort(Comparator.comparingDouble(v->v.pos().distToCenterSqr(p.getX(),p.getY(),p.getZ())));return out;
        }
        return List.of(byId(PrimordialTemple.primordialFelled(p)?"sage":"temple"));
    }
    public static List<Rule> rules(ServerPlayer p){
        var out=new ArrayList<Rule>();
        out.add(new Rule(t("altar"),achievement(p,local.primordial.achievement.Achievements.FIRST_FURNACE)));
        out.add(new Rule(t("learn"),!AlchemyKnowledge.known(p).isEmpty()));
        out.add(new Rule(t("enter"),achievement(p,local.primordial.achievement.Achievements.SPIRIT_REALM)));
        var all=places();var sins=PrimordialTemple.defeated(p);var guards=PrimordialTemple.guardians(p);
        for(var sin:BossAffix.values())out.add(new Rule(t("hunt",all.get(sin.ordinal()).name()),sins.contains(sin)));
        for(var g:Guardian.values())out.add(new Rule(t("four_realms").append(" · ").append(t("hunt",byId(g.id).name())),guards.contains(g)));
        out.add(new Rule(t("hunt",t("temple")),PrimordialTemple.primordialFelled(p)));
        out.add(new Rule(t("visit",t("sage")),achievement(p,local.primordial.achievement.Achievements.COURT)));
        return out;
    }
    private static boolean achievement(ServerPlayer p,String id){var a=p.server.getAdvancements().getAdvancement(new net.minecraft.resources.ResourceLocation("primordial",id));return a!=null&&p.getAdvancements().getOrStartProgress(a).isDone();}
    public static Component advice(ServerPlayer p){
        var rules=rules(p);for(int i=0;i<3;i++)if(!rules.get(i).done())return t("next",rules.get(i).text());
        if(!PrimordialTemple.primordialFelled(p))return t("next",t("hunt",candidates(p).get(0).name()));
        return t("next",t("visit",t("sage")));
    }
    public static net.minecraft.network.chat.MutableComponent t(String key,Object... args){return Component.translatable("guide.primordial."+key,args);}
}
