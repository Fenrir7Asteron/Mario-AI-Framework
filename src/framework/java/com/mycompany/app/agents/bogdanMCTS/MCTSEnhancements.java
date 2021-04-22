package com.mycompany.app.agents.bogdanMCTS;

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

    public static String enhancementsToString(int enhancementsMask) {
        StringBuilder shortNames = new StringBuilder();

        for (Enhancement enhancement : Enhancement.values()) {
            if (MaskContainsEnhancement(enhancementsMask, enhancement)) {
                shortNames.append("+").append(enhancement.toString(), 0, 2);
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

    public static int AddEnhancement(int enhancementMask, Enhancement enhancement) {
        return enhancementMask | (1 << enhancement.ordinal());
    }

    public static int AddEnhancements(int enhancementMask, Enhancement[] enhancements) {
        for (Enhancement enhancement : enhancements) {
            enhancementMask |= (1 << enhancement.ordinal());
        }

        return enhancementMask;
    }
}
