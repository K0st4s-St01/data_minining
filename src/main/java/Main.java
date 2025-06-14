import entities.DataRecord;
import trees.PrunedDecisionTree;
import utils.DataSetReader;

import java.io.File;
import java.util.List;


public class Main {
    public static File dataSetFile= new File("src/main/resources/dataset.xls");
    public static void main(String[] args){
        List<DataRecord> data =DataSetReader.loadData(Main.dataSetFile);
        var tree = new PrunedDecisionTree(10);
        var treeNode = tree.train(data);
        tree.printTree(treeNode,"");

    }
}
