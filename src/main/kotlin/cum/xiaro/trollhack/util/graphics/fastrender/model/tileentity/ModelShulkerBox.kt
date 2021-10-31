package cum.xiaro.trollhack.util.graphics.fastrender.model.tileentity

import cum.xiaro.trollhack.util.graphics.fastrender.model.Model
import cum.xiaro.trollhack.util.graphics.fastrender.model.ModelBuilder

class ModelShulkerBox : Model(64, 64) {
    override fun ModelBuilder.buildModel() {
        // Base
        childModel(0.0f, 28.0f) {
            addBox(-8.0f, 0.0f, -8.0f, 16.0f, 8.0f, 16.0f)
        }

        // Lid
        childModel {
            addBox(-8.0f, 4.0f, -8.0f, 16.0f, 12.0f, 16.0f)
        }
    }
}