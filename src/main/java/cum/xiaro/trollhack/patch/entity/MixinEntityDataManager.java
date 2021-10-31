package cum.xiaro.trollhack.patch.entity;

import cum.xiaro.trollhack.util.collections.ArrayMap;
import io.netty.handler.codec.EncoderException;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ReportedException;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

@SuppressWarnings("unchecked")
@Mixin(EntityDataManager.class)
public class MixinEntityDataManager {
    @Shadow @Final private ReadWriteLock lock;
    @Shadow @Final private Entity entity;
    @Shadow private boolean empty;
    @Shadow private boolean dirty;

    private final ArrayMap<EntityDataManager.DataEntry<?>> entriesOverwrite = new ArrayMap<>();

    /**
     * @author Xiaro
     * @reason Fuck
     */
    @Overwrite
    public <T> void register(DataParameter<T> key, T value) {
        int i = key.getId();

        if (i > 254) {
            throw new IllegalArgumentException("Data value id is too big with " + i + "! (Max is " + 254 + ")");
        } else if (this.entriesOverwrite.containsKey(i)) {
            throw new IllegalArgumentException("Duplicate id value for " + i + "!");
        } else if (DataSerializers.getSerializerId(key.getSerializer()) < 0) {
            throw new IllegalArgumentException("Unregistered serializer " + key.getSerializer() + " for " + i + "!");
        } else {
            this.setEntry(key, value);
        }
    }

    /**
     * @author Xiaro
     * @reason Fuck
     */
    @Overwrite
    private <T> void setEntry(DataParameter<T> key, T value) {
        EntityDataManager.DataEntry<T> dataEntry = new EntityDataManager.DataEntry<>(key, value);
        this.lock.writeLock().lock();
        this.entriesOverwrite.put(key.getId(), dataEntry);
        this.empty = false;
        this.lock.writeLock().unlock();
    }

    /**
     * @author Xiaro
     * @reason Fuck
     */
    @Overwrite
    private <T> EntityDataManager.DataEntry<T> getEntry(DataParameter<T> key) {
        this.lock.readLock().lock();
        EntityDataManager.DataEntry<T> dataEntry;

        try {
            dataEntry = (EntityDataManager.DataEntry<T>) this.entriesOverwrite.get(key.getId());
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Getting synched entity data");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Synched entity data");
            crashreportcategory.addCrashSection("Data ID", key);
            throw new ReportedException(crashreport);
        }

        this.lock.readLock().unlock();
        return dataEntry;
    }

    /**
     * @author Xiaro
     * @reason Fuck
     */
    @Overwrite
    @Nullable
    public List<EntityDataManager.DataEntry<?>> getDirty() {
        List<EntityDataManager.DataEntry<?>> list = null;

        if (this.dirty) {
            this.lock.readLock().lock();

            for (EntityDataManager.DataEntry<?> dataEntry : this.entriesOverwrite.values()) {
                if (dataEntry.isDirty()) {
                    dataEntry.setDirty(false);

                    if (list == null) {
                        list = new ArrayList<>();
                    }

                    list.add(dataEntry.copy());
                }
            }

            this.lock.readLock().unlock();
        }

        this.dirty = false;
        return list;
    }

    /**
     * @author Xiaro
     * @reason Fuck
     */
    @Overwrite
    public void writeEntries(PacketBuffer buf) {
        this.lock.readLock().lock();

        for (EntityDataManager.DataEntry<?> dataEntry : this.entriesOverwrite.values()) {
            writeEntryS(buf, dataEntry);
        }

        this.lock.readLock().unlock();
        buf.writeByte(255);
    }

    /**
     * @author Xiaro
     * @reason Fuck
     */
    @Overwrite
    @Nullable
    public List<EntityDataManager.DataEntry<?>> getAll() {
        List<EntityDataManager.DataEntry<?>> list = null;
        this.lock.readLock().lock();

        for (EntityDataManager.DataEntry<?> dataEntry : this.entriesOverwrite.values()) {
            if (list == null) {
                list = new ArrayList<>();
            }

            list.add(dataEntry.copy());
        }

        this.lock.readLock().unlock();
        return list;
    }

    /**
     * @author Xiaro
     * @reason Fuck
     */
    @SideOnly(Side.CLIENT)
    @Overwrite
    public void setEntryValues(List<EntityDataManager.DataEntry<?>> entriesIn) {
        this.lock.writeLock().lock();

        for (EntityDataManager.DataEntry<?> dataEntry : entriesIn) {
            EntityDataManager.DataEntry<?> dataEntry1 = this.entriesOverwrite.get(dataEntry.getKey().getId());

            if (dataEntry1 != null) {
                this.setEntryValueS(dataEntry1, dataEntry);
                this.entity.notifyDataManagerChange(dataEntry.getKey());
            }
        }

        this.lock.writeLock().unlock();
        this.dirty = true;
    }

    /**
     * @author Xiaro
     * @reason Fuck
     */
    @Overwrite
    public void setClean() {
        this.dirty = false;
        this.lock.readLock().lock();

        for (EntityDataManager.DataEntry<?> dataEntry : this.entriesOverwrite.values()) {
            dataEntry.setDirty(false);
        }

        this.lock.readLock().unlock();
    }

    private <T> void setEntryValueS(EntityDataManager.DataEntry<T> target, EntityDataManager.DataEntry<?> source) {
        target.setValue((T) source.getValue());
    }

    private static <T> void writeEntryS(PacketBuffer buf, EntityDataManager.DataEntry<T> entry) {
        DataParameter<T> dataParameter = entry.getKey();
        int i = DataSerializers.getSerializerId(dataParameter.getSerializer());

        if (i < 0) {
            throw new EncoderException("Unknown serializer type " + dataParameter.getSerializer());
        } else {
            buf.writeByte(dataParameter.getId());
            buf.writeVarInt(i);
            dataParameter.getSerializer().write(buf, entry.getValue());
        }
    }
}
