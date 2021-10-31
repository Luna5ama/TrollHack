package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.render.RenderEntityEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.and
import cum.xiaro.trollhack.util.atValue
import cum.xiaro.trollhack.util.threads.runSafe
import net.minecraft.block.BlockSnow
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleFirework
import net.minecraft.entity.EntityAreaEffectCloud
import net.minecraft.entity.effect.EntityLightningBolt
import net.minecraft.entity.item.*
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.passive.IAnimals
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.*
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.*
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntityEnchantmentTable
import net.minecraft.world.EnumSkyBlock
import net.minecraftforge.registries.GameData
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

internal object NoRender : Module(
    name = "NoRender",
    category = Category.RENDER,
    description = "Ignore entity spawn packets"
) {
    private val page = setting("Page", Page.OTHER)

    // Object
    private val droppedItem by setting("Dropped Item", Mode.OFF, page.atValue(Page.OBJECT))
    private val effectCloud by setting("Effect Cloud", Mode.OFF, page.atValue(Page.OBJECT))
    private val tnt by setting("TNT", Mode.OFF, page.atValue(Page.OBJECT))
    private val crystal by setting("Crystal", Mode.OFF, page.atValue(Page.OBJECT))
    private val arrow by setting("Arrow", Mode.OFF, page.atValue(Page.OBJECT))
    private val itemFrame by setting("Item Frame", Mode.OFF, page.atValue(Page.OBJECT))
    private val painting by setting("Painting", Mode.OFF, page.atValue(Page.OBJECT))
    private val potion by setting("Potion", Mode.OFF, page.atValue(Page.OBJECT))
    private val xpBottle by setting("XP Bottle", Mode.REMOVE, page.atValue(Page.OBJECT))
    private val xpOrb by setting("XP Orb", Mode.REMOVE, page.atValue(Page.OBJECT))
    private val fallingBlock by setting("Falling Block", Mode.REMOVE, page.atValue(Page.OBJECT))
    private val firework by setting("Firework", Mode.REMOVE, page.atValue(Page.OBJECT))
    private val armorStand by setting("Armor Stand", Mode.OFF, page.atValue(Page.OBJECT))
    private val projectile by setting("Projectile", Mode.OFF, page.atValue(Page.OBJECT))

    // Entities
    private val player by setting("Player", Mode.OFF, page.atValue(Page.ENTITY))
    private val mob by setting("Mobs", Mode.OFF, page.atValue(Page.ENTITY))
    private val animal by setting("Animal", Mode.OFF, page.atValue(Page.ENTITY))

    // Tile Entity
    private val tileEntityRange by setting("Tile Entity Range", true, page.atValue(Page.TILE_ENTITY))
    private val range by setting("Range", 16, 0..128, 2, page.atValue(Page.TILE_ENTITY) and ::tileEntityRange)
    val beaconBeams by setting("Beacon Beams", true, page.atValue(Page.TILE_ENTITY))
    private val enchantingTableSnow by setting("Enchanting Table Snow", false, page.atValue(Page.TILE_ENTITY), description = "Replace enchanting table models with snow layers")

    // Others
    val map by setting("Maps", false, page.atValue(Page.OTHER))
    private val explosion by setting("Explosions", true, page.atValue(Page.OTHER))
    val signText by setting("Sign Text", false, page.atValue(Page.OTHER))
    private val particle by setting("Particle", Mode.REMOVE, page.atValue(Page.OTHER))
    private val allLightingUpdate by setting("All Lighting Update", true, page.atValue(Page.OTHER))
    val skylightUpdate by setting("SkyLight Update", true, page.atValue(Page.OTHER))
    private val lightning by setting("Lightning", Mode.REMOVE, page.atValue(Page.OTHER))

    private enum class Page {
        OBJECT, ENTITY, TILE_ENTITY, OTHER
    }

    private enum class Mode(val hide: Boolean, val remove: Boolean) {
        OFF(false, false),
        HIDE(true, false),
        REMOVE(true, true);

        fun handleRenderEvent(event: RenderEntityEvent.All) {
            event.cancelled = hide
            if (remove) event.entity.setDead()
        }
    }

    init {
        listener<PacketEvent.Receive> {
            when (it.packet) {
                is SPacketExplosion -> {
                    it.cancelled = explosion
                }
                is SPacketSpawnGlobalEntity -> {
                    it.cancelled = lightning.remove
                }
                is SPacketSpawnExperienceOrb -> {
                    it.cancelled = xpOrb.remove
                }
                is SPacketSpawnObject -> {
                    it.cancelled = when (it.packet.type) {
                        2 -> droppedItem.remove
                        3 -> effectCloud.remove
                        50 -> tnt.remove
                        51 -> crystal.remove
                        60, 91 -> arrow.remove
                        70 -> fallingBlock.remove
                        71 -> itemFrame.remove
                        73 -> potion.remove
                        75 -> xpBottle.remove
                        78 -> armorStand.remove
                        76 -> firework.remove
                        61, 62, 63, 64, 66, 67, 68, 93 -> projectile.remove
                        else -> false
                    }
                }
                is SPacketSpawnMob -> {
                    val entityClass = GameData.getEntityRegistry().getValue(it.packet.entityType).entityClass
                    if (EntityMob::class.java.isAssignableFrom(entityClass)) {
                        if (mob.remove) it.cancel()
                    } else if (IAnimals::class.java.isAssignableFrom(entityClass)) {
                        if (animal.remove) it.cancel()
                    }
                }
                is SPacketSpawnPainting -> {
                    it.cancelled = painting.remove
                }
                is SPacketParticles -> {
                    it.cancelled = particle.remove
                }
            }
        }

        listener<RenderEntityEvent.All.Pre>(Int.MAX_VALUE) {
            when (val entity = it.entity) {
                is EntityLightningBolt -> lightning.handleRenderEvent(it)
                is EntityXPOrb -> xpOrb.handleRenderEvent(it)
                is EntityItem -> droppedItem.handleRenderEvent(it)
                is EntityAreaEffectCloud -> effectCloud.handleRenderEvent(it)
                is EntityTNTPrimed -> tnt.handleRenderEvent(it)
                is EntityEnderCrystal -> crystal.handleRenderEvent(it)

                is EntityArrow,
                is EntityTippedArrow,
                is EntitySpectralArrow -> arrow.handleRenderEvent(it)

                is EntityFallingBlock -> fallingBlock.handleRenderEvent(it)
                is EntityItemFrame -> itemFrame.handleRenderEvent(it)
                is EntityPotion -> potion.handleRenderEvent(it)
                is EntityExpBottle -> xpBottle.handleRenderEvent(it)
                is EntityArmorStand -> armorStand.handleRenderEvent(it)
                is EntityFireworkRocket -> firework.handleRenderEvent(it)

                is EntitySnowball,
                is EntityEgg,
                is EntitySmallFireball,
                is EntityFireball,
                is EntityLargeFireball,
                is EntityWitherSkull,
                is EntityShulkerBullet,
                is EntityLlamaSpit,
                is EntityDragonFireball -> projectile.handleRenderEvent(it)

                is IAnimals -> animal.handleRenderEvent(it)
                is EntityMob -> mob.handleRenderEvent(it)
                is EntityPlayer -> {
                    if (entity != mc.player && entity != mc.renderViewEntity) {
                        player.handleRenderEvent(it)
                    }
                }
            }
        }
    }

    @JvmStatic
    fun handleTileEntity(tileEntity: TileEntity, ci: CallbackInfo) {
        if (enchantingTableSnow && tileEntity is TileEntityEnchantmentTable) {
            runSafe {
                val blockState = Blocks.SNOW_LAYER.defaultState.withProperty(BlockSnow.LAYERS, 7)
                world.setBlockState(tileEntity.pos, blockState)
                world.markTileEntityForRemoval(tileEntity)
            }

            ci.cancel()
        } else if (tileEntityRange) {
            val entity = mc.renderViewEntity ?: mc.player ?: return
            if (entity.getDistanceSqToCenter(tileEntity.pos) > range * range) {
                ci.cancel()
            }
        }
    }

    @JvmStatic
    fun shouldHideParticles(effect: Particle): Boolean {
        return isEnabled
            && (particle.hide
            || firework.hide && (effect is ParticleFirework.Overlay || effect is ParticleFirework.Spark || effect is ParticleFirework.Starter))
    }

    @JvmStatic
    fun handleLighting(lightType: EnumSkyBlock): Boolean {
        return isEnabled && (allLightingUpdate || skylightUpdate && lightType == EnumSkyBlock.SKY)
    }
}