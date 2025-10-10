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

    /**
     * MultiDiscrete対応の推論
     * @param input 状態ベクトル
     * @return [move_action, combat_action]
     */
    public int[] inferMulti(float[] input) {
        try {
            OnnxTensor tensor = OnnxTensor.createTensor(env, new float[][]{input});
            OrtSession.Result result = session.run(Collections.singletonMap("state", tensor));

            // MultiDiscreteならoutputは複数のQ値配列の組になっている
            // 例: shape = [1, move_action_size + combat_action_size]
            float[][] output = (float[][]) result.get(0).getValue();
            float[] logits = output[0];

            System.out.println("output state: " + Arrays.toString(logits));

            // --- move_action と combat_action に分割 ---
            int MOVE_ACTION_SIZE = 5;   // 前進, 後退, 左, 右, 待機(0)
            int COMBAT_ACTION_SIZE = 4; // 射撃, リロード, 隠れる, 待機(0)

            float[] moveQ = Arrays.copyOfRange(logits, 0, MOVE_ACTION_SIZE);
            float[] combatQ = Arrays.copyOfRange(logits, MOVE_ACTION_SIZE, MOVE_ACTION_SIZE + COMBAT_ACTION_SIZE);

            int moveAction = argMax(moveQ);
            int combatAction = argMax(combatQ);

            return new int[]{moveAction, combatAction};
        } catch (Exception e) {
            e.printStackTrace();
            return new int[]{0, 0};
        }
    }

    private int argMax(float[] array) {
        int maxIndex = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[maxIndex]) maxIndex = i;
        }
        return maxIndex;
    }
}
