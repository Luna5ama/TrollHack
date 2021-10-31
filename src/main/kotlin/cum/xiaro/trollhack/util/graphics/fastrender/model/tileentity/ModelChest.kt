package cum.xiaro.trollhack.util.graphics.fastrender.model.tileentity

import cum.xiaro.trollhack.util.graphics.fastrender.model.Model
import cum.xiaro.trollhack.util.graphics.fastrender.model.ModelBuilder

class ModelChest : Model(64, 64) {
    override fun ModelBuilder.buildModel() {
        // Below
        childModel(0.0f, 19.0f) {
            addBox(-7.0f, 0.0f, -7.0f, 14.0f, 10.0f, 14.0f)
        }

        // Knob
        childModel {
            addBox(-1.0f, 7.0f, 7.0f, 2.0f, 4.0f, 1.0f)
        }

        // Lib
        childModel {
            addBox(-7.0f, 9.0f, -7.0f, 14.0f, 5.0f, 14.0f)
        }
    }
}