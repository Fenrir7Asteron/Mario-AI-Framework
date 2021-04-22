package com.mycompany.app.agents.bogdanMCTS;

import java.util.ArrayList;
import java.util.HashSet;

public class MCTSEnhancements {
    public enum Enhancement {
        MIXMAX,
        PARTIAL_EXPANSION,
        LOSS_AVOIDANCE,
        TREE_REUSE,
        HARD_PRUNING,
        SAFETY_PREPRUNING,
        WU_UCT,
        AGING,
        N_GRAM_SELECTION,
    }

    public static HashSet<Integer> AvailableEnhancementMasks() {
        return findAvailableMasksRecursively(new HashSet<>(), 0, 0);
    }

    public static String enhancementsToString(int enhancementsMask) {
        StringBuilder shortNames = new StringBuilder();

        for (Enhancement enhancement : Enhancement.values()) {
            if (MaskContainsEnhancement(enhancementsMask, enhancement)) {
                shortNames.append("+").append(enhancement.toString(), 0, 3);
            }

//            switch (enhancement) {
//                case MIXMAX:
//                    shortNames.append("+MM");
//                    break;
//                case TREE_REUSE:
//                    shortNames.append("+TR");
//                    break;
//                case HARD_PRUNING:
//                    shortNames.append("+HP");
//                    break;
//                case LOSS_AVOIDANCE:
//                    shortNames.append("+LA");
//                    break;
//                case PARTIAL_EXPANSION:
//                    shortNames.append("+PA");
//                    break;
//                case SAFETY_PREPRUNING:
//                    shortNames.append("+SP");
//                    break;
//                default:
//                    break;
//            }
        }

        return shortNames.toString();
    }

    public static boolean MaskContainsEnhancement(int enhancementMask, Enhancement enhancement) {
        return (enhancementMask & (1 << enhancement.ordinal())) != 0;
    }

    public static boolean MaskContainsEnhancements(int enhancementMask, Enhancement[] enhancements) {
        return (enhancementMask & AddEnhancements(0, enhancements)) != 0;
    }

    public static int AddEnhancement(int enhancementMask, Enhancement enhancement) {
        return enhancementMask | (1 << enhancement.ordinal());
    }

    public static int AddEnhancements(int enhancementMask, Enhancement[] enhancements) {
        for (Enhancement enhancement : enhancements) {
            enhancementMask |= (1 << enhancement.ordinal());
        }

        return enhancementMask;
    }

    public static int RemoveEnhancement(int enhancementMask, Enhancement enhancement) {
        return enhancementMask & (~(1 << enhancement.ordinal()));
    }

    private static HashSet<Integer> findAvailableMasksRecursively(
            HashSet<Integer> masks, int currentMask, int enhancementIdx) {

        if (enhancementIdx >= Enhancement.values().length
                || !maskValid(currentMask)) {
            return masks;
        }

        masks.add(currentMask);

        // Add enhancement in current index.
        findAvailableMasksRecursively(
                masks,
                AddEnhancement(currentMask, Enhancement.values()[enhancementIdx]),
                enhancementIdx + 1
        );

        // Not add enhancement in current index.
        findAvailableMasksRecursively(masks, currentMask, enhancementIdx + 1);

        return masks;
    }

    private static boolean maskValid(int enhancementMask) {
        if (MaskContainsEnhancements(enhancementMask, new Enhancement[] {
                Enhancement.PARTIAL_EXPANSION,
                Enhancement.HARD_PRUNING,
        })) {
            return false;
        }

        if (MaskContainsEnhancement(enhancementMask, Enhancement.AGING)
                && !MaskContainsEnhancement(enhancementMask, Enhancement.TREE_REUSE)){
            return false;
        }

        return true;
    }
}
