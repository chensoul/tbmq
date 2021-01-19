/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.mqtt.broker.service.subscription;

import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.mqtt.broker.constant.BrokerConstants;
import org.thingsboard.mqtt.broker.service.stats.StatsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConcurrentMapSubscriptionTrie<T> implements SubscriptionTrie<T> {
    private final AtomicInteger size;
    private final Node<T> root = new Node<>();

    public ConcurrentMapSubscriptionTrie(StatsManager statsManager) {
        this.size = statsManager.createSubscriptionSizeCounter();
    }

    private static class Node<T> {
        private final ConcurrentMap<String, Node<T>> children = new ConcurrentHashMap<>();
        private final Set<T> values = Sets.newConcurrentHashSet();

        public Node() {
        }
    }

    @Override
    public List<T> get(String topic) {
        if (topic == null) {
            throw new IllegalArgumentException("Topic cannot be null");
        }
        List<T> result = new ArrayList<>();
        Stack<TopicPosition<T>> topicPositions = new Stack<>();
        topicPositions.add(new TopicPosition<>(0, root));

        while (!topicPositions.isEmpty()) {
            TopicPosition<T> topicPosition = topicPositions.pop();
            if (topicPosition.prevDelimiterIndex >= topic.length()) {
                result.addAll(topicPosition.node.values);
                continue;
            }
            ConcurrentMap<String, Node<T>> childNodes = topicPosition.node.children;
            String segment = getSegment(topic, topicPosition.prevDelimiterIndex);
            int nextDelimiterIndex = topicPosition.prevDelimiterIndex + segment.length() + 1;

            if (notStartingWith$(topic, topicPosition)) {
                Node<T> multiLevelWildcardSubs = childNodes.get(BrokerConstants.MULTI_LEVEL_WILDCARD);
                if (multiLevelWildcardSubs != null) {
                    result.addAll(multiLevelWildcardSubs.values);
                }
                Node<T> singleLevelWildcardSubs = childNodes.get(BrokerConstants.SINGLE_LEVEL_WILDCARD);
                if (singleLevelWildcardSubs != null) {
                    topicPositions.add(new TopicPosition<>(nextDelimiterIndex, singleLevelWildcardSubs));
                }
            }

            Node<T> segmentNode = childNodes.get(segment);
            if (segmentNode != null) {
                topicPositions.add(new TopicPosition<>(nextDelimiterIndex, segmentNode));
            }
        }
        return result;
    }

    private boolean notStartingWith$(String topic, TopicPosition<T> topicPosition) {
        return topicPosition.prevDelimiterIndex != 0 || topic.charAt(0) != '$';
    }

    @AllArgsConstructor
    private static class TopicPosition<T> {
        private final int prevDelimiterIndex;
        private final Node<T> node;
    }

    @Override
    public void put(String topicFilter, T val) {
        if (topicFilter == null || val == null) {
            throw new IllegalArgumentException("Topic filter or value cannot be null");
        }
        put(root, topicFilter, val, 0);
    }

    private void put(Node<T> x, String key, T val, int prevDelimiterIndex) {
        if (prevDelimiterIndex >= key.length()) {
            addOrReplace(x.values, val);
        } else {
            String segment = getSegment(key, prevDelimiterIndex);
            Node<T> nextNode = x.children.computeIfAbsent(segment, s -> new Node<>());
            put(nextNode, key, val, prevDelimiterIndex + segment.length() + 1);
        }
    }

    private void addOrReplace(Set<T> values, T val) {
        if (!values.add(val)) {
            values.remove(val);
            values.add(val);
        } else {
            size.getAndIncrement();
        }
    }

    @Override
    public boolean delete(String topicFilter, Predicate<T> deletionFilter) {
        if (topicFilter == null || deletionFilter == null) {
            throw new IllegalArgumentException("Topic filter or deletionFilter cannot be null");
        }
        Node<T> x = getNode(root, topicFilter, 0);
        if (x != null) {
            List<T> valuesToDelete = x.values.stream().filter(deletionFilter).collect(Collectors.toList());
            if (valuesToDelete.isEmpty()) {
                return false;
            }
            if (valuesToDelete.size() > 1) {
                log.error("There are more than one value to delete!");
            }
            boolean deleted = x.values.removeAll(valuesToDelete);
            if (deleted) {
                size.getAndSet(size.get() - valuesToDelete.size());
            }
            return deleted;
        }
        return false;
    }

    private Node<T> getNode(Node<T> x, String key, int prevDelimiterIndex) {
        if (x == null) return null;
        if (prevDelimiterIndex >= key.length()) {
            return x;
        }
        String segment = getSegment(key, prevDelimiterIndex);
        return getNode(x.children.get(segment), key, prevDelimiterIndex + segment.length() + 1);
    }

    private String getSegment(String key, int prevDelimiterIndex) {
        int nextDelimitedIndex = key.indexOf(BrokerConstants.TOPIC_DELIMITER, prevDelimiterIndex);

        return nextDelimitedIndex == -1 ?
                key.substring(prevDelimiterIndex)
                : key.substring(prevDelimiterIndex, nextDelimitedIndex);
    }
}