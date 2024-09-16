public class Entry {
    String name;
    String type;
    String kind;
    int index;

    private Entry(String name, String type, String kind, int index){
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.index = index;
    }

    public static Entry entryStatic(String name,String type,int index){
        return new Entry(name,type,"static",index);
    }

    public static Entry entryField(String name, String type, int index){
        return new Entry(name,type,"field",index);
    }

    public static Entry entryArgument(String name, String type, int index){
        return new Entry(name,type,"argument",index);
    }

    public static Entry entryLocal(String name, String type, int index){
        return new Entry(name,type,"local",index);
    }
}
