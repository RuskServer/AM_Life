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
    /**
     * MultiDiscrete対応の推論 (ONNXモデルが出力を2つ返す場合)
     */
    public int[] inferMulti(float[] input) {
        try {
            OnnxTensor tensor = OnnxTensor.createTensor(env, new float[][]{input});
            OrtSession.Result result = session.run(Collections.singletonMap("state", tensor));

            // 出力0 = move_head、出力1 = combat_head
            float[][] moveOutput = (float[][]) result.get(0).getValue();
            float[][] combatOutput = (float[][]) result.get(1).getValue();

            float[] moveQ = moveOutput[0];
            float[] combatQ = combatOutput[0];

            System.out.println("moveQ:   " + Arrays.toString(moveQ));
            System.out.println("combatQ: " + Arrays.toString(combatQ));

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
