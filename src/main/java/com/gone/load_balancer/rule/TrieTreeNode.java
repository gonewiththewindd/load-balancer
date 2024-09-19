package com.gone.load_balancer.rule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(chain = true)
public class TrieTreeNode {

    private String val;
    private List<TrieTreeNode> children = new ArrayList<>();
    private boolean isLeaf;
    private String upstream;

    public boolean containsWildcards() {
        return this.val.contains(TrieTreeRouteImpl.SINGLE_WILDCARDS);
    }

    public static TrieTreeNode of(String val, List<TrieTreeNode> children, boolean isLeaf, String upstream) {
        return new TrieTreeNode(val, children, isLeaf, upstream);
    }
}
