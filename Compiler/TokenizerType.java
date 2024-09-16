public enum TokenizerType {
    KEYWORD("keyword"),
    SYMBOL("symbol"),
    IDENTIFIER("identifier"),
    INT_CONST("integerConstant"),
    STRING_CONST("stringConstant");

    private final String name;

    TokenizerType(String value) {
        this.name = value;
    }

    public String constructT(String s){
        char c = '"';
        String v = ""+c;
        s = s.replaceAll(v,"");
        return String.format("<%s> %s </%s>\n",name,s,name);
    }
}
