import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SymbolTable {
    private final List<Entry> entries = new ArrayList<>();
    private Integer fieldCounter = 0;
    private Integer staticCounter = 0;
    private Integer argumentCounter = 0;
    private Integer localCounter = 0;

    public void addEntry(List<EntryBuilder> entryBuilders){
        for(EntryBuilder entryBuilder : entryBuilders){
            switch (entryBuilder.getKind()){
                case "argument" -> entryBuilder.index(argumentCounter++);
                case "local" -> entryBuilder.index(localCounter++);
                case "static" -> entryBuilder.index(staticCounter++);
                case "field" -> entryBuilder.index(fieldCounter++);
            }
            entries.add(entryBuilder.build());
        }
    }


    public String retrieveFields(){
        String s = "";
        return s + entries.stream().map(x -> x.kind).filter(x -> x.equals("field")).count();
    }

    public int retrieveLocalVariables(){
        return (int)entries.stream().map(x -> x.kind).filter(x -> x.equals("local")).count();
    }

    public int retrieveArgs(){
        return (int)entries.stream().map(x -> x.kind).filter(x -> x.equals("argument")).count();
    }

    // looks if there is a hit -> if so return the Entry -> else optional.empty
    public Optional<Entry> hit(String name){
        return entries.stream().filter(x -> x.name.equals(name)).findAny();
    }

}
