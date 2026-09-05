package dev.dusk.rankcolors.menu;

import java.util.ArrayList;
import java.util.List;

/** Pure layout policy for centering gradient stops across the first two menu rows. */
public final class GradientSlotLayout {
    private GradientSlotLayout() {
    }

    public static List<Integer> resolve(int stopCount, List<Integer> configured) {
        if (stopCount <= 0) return List.of();
        if (configured != null && configured.size() >= stopCount) {
            return List.copyOf(configured.subList(0, stopCount));
        }
        List<Integer> standard = switch (stopCount) {
            case 2 -> List.of(10, 16);
            case 3 -> List.of(10, 13, 16);
            case 4 -> List.of(10, 12, 14, 16);
            case 5 -> List.of(10, 11, 13, 15, 16);
            default -> List.of();
        };
        if (!standard.isEmpty()) return standard;

        List<Integer> generated = new ArrayList<>(stopCount);
        int firstRowCount = Math.min(9, stopCount);
        int firstStart = 9 + (9 - firstRowCount) / 2;
        for (int index = 0; index < firstRowCount; index++) generated.add(firstStart + index);
        int secondRowCount = Math.min(9, stopCount - firstRowCount);
        int secondStart = 18 + (9 - secondRowCount) / 2;
        for (int index = 0; index < secondRowCount; index++) generated.add(secondStart + index);
        return List.copyOf(generated);
    }
}
