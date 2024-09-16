public class EntryBuilder {

    private String name;
    private String type;
    private String kind;
    private int index;

    public void name(String name){
        this.name = reduce(name);
    }

    public void type(String type){
        this.type = reduce(type);
    }

    public void kind(String kind){
        this.kind = reduce(kind);
    }

    public void index(int index){
        this.index = index;
    }

    // eliminateSymbols -> only value should remain
    public String reduce(String s){
        if(s.split(" ").length == 1){
            return s;
        }
        return s.strip().split("(<)[a-z]*(>)|(</)[a-z]*(>)")[1].strip();
    }

    public Entry build(){
        return switch (kind) {
            case "field" -> Entry.entryField(name, type, index);
            case "argument" -> Entry.entryArgument(name, type, index);
            case "local" -> Entry.entryLocal(name, type, index);
            case "static" -> Entry.entryStatic(name, type, index);
            default -> null;
        };
    }

    public String getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
