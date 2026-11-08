package local.primordial.smoke;

import local.primordial.spirit.SpiritIslands;
import local.primordial.spirit.SpiritRealm;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Coming back into the Spirit Realm: where one left by choice, somewhere at random after being thrown out. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class SpiritReturnGameTests {
    @GameTest(template="empty",timeoutTicks=400) public static void realmEntryRemembersOrScatters(GameTestHelper helper) {
        var realm=helper.getLevel().getServer().getLevel(SpiritRealm.LEVEL);
        helper.assertTrue(realm!=null,"the realm exists");
        var player=helper.makeMockPlayer();var random=RandomSource.create(42);
        var arrival=SpiritRealm.arrivalPoint(realm);
        helper.assertTrue(SpiritRealm.entryPoint(realm,player,random).distanceTo(arrival)<0.01,"the first time: the arrival island");
        // left by drinking Inspiration: back where they stood
        player.setPos(300.5,88,-140.5);
        SpiritRealm.rememberLeaving(player,false);
        var back=SpiritRealm.entryPoint(realm,player,random);
        helper.assertTrue(back.x==300.5&&back.y==88&&back.z==-140.5,"left by choice: back where they stood, got "+back);
        // a death in another world after leaving keeps what the realm remembers
        var reborn=helper.makeMockPlayer();reborn.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG,SpiritRealm.kept(player).copy());
        helper.assertTrue(SpiritRealm.entryPoint(realm,reborn,random).y==88,"kept across a respawn");
        // thrown out by a death far out: somewhere at random, on an island within 10000 blocks of where it happened
        player.setPos(4000.5,80,-3000.5);
        SpiritRealm.rememberLeaving(player,true);
        var dead=player.position();
        helper.assertTrue(!SpiritRealm.kept(player).contains(SpiritRealm.LEFT_AT),"a death forgets the spot");
        var spots=new java.util.HashSet<String>();
        for(int i=0;i<4;i++){
            var at=SpiritRealm.entryPoint(realm,player,random);
            var island=SpiritIslands.islandAt((int)Math.floor(at.x),(int)Math.floor(at.z));
            helper.assertTrue(island!=null,"a scattered landing is on an island, got "+at);
            double far=Math.hypot(at.x-dead.x,at.z-dead.z);
            helper.assertTrue(far<=SpiritRealm.SCATTER_RADIUS+SpiritIslands.CELL*1.5,"within 10000 blocks of the death, got "+far);
            spots.add(island.cellX()+","+island.cellZ());
        }
        helper.assertTrue(spots.size()>1,"and not always the same island: "+spots);
        // leaving by choice again puts the spot back
        player.setPos(10.5,90,10.5);SpiritRealm.rememberLeaving(player,false);
        helper.assertTrue(!SpiritRealm.kept(player).getBoolean(SpiritRealm.SCATTERED)&&SpiritRealm.entryPoint(realm,player,random).y==90,"leaving by choice again is remembered");
        helper.succeed();
    }
}
