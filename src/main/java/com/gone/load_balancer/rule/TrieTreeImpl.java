package com.gone.load_balancer.rule;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 前缀树（最长前缀匹配）
 * TODO：支持双通配符**的规则配置和解析（代表着任意长度）
 */
public class TrieTreeImpl implements TrieTree {

    public static final String SPLIT_CHARACTER = "/";
    public static final String WILDCARDS = "*";

    private TrieTreeNode root = new TrieTreeNode();

    @Override
    public void insert(String path, String upstream) {
        TrieTreeNode current = root;
        String[] segments = path.split(SPLIT_CHARACTER);
        for (int i = 0; i < segments.length; i++) {
            if (StringUtils.isBlank(segments[i])) {
                continue;
            }
            // 搜索当前所有子节点，获取到匹配的节点列表
            List<TrieTreeNode> matchList = searchTheMatchList(current, segments, i);
            if (CollectionUtils.isEmpty(matchList)) {
                // 共同路径未匹配创建分叉节点
                appendChild(current, segments, i, upstream);
                break;
            } else {
                // 选中优先级最高的节点作为匹配节点
                TrieTreeNode bestMatchNode = bestMatch(matchList);
                if (Objects.nonNull(bestMatchNode) && WILDCARDS.equals(bestMatchNode.getVal())) {
                    // 节点本身为通配符的情况下，应该视为同级，创建分叉节点
                    appendChild(current, segments, i, upstream);
                    break;
                }
                current = bestMatchNode;
            }
        }
    }

    private void appendChild(TrieTreeNode current, String[] segments, int i, String upstream) {
        boolean isLeaf = i == segments.length - 1;
        TrieTreeNode son = TrieTreeNode.of(segments[i], new ArrayList<>(), isLeaf, upstream);
        if (!isLeaf) { // 递归构造子节点
            recurseCreateChild(son, segments, i + 1, upstream);
        }
        current.getChildren().add(son);
    }

    /**
     * 优先级：非通配符 > 含通配符 > 单通配符
     *
     * @param matchList
     * @return
     */
    private TrieTreeNode bestMatch(List<TrieTreeNode> matchList) {
        if (matchList.size() == 1) {
            return matchList.get(0);
        }
        Optional<TrieTreeNode> fullMatchNode = matchList.stream()
                .filter(n -> !n.containsWildcards())
                .findFirst();
        if (fullMatchNode.isPresent()) {
            return fullMatchNode.get();
        }
        Optional<TrieTreeNode> nonWildcardNode = matchList
                .stream()
                .filter(n -> !WILDCARDS.equals(n.getVal()))
                .findFirst();
        if (nonWildcardNode.isPresent()) {
            return nonWildcardNode.get();
        }
        return matchList
                .stream()
                .filter(n -> WILDCARDS.equals(n.getVal()))
                .findFirst()
                .orElse(null);
    }

    private List<TrieTreeNode> searchTheMatchList(TrieTreeNode current, String[] segments, int i) {
        List<TrieTreeNode> matchList = new ArrayList<>();
        int j = 0, size = current.getChildren().size();
        String segment = segments[i];
        for (; j < size; j++) {
            // 共同路径匹配
            TrieTreeNode node = current.getChildren().get(j);
            if (node.getVal().equals(segment)) { // 全匹配
                matchList.add(node);
            } else if (node.containsWildcards()) {// 单通配符匹配，比如 *, select*Users, *getUser, getUser*. 多通配符？？
                int wi = node.getVal().indexOf(WILDCARDS);
                if (wi == 0 || (segment.length() > wi && node.getVal().substring(0, wi).equals(segment.substring(0, wi)))) { // 前缀匹配
                    if (wi == node.getVal().length() - 1) { // 无后缀：*, getUser*（匹配）
                        matchList.add(node);
                        continue;
                    }
                    String suffix = node.getVal().substring(wi + 1);
                    if (segment.endsWith(suffix)) {// 有后缀：select*Users, *getUser（匹配）
                        matchList.add(node);
                    }
                }
            }
        }
        return matchList;
    }

    private void recurseCreateChild(TrieTreeNode son, String[] segments, int i, String upstream) {
        if (i == segments.length) {
            return;
        }
        TrieTreeNode e = TrieTreeNode.of(segments[i], new ArrayList<>(), i == segments.length - 1, upstream);
        son.getChildren().add(e);
        recurseCreateChild(e, segments, i + 1, upstream);
    }

    @Override
    public String search(String path) {
        TrieTreeNode current = root;
        String[] segments = path.split(SPLIT_CHARACTER);
        for (int i = 0; i < segments.length; i++) {
            if (StringUtils.isBlank(segments[i])) {
                continue;
            }
            // 搜索当前所有子节点，获取到匹配的节点列表
            List<TrieTreeNode> matchList = searchTheMatchList(current, segments, i);
            if (CollectionUtils.isEmpty(matchList)) {
                return "404";
            }
            // 选中优先级最高的节点作为匹配节点
            TrieTreeNode bestMatchNode = bestMatch(matchList);
            current = bestMatchNode;
        }
        if (current.isLeaf()) {
            return current.getUpstream();
        }
        return "404";
    }

    public static void main(String[] args) {
        TrieTree tree = new TrieTreeImpl();
        // *, select*Users, *getUser, getUser*
        tree.insert("/api/user-service/*", "user-service");
        tree.insert("/api/user-service/select*Users", "user-service");
        tree.insert("/api/user-service/*getUser", "user-service");
        tree.insert("/api/user-service/getUser*", "user-service");

        int times = 1000000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            tree.search("/api/user-service/selectAllUsers");
            tree.search("/api/user-service/getUsername");
            tree.search("/api/user-service/getUserAccount/404");
            tree.search("/api/device-service/xxx");
        }
        System.out.println(String.format("search route %s times, end in %s ms", times, System.currentTimeMillis() - start));

        System.out.println();
    }
}
