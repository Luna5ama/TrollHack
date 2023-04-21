package dev.luna5ama.trollhack.util.graphics;

@FunctionalInterface
public interface InterpolateFunction {
    float invoke(long time, float prev, float current);
}
