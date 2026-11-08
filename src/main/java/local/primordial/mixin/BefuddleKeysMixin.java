package local.primordial.mixin;

import local.primordial.client.AffixClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Befuddlement: in the key handling, the attack (break) and use (place) keys answer for each other.
 * The bindings themselves are never touched, so nothing wrong can end up saved in options.txt.
 */
@Mixin(Minecraft.class)
public abstract class BefuddleKeysMixin {
    @Redirect(method="handleKeybinds",at=@At(value="FIELD",target="Lnet/minecraft/client/Options;keyAttack:Lnet/minecraft/client/KeyMapping;",opcode=Opcodes.GETFIELD))
    private KeyMapping angelBefuddledAttack(Options options) {return AffixClient.keysSwapped()?options.keyUse:options.keyAttack;}
    @Redirect(method="handleKeybinds",at=@At(value="FIELD",target="Lnet/minecraft/client/Options;keyUse:Lnet/minecraft/client/KeyMapping;",opcode=Opcodes.GETFIELD))
    private KeyMapping angelBefuddledUse(Options options) {return AffixClient.keysSwapped()?options.keyAttack:options.keyUse;}
}
