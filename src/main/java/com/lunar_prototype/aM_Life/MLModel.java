package com.lunar_prototype.aM_Life;

import ai.onnxruntime.*;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.Collections;

public class MLModel {

    private final OrtEnvironment env;
    private final OrtSession session;

    public MLModel() {
        try {
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setInterOpNumThreads(1);
            options.setIntraOpNumThreads(1);
            session = env.createSession("plugins/ScavPlugin/model.onnx", options);
        } catch (Exception e) {
            throw new RuntimeException("モデル読み込み失敗", e);
        }
    }

    public int infer(float[] input) {
        try {
            OnnxTensor tensor = OnnxTensor.createTensor(env, new float[][]{input});
            OrtSession.Result result = session.run(Collections.singletonMap("state", tensor));

            float[][] output = (float[][]) result.get(0).getValue();
            float[] logits = output[0];

            System.out.println("output state: " + Arrays.toString(output));

            int maxIndex = 0;
            for (int i = 1; i < logits.length; i++) {
                if (logits[i] > logits[maxIndex]) maxIndex = i;
            }

            return maxIndex;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}