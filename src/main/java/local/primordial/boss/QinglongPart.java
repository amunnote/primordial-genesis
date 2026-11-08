package local.primordial.boss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.entity.PartEntity;

/**
 * A piece of the Azure Dragon's long body (its head, or a length of it toward the tail), so a blow lands wherever the
 * dragon is seen and not only on its middle. It is struck for the dragon: the harm goes to the dragon as if dealt to it.
 * It lies a fixed way along the dragon from its middle, {@link #ahead} blocks forward (negative: behind), and turns with it.
 */
public class QinglongPart extends PartEntity<QinglongLord> {
    final double ahead;
    private final EntityDimensions size;
    public QinglongPart(QinglongLord dragon,double ahead,float width,float height) {
        super(dragon);this.ahead=ahead;size=EntityDimensions.scalable(width,height);refreshDimensions();
    }
    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean isPickable() {return true;}
    @Override public ItemStack getPickResult() {return getParent().getPickResult();}
    @Override public boolean hurt(DamageSource source,float amount) {return !isInvulnerableTo(source)&&getParent().hurt(source,amount);}
    @Override public boolean is(Entity entity) {return this==entity||getParent()==entity;}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {throw new UnsupportedOperationException();}
    @Override public EntityDimensions getDimensions(Pose pose) {return size;}
    @Override public boolean shouldBeSaved() {return false;}
}
