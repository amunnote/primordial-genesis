package local.primordial.smoke;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Every item and entity of the mod has a name in both languages (a renamed id once broke the Ascension Charm's). */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class LangCoverageGameTests {
    private static com.google.gson.JsonObject lang(String code) throws Exception {
        try(var in=LangCoverageGameTests.class.getResourceAsStream("/assets/primordial/lang/"+code+".json")){
            return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    @GameTest(template="empty") public static void everythingHasAName(GameTestHelper helper) throws Exception {
        for(var code:new String[]{"zh_cn","en_us"}){
            var lang=lang(code);var missing=new java.util.ArrayList<String>();
            for(var entry:ForgeRegistries.ITEMS.getEntries())if(entry.getKey().location().getNamespace().equals("primordial")&&!lang.has(entry.getValue().getDescriptionId()))missing.add(entry.getValue().getDescriptionId());
            for(var entry:ForgeRegistries.ENTITY_TYPES.getEntries())if(entry.getKey().location().getNamespace().equals("primordial")&&!lang.has(entry.getValue().getDescriptionId()))missing.add(entry.getValue().getDescriptionId());
            helper.assertTrue(missing.isEmpty(),code+" lacks "+missing);
        }
        helper.succeed();
    }
}
