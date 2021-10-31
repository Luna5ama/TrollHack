package cum.xiaro.trollhack.util.accessor

import cum.xiaro.trollhack.accessor.entity.AccessorEntityLivingBase
import net.minecraft.entity.EntityLivingBase

fun EntityLivingBase.onItemUseFinish() {
    (this as AccessorEntityLivingBase).trollInvokeOnItemUseFinish()
}