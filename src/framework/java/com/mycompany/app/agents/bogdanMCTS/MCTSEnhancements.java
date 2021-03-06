package com.mycompany.app.agents.bogdanMCTS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MCTSEnhancements {
    public enum Enhancement {
        HARD_PRUNING,
        WU_UCT,
        TREE_REUSE,
        MIXMAX,
        N_GRAM_SELECTION,
        MACRO_ACTIONS,
        LOSS_AVOIDANCE,
        AGING,
        SP_MCTS,
        PARTIAL_EXPANSION,
        SAFETY_PREPRUNING,
        PROCRASTINATION_PUNISHER,
    }

    public static ArrayList<Integer> AvailableEnhancementMasks() {
        return new ArrayList<>(findAvailableMasksRecursively(new HashSet<>(), 0, 0));
    }

    public static String enhancementsToString(int enhancementsMask) {
        StringBuilder shortNames = new StringBuilder();

        for (Enhancement enhancement : Enhancement.values()) {
            if (MaskContainsEnhancement(enhancementsMask, enhancement)) {
                shortNames.append("+").append(enhancement.toString(), 0, 3);
            }
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

        // Remove enhancements that didn't pass sreening stage.

        return !MaskContainsEnhancement(enhancementMask, Enhancement.HARD_PRUNING)
                && !MaskContainsEnhancement(enhancementMask, Enhancement.TREE_REUSE)
                && !MaskContainsEnhancement(enhancementMask, Enhancement.N_GRAM_SELECTION)
                && !MaskContainsEnhancement(enhancementMask, Enhancement.AGING)
                && !MaskContainsEnhancement(enhancementMask, Enhancement.PARTIAL_EXPANSION)
                && !MaskContainsEnhancement(enhancementMask, Enhancement.SAFETY_PREPRUNING)
                && !MaskContainsEnhancement(enhancementMask, Enhancement.PROCRASTINATION_PUNISHER);
    }
}
