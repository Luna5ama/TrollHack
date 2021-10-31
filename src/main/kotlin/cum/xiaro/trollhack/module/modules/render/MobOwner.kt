package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.util.extension.cubic
import cum.xiaro.trollhack.util.extension.sq
import cum.xiaro.trollhack.util.math.MathUtils
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.UUIDManager
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.threads.onMainThreadSafe
import net.minecraft.entity.passive.AbstractHorse
import net.minecraft.entity.passive.EntityTameable

internal object MobOwner : Module(
    name = "MobOwner",
    description = "Displays the owner of tamed mobs",
    category = Category.RENDER
) {
    private val speed by setting("Speed", true)
    private val jump by setting("Jump", true)

    private const val invalidText = "Offline or invalid UUID!"

    init {
        onDisable {
            onMainThreadSafe {
                for (entity in world.loadedEntityList) {
                    if (entity !is AbstractHorse) continue
                    entity.alwaysRenderNameTag = false
                }
            }
        }

        safeListener<TickEvent.Post> {
            for (entity in world.loadedEntityList) {
                /* Non Horse types, such as wolves */
                if (entity is EntityTameable) {
                    val owner = entity.owner
                    if (!entity.isTamed || owner == null) continue

                    entity.alwaysRenderNameTag = true
                    entity.customNameTag = "Owner: ${owner.displayName.formattedText}}"
                }

                if (entity is AbstractHorse) {
                    val ownerUUID = entity.ownerUniqueId
                    if (!entity.isTame || ownerUUID == null) continue

                    val ownerName = UUIDManager.getByUUID(ownerUUID)?.name ?: invalidText
                    entity.alwaysRenderNameTag = true
                    entity.customNameTag = "Owner: $ownerName${getSpeed(entity)}${getJump(entity)}}"
                }
            }
        }
    }

    private fun getSpeed(horse: AbstractHorse): String {
        return if (!speed) "" else " S: ${MathUtils.round(43.17 * horse.aiMoveSpeed, 2)}"
    }

    private fun getJump(horse: AbstractHorse): String {
        return if (!jump) "" else " J: ${MathUtils.round(-0.1817584952 * horse.horseJumpStrength.cubic + 3.689713992 * horse.horseJumpStrength.sq + 2.128599134 * horse.horseJumpStrength - 0.343930367, 2)}"
    }
}