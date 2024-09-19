package com.gone.load_balancer.rule;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 哈希+正则表达式
 */
public class HashRegexRouteImpl implements Route {

    public static final String SPLIT_CHARACTER = "/";
    public static final String SINGLE_WILDCARDS = "*";
    public static final String SINGLE_WILDCARDS_REPLACEMENT = "<-!->";
    public static final String DOUBLE_WILDCARDS = "**";
    public static final String DOUBLE_WILDCARDS_REPLACEMENT = "<-!--!->";

    private Map<String, KeyValue<String, String>> map = new HashMap<>();
    private Map<Pattern, KeyValue<String, String>> regexMap = new HashMap<>();


    @Override
    public void insert(String path, String upstream) {
        KeyValue keyValue = new DefaultKeyValue(path, upstream);
        if (path.contains(SINGLE_WILDCARDS)) {
            path = path.replace(DOUBLE_WILDCARDS, DOUBLE_WILDCARDS_REPLACEMENT)
                    .replace(SINGLE_WILDCARDS, SINGLE_WILDCARDS_REPLACEMENT);
            path = path.replace(DOUBLE_WILDCARDS_REPLACEMENT, "((.*/)*.*)")
                    .replace(SINGLE_WILDCARDS_REPLACEMENT, "[^/]*");
            regexMap.put(Pattern.compile(path), keyValue);
        } else {
            map.put(path, keyValue);
        }
    }

    /**
     * 优先级：非通配符 > 含通配符 > 单通配符 > 双通配符
     *
     * @param matchList
     * @return
     */
    private String bestGuess(List<KeyValue<String, String>> matchList) {
        if (CollectionUtils.isEmpty(matchList)) {
            return null;
        }
        if (matchList.size() == 1) {
            return matchList.get(0).getKey();
        }
        // 非通配符
        Optional<KeyValue<String, String>> fullMatchNode = matchList.stream()
                .filter(kv -> kv.getKey().indexOf(SINGLE_WILDCARDS) < 0)
                .findFirst();
        if (fullMatchNode.isPresent()) {
            return fullMatchNode.get().getKey();
        }
        // 含通配符
        Optional<KeyValue<String, String>> nonWildcardNode = matchList.stream()
                .filter(kv -> segmentNotAllWildcard(kv.getKey()))
                .findFirst();
        if (nonWildcardNode.isPresent()) {
            return nonWildcardNode.get().getKey();
        }
        // 单通配符，越靠近末尾越优先
        Optional<Triple<Boolean, Integer, KeyValue<String, String>>> bestGuess = matchList.stream()
                .map(kv -> segmentEqual(kv, SINGLE_WILDCARDS))
                .filter(triple -> triple.getLeft())
                .max(Comparator.comparing(Triple::getMiddle));
        if (bestGuess.isPresent()) {
            return bestGuess.get().getRight().getKey();
        }
        // 双通配符，越靠近末尾越优先
        return matchList.stream()
                .map(kv -> segmentEqual(kv, DOUBLE_WILDCARDS))
                .filter(triple -> triple.getLeft())
                .max(Comparator.comparing(Triple::getMiddle))
                .map(triple -> triple.getRight().getKey())
                .orElse(null);
    }

    private Triple<Boolean, Integer, KeyValue<String, String>> segmentEqual(KeyValue<String, String> kv, String wildcard) {
        String[] split = kv.getKey().split("/");
        for (int i = split.length - 1; i > 0; i--) {
            if (split[i].equals(wildcard)) {
                return new ImmutableTriple<>(true, i, kv);
            }
        }
        return new ImmutableTriple<>(false, -1, kv);
    }

    private boolean segmentNotAllWildcard(String key) {
        for (String s : key.split("/")) {
            if (s.equals(SINGLE_WILDCARDS) || s.equals(DOUBLE_WILDCARDS)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String search(String path) {
        KeyValue<String, String> keyValue = map.get(path);
        if (keyValue != null) {
            return keyValue.getKey();
        }
        List<KeyValue<String, String>> matchList = regexMap.entrySet()
                .stream()
                .filter(entry -> entry.getKey().matcher(path).matches())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        return bestGuess(matchList);
    }

    public static void main(String[] args) {
        Route tree = new HashRegexRouteImpl();
        // *, select*Users, *getUser, getUser*
        tree.insert("/api/user-service/users/selectAllUsers", "user-service");
        tree.insert("/api/user-service/users/select*Users", "user-service");
        tree.insert("/api/user-service/users/*getUser", "user-service");
        tree.insert("/api/user-service/users/getUser*", "user-service");
        tree.insert("/api/*/users/*", "user-service");
        tree.insert("/api/*/users/*/get", "user-service");
        tree.insert("/api/user-service/**/", "user-service");
        tree.insert("/api/user-service/depts/list*", "user-service");
        tree.insert("/api/**", "user-service");

        int times = 100000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            tree.search("/api/user-service/users/selectPaymentUsers");
            tree.search("/api/user-service/users/getUsername");
            tree.search("/api/user-service/users/404");
            tree.search("/api/user-service/depts/listFirst");
            tree.search("/api/device-service/xxx");
        }
        System.out.println(String.format("search route %s times, end in %s ms", times, System.currentTimeMillis() - start));

//        for (int i = 0; i < times; i++) {
//            System.out.println(tree.search("/api/user-service/users/selectPaymentUsers"));    // match /api/user-service/users/select*Users
//            System.out.println(tree.search("/api/user-service/users/getUsername"));           // match /api/user-service/users/getUser*
//            System.out.println(tree.search("/api/user-service/users/404"));                   // match /api/*/users/*
//            System.out.println(tree.search("/api/user-service/depts/listFirst"));             // match /api/user-service/depts/list*
//            System.out.println(tree.search("/api/device-service/xxx"));                       // match /api/**
//        }
    }
}
