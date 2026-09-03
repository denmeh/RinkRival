package com.github.denmeh.npcaitest.bt;

public final class Trees {

    private static final int MAX_DEPTH = 16;

    private Trees() {
    }

    /**
     * Walks from the root down through whichever child each composite ran, giving a live description of
     * what the tree is doing, e.g. {@code rival>defend>GUARD_NET}. Nodes with an empty name, such as the
     * plumbing decorators, are left out. Replaces having to hand-label every node from inside its own
     * tick, which only ever tells you about one node at a time.
     */
    public static String activePath(Node root) {
        StringBuilder path = new StringBuilder();
        Node node = root;
        for (int depth = 0; node != null && depth < MAX_DEPTH; depth++) {
            String name = node.name();
            if (!name.isEmpty()) {
                if (!path.isEmpty()) {
                    path.append('>');
                }
                path.append(name);
            }
            node = node.activeChild();
        }
        return path.isEmpty() ? "NONE" : path.toString();
    }
}
