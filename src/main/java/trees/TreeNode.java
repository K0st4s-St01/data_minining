package trees;



import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;
import java.util.TreeMap;
@NoArgsConstructor
@ToString
@Setter
@Getter

public class TreeNode {
    private Long id;
    private Long y;
    private Long threshold;
    private String attribute;
    private TreeNode left=null;
    private TreeNode right=null;

}
