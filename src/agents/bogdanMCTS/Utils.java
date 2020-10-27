package agents.bogdanMCTS;

import engine.helper.MarioActions;

import java.util.ArrayList;

public class Utils {

//    static MarioActions[][] availableActions = new MarioActions[][] {
//        new MarioActions[]{MarioActions.RIGHT},
//        new MarioActions[]{MarioActions.RIGHT, MarioActions.SPEED},
//        new MarioActions[]{MarioActions.RIGHT, MarioActions.JUMP},
//        new MarioActions[]{MarioActions.RIGHT, MarioActions.JUMP, MarioActions.SPEED},
//        new MarioActions[]{MarioActions.RIGHT, MarioActions.JUMP, MarioActions.SPEED},
//        new MarioActions[]{MarioActions.LEFT},
//        new MarioActions[]{MarioActions.LEFT, MarioActions.SPEED},
//        new MarioActions[]{MarioActions.LEFT, MarioActions.JUMP},
//        new MarioActions[]{MarioActions.LEFT, MarioActions.JUMP, MarioActions.SPEED},
//    };

    // Left, Right, Down, Speed, Jump
    static boolean[][] availableActions = new boolean[][] {
        new boolean[] {false, true, false, false, false},
        new boolean[] {false, true, false, true, false},
        new boolean[] {false, true, false, false, true},
        new boolean[] {false, true, false, true, true},
        new boolean[] {true, false, false, false, false},
        new boolean[] {true, false, false, true, false},
        new boolean[] {true, false, false, false, true},
        new boolean[] {true, false, false, true, true},
    };

//    public static boolean[] getAction(int ind) {
//        if (ind < 0 || ind > availableActions.length) {
//            return null;
//        }
//        boolean[] action = new boolean[MarioActions.numberOfActions()];
//        for (MarioActions button : availableActions[ind]) {
//            action[button.getValue()] = true;
//        }
//        return action;
//    }
}
