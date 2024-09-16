public enum Segments {
    CONST(0),
    ARG(1),
    LOCAL(2),
    STATIC(3),
    THIS(4),
    THAT(5),
    POINTER(6),
    TEMP(7);

    int key;
    Segments(int key){
        this.key = key;
    }

}
