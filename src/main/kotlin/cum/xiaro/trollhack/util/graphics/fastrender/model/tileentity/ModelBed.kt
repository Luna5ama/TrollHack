package cum.xiaro.trollhack.util.graphics.fastrender.model.tileentity

import cum.xiaro.trollhack.util.graphics.fastrender.model.Model
import cum.xiaro.trollhack.util.graphics.fastrender.model.ModelBuilder

class ModelBed : Model(64, 64) {
    override fun ModelBuilder.buildModel() {
        // Head
        childModel {
            addBox(-8.0f, 3.0f, -8.0f, 16.0f, 6.0f, 16.0f)
        }

        // Leg 0
        childModel(0.0f, 44.0f) {
            addBox(-8.0f, 0.0f, -8.0f, 3.0f, 3.0f, 3.0f)
        }

        // Leg 1
        childModel(0.0f, 50.0f) {
            addBox(5.0f, 0.0f, -8.0f, 3.0f, 3.0f, 3.0f)
        }

        // Foot
        childModel(0.0f, 22.0f) {
            addBox(-8.0f, 3.0f, -8.0f, 16.0f, 6.0f, 16.0f)
        }

        // Leg 2
        childModel(12.0f, 44.0f) {
            addBox(-8.0f, 0.0f, 5.0f, 3.0f, 3.0f, 3.0f)
        }

        // Leg 3
        childModel(12.0f, 50.0f) {
            addBox(5.0f, 0.0f, 5.0f, 3.0f, 3.0f, 3.0f)
        }
    }
}