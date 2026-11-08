package local.primordial.smoke;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import local.primordial.Primordial;
import local.primordial.boss.FirstSage;
import local.primordial.boss.SinBosses;
import local.primordial.sound.ModSounds;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** The creatures' voices and the bosses' battle music: every sound defined, every vanilla sound it draws on real. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class SoundGameTests {
    @GameTest(template="empty") public static void soundsAreDefined(GameTestHelper helper) {
        var stream=Primordial.class.getResourceAsStream("/assets/primordial/sounds.json");
        helper.assertTrue(stream!=null,"sounds.json is there");
        var sounds=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
        // every registered sound has its definition
        for(var key:ForgeRegistries.SOUND_EVENTS.getKeys())if(key.getNamespace().equals(Primordial.ID))
            helper.assertTrue(sounds.has(key.getPath()),"sounds.json defines "+key);
        // every vanilla event a voice is built from exists
        for(var entry:sounds.entrySet())for(var sound:entry.getValue().getAsJsonObject().getAsJsonArray("sounds")){
            var object=sound.getAsJsonObject();
            if(!object.has("type")||!object.get("type").getAsString().equals("event"))continue;
            var id=new ResourceLocation(object.get("name").getAsString());
            helper.assertTrue(ForgeRegistries.SOUND_EVENTS.containsKey(id),entry.getKey()+" draws on "+id+", which does not exist");
        }
        helper.assertTrue(ModSounds.voice("qinglong","ambient")!=null&&ModSounds.music("first_sage_rubedo")!=null,"voices and tracks are registered");
        helper.succeed();
    }

    // a batch of its own: the Sage is a big body, and a neighbouring test clearing its ground would take it too
    @GameTest(template="empty",batch="boss_music") public static void bossesHaveVoicesAndMusic(GameTestHelper helper) {
        var dragon=helper.spawn(SinBosses.QINGLONG.get(),2,2,2);
        helper.assertTrue(dragon.battleMusic()==ModSounds.music("qinglong"),"the Azure Dragon has its battle music");
        var sage=helper.spawn(SinBosses.FIRST_SAGE.get(),2,2,2);
        helper.assertTrue(sage.battleMusic()==ModSounds.music("first_sage_nigredo"),"the First Sage begins with So Below");
        sage.wake();sage.setHealth(sage.getMaxHealth()*0.74F);
        helper.runAfterDelay(3,()->{
            helper.assertTrue(sage.stage()==2&&sage.battleMusic()==ModSounds.music("first_sage_albedo"),"and turns to Chrysopoeia at the whitening");
            dragon.discard();sage.discard();
            helper.succeed();
        });
    }
}
