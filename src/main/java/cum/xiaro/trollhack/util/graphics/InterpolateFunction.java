package cum.xiaro.trollhack.util.graphics;

@FunctionalInterface
public interface InterpolateFunction {
    float invoke(long time, float prev, float current);
}
