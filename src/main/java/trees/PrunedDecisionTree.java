package trees;

import com.sun.source.tree.Tree;
import entities.DataRecord;
import lombok.extern.slf4j.Slf4j;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
public class PrunedDecisionTree {
    private Integer maxDepth;

    public PrunedDecisionTree(Integer maxDepth) {
        this.maxDepth = maxDepth;
    }

    public TreeNode train(List<DataRecord> data ){
        List<String> attributes = data.get(0).getAttributes().keySet().parallelStream().toList();
        log.info("training ... attr = {}",attributes);
        return buildTree(data,attributes,0);
    }

    private TreeNode buildTree(List<DataRecord> data, List<String> attributes, int depth ) {
        TreeNode node = new TreeNode();
        if (depth == maxDepth){
            node.setY(majorityLabel(data));
            return node;
        }
        double bestGain = -1;
        String bestAttr = null;
        double bestThreshold = 0;
        List<DataRecord> bestLeft = new ArrayList<>();
        List<DataRecord> bestRight = new ArrayList<>();

        double baseEntropy = entropy(data);

        for (String attr : attributes) {
            List<Long> values = data.stream().map(r -> r.getAttributes().get(attr)).distinct().sorted().toList();

            for (int i = 1; i < values.size(); i++) {
                log.info("{} -> {}/{}",attr,i,values.size());
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
                    bestAttr = attr;
                    bestThreshold = threshold;
                    bestLeft = left;
                    bestRight = right;
                }
            }
        }

        if (bestGain == -1) {
            node.setY( majorityLabel(data));
            return node;
        }

        node.setAttribute (bestAttr);
        node.setThreshold((long) bestThreshold);
        log.info("best attr =  {}",bestAttr);
        node.setLeft(buildTree(bestLeft, attributes, depth + 1));
        node.setRight( buildTree(bestRight, attributes, depth + 1));

        return node;
    }

    private Long majorityLabel(List<DataRecord> data) {
        return data.stream()
                .collect(Collectors.groupingBy(classifier -> classifier.getY(),Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue()).get().getKey();
    }

    private double entropy(List<DataRecord> data){
        Map<Long,Long> freq = data.stream().collect(Collectors.groupingBy(r -> r.getY(),Collectors.counting()));
        double result = 0.0;
        for (long count : freq.values()){
            double p = (double) count / data.size();
            result -= p*(Math.log(p)/Math.log(2));
        }
        return result;
    }
    public Long predict(TreeNode node, DataRecord r) {
        if (node.getLeft()==null && node.getRight()==null) return node.getY();
        if (r.getAttributes().get(node.getAttribute()) <= node.getThreshold())
            return predict(node.getLeft(), r);
        else
            return predict(node.getRight(), r);
    }
    public void printTree(TreeNode node, String indent) {
        if (node.getLeft() ==null && node.getRight()==null) {
            System.out.println(indent + "Leaf: " + node.getY());
            return;
        }

        log.info("{}[IF {} <= {}] {}", indent, node.getAttribute(), node.getThreshold());
        printTree(node.getLeft(), indent + "  ");
        log.info("{}[ELSE {} > {}] {}", indent, node.getAttribute(), node.getThreshold());
        printTree(node.getRight(), indent + "  ");
    }


}
