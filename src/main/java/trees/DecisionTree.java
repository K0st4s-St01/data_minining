package trees;

import entities.DataRecord;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
public class DecisionTree {
    private final int maxDepth;
    private long timeStarted;
    private long timeEnded;

    public DecisionTree(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public TreeNode train(List<DataRecord> data) {
        timeStarted = System.currentTimeMillis();
        log.info("starting training {}",new Date(timeStarted));
        List<String> attributes = new ArrayList<>(data.get(0).getAttributes().keySet());
        var treeNode=buildTree(data, attributes, 0);
        timeEnded = System.currentTimeMillis();
        log.info("training ended {}",new Date(timeEnded));
        log.info("time {}",(timeEnded-timeStarted)/1000);
        return treeNode;
    }


    private TreeNode buildTree(List<DataRecord> data, List<String> attributes, int depth) {
        TreeNode node = new TreeNode();

        if (depth == maxDepth || allSameClass(data)) {
            node.setY(majorityLabel(data));
            return node;
        }

        double baseEntropy = entropy(data);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<Future<SplitResult>> futures = new ArrayList<>();
        for (String attr : attributes) {
            futures.add(executor.submit(() -> evaluateAttributeSplit(data, attr, baseEntropy)));
        }

        executor.shutdown();

        SplitResult bestSplit = null;
        double bestGain = -1;

        for (Future<SplitResult> future : futures) {
            try {
                SplitResult split = future.get();
                if (split != null && split.gain > bestGain) {
                    bestGain = split.gain;
                    bestSplit = split;
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        if (bestSplit == null || bestSplit.left.isEmpty() || bestSplit.right.isEmpty()) {
            node.setY(majorityLabel(data));
            return node;
        }

        node.setAttribute(bestSplit.attr);
        node.setThreshold((long) bestSplit.threshold);
        node.setLeft(buildTree(bestSplit.left, attributes, depth + 1));
        node.setRight(buildTree(bestSplit.right, attributes, depth + 1));

        return node;
    }

    private SplitResult evaluateAttributeSplit(List<DataRecord> data, String attr, double baseEntropy) {
        Set<Long> uniqueValues = new TreeSet<>();
        for (DataRecord r : data) uniqueValues.add(r.getAttributes().get(attr));
        List<Long> values = new ArrayList<>(uniqueValues);

        double bestGain = -1;
        double bestThreshold = 0;
        List<DataRecord> bestLeft = new ArrayList<>();
        List<DataRecord> bestRight = new ArrayList<>();
        int size = values.size();
        for (int i = 1; i < size; i++) {
            log.info("{} {}/{}",attr,i,size);
            double threshold = (values.get(i - 1) + values.get(i)) / 2.0;
            List<DataRecord> left = new ArrayList<>();
            List<DataRecord> right = new ArrayList<>();

            for (DataRecord r : data) {
                if (r.getAttributes().get(attr) <= threshold) left.add(r);
                else right.add(r);
            }

            if (left.isEmpty() || right.isEmpty()) continue;

            double gain = baseEntropy -
                    ((double) left.size() / data.size()) * entropy(left) -
                    ((double) right.size() / data.size()) * entropy(right);

            if (gain > bestGain) {
                bestGain = gain;
                bestThreshold = threshold;
                bestLeft = left;
                bestRight = right;
            }
        }

        return bestGain > 0 ? new SplitResult(attr, bestThreshold, bestGain, bestLeft, bestRight) : null;
    }

    private boolean allSameClass(List<DataRecord> data) {
        Long first = data.get(0).getY();
        return data.stream().allMatch(r -> r.getY().equals(first));
    }

    private Long majorityLabel(List<DataRecord> data) {
        return data.stream()
                .collect(Collectors.groupingBy(DataRecord::getY, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .get().getKey();
    }

    private double entropy(List<DataRecord> data) {
        Map<Long, Long> freq = data.stream()
                .collect(Collectors.groupingBy(DataRecord::getY, Collectors.counting()));

        double result = 0.0;
        for (long count : freq.values()) {
            double p = (double) count / data.size();
            result -= p * (Math.log(p) / Math.log(2));
        }
        return result;
    }

    public Long predict(TreeNode node, DataRecord r) {
        if (node.getLeft() == null && node.getRight() == null) return node.getY();
        if (r.getAttributes().get(node.getAttribute()) <= node.getThreshold())
            return predict(node.getLeft(), r);
        else
            return predict(node.getRight(), r);
    }

    public void printTree(TreeNode node, String indent) {
        if (node.getLeft() == null && node.getRight() == null) {
            System.out.println(indent + "Leaf: " + node.getY());
            return;
        }
        log.info("{}[IF {} <= {}]", indent, node.getAttribute(), node.getThreshold());
        printTree(node.getLeft(), indent + "  ");
        log.info("{}[ELSE {} > {}]", indent, node.getAttribute(), node.getThreshold());
        printTree(node.getRight(), indent + "  ");
    }

    private static class SplitResult {
        String attr;
        double threshold;
        double gain;
        List<DataRecord> left;
        List<DataRecord> right;

        public SplitResult(String attr, double threshold, double gain,
                           List<DataRecord> left, List<DataRecord> right) {
            this.attr = attr;
            this.threshold = threshold;
            this.gain = gain;
            this.left = left;
            this.right = right;
        }
    }
}
