import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ListToSetTest {
    public L<caret>ist<String> getData() {
        List<String> data = new LinkedList<String>();
        data.add("");
        var tmp = data.get(0);
        return data.stream().filter(it -> it.contains("haha")).collect(Collectors.toList());
    }

    public void main() {
        List<String> a = getData();
        System.out.print(a.get(0));
    }
}
