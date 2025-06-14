package entities;

import lombok.*;

import java.util.Map;
import java.util.TreeMap;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DataRecord {
    private Long id;
    private Long y;
    private Map<String,Long> attributes =new TreeMap<>();
}
