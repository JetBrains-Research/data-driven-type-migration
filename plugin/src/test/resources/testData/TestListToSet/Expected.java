import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ListToSetTest {
    public Set<String> getData() {
        List<String> data = new LinkedList<String>();
        data.add("");
        var tmp = data.get(0);
        return data.stream().filter(it -> it.contains("haha")).collect(Collectors.toSet());
    }

    public void main() {
        Set<String> a = getData();
        System.out.print(a.iterator().next());
    }
}