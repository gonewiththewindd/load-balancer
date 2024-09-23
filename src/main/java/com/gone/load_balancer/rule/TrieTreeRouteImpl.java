package com.gone.load_balancer.rule;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.gone.load_balancer.common.Constants.*;

/**
 * 前缀树（最长前缀匹配）
 * TODO：支持双通配符**的规则配置和解析（代表着任意长度）
 */
public class TrieTreeRouteImpl implements Route {

    private TrieTreeNode root = new TrieTreeNode();

    @Override
    public void insert(String path, String upstream) {
        TrieTreeNode current = root;
        String[] segments = path.split(PATH_SEPARATOR);
        for (int i = 0; i < segments.length; i++) {
            if (StringUtils.isBlank(segments[i])) {
                continue;
            }
            // 搜索共同路径节点
            TrieTreeNode commonPathNode = lookupCommonPath(current, segments[i]);
            if (Objects.isNull(commonPathNode)) {
                // 共同路径未匹配创建分叉节点
                appendRestAsChild(current, segments, i, upstream);
                break;
            } else {
                current = commonPathNode;
            }
        }
    }

    private void appendRestAsChild(TrieTreeNode current, String[] segments, int i, String upstream) {
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
                .filter(n -> !SINGLE_WILDCARDS.equals(n.getVal()))
                .findFirst();
        if (nonWildcardNode.isPresent()) {
            return nonWildcardNode.get();
        }
        return matchList
                .stream()
                .filter(n -> SINGLE_WILDCARDS.equals(n.getVal()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 共同路径节点搜索
     *
     * @param current
     * @param segment
     * @return
     */
    private TrieTreeNode lookupCommonPath(TrieTreeNode current, String segment) {
        for (int j = 0; j < current.getChildren().size(); j++) {
            TrieTreeNode node = current.getChildren().get(j);
            if (node.getVal().equals(segment)) {
                return node;
            }
        }
        return null;
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
            } else if (node.containsWildcards()) {
                // 单通配符、双通配符匹配（*，**）
                if (SINGLE_WILDCARDS.equals(node.getVal()) || DOUBLE_WILDCARDS.equals(node.getVal())) {
                    matchList.add(node);
                    continue;
                }
                // 单通配符匹配，比如select*Users, *getUser, getUser*. 多通配符？？
                int wi = node.getVal().indexOf(SINGLE_WILDCARDS);
                if (wi == 0 || (segment.length() > wi && node.getVal().substring(0, wi).equals(segment.substring(0, wi)))) { // 前缀匹配
                    if (wi == node.getVal().length() - 1) { // 无后缀：getUser*（匹配）
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
        String[] segments = path.split(PATH_SEPARATOR);
        List<TrieTreeNode> targetList = new ArrayList<>();
        List<TrieTreeNode> doubleWildcardMatchList = new ArrayList<>();
        for (int i = 0; i < segments.length; i++) {
            if (StringUtils.isBlank(segments[i])) {
                continue;
            }
            // 搜索当前所有子节点，获取到匹配的节点列表
            List<TrieTreeNode> matchList = searchTheMatchList(current, segments, i);
            if (CollectionUtils.isEmpty(matchList)) {
                return null;
            }
            doubleWildcardMatchList.forEach(n -> {

            });
            matchList.stream()
                    .filter(n -> DOUBLE_WILDCARDS.equals(n.getVal()))
                    .forEach(n -> {
                        if (CollectionUtils.isEmpty(n.getChildren())) {
                            // 如果双通配符节点没有子节点，意味着一定是匹配的
                            targetList.add(n);
                        } else {
                            doubleWildcardMatchList.add(n);
                        }
                    });
            // TODO 双通配符什么情况下会匹配失效呢？ 比如 /**/users/aaaaa
            // 在到达segments的倒数第二个节点时开始需要校验双通配符的剩余部分是否生效
            // 怎么知道segments当前的节点位置呢？通过i
            // 怎么知道当前匹配节点的深度呢？


            // 选中优先级最高的节点作为匹配节点
            TrieTreeNode bestMatchNode = bestMatch(matchList);
            current = bestMatchNode;
        }
        if (current.isLeaf()) {
            return current.getUpstream();
        }
        return null;
    }

    public static void main(String[] args) {
        Route tree = new TrieTreeRouteImpl();
        // *, select*Users, *getUser, getUser*
        tree.insert("/api/user-service/*", "user-service");
        tree.insert("/api/user-service/select*Users", "user-service");
        tree.insert("/api/user-service/selectAllUsers", "user-service");
        tree.insert("/api/user-service/selectPaymentUsers", "user-service");
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
