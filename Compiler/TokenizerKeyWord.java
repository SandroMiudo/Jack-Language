public enum TokenizerKeyWord {
    CLASS,
    METHOD,
    FUNCTION,
    CONSTRUCTOR,
    INT,
    BOOLEAN,
    CHAR,
    VOID,
    VAR,
    STATIC,
    FIELD,
    LET,
    DO,
    IF, // ()
    ELSE,
    WHILE, // ()
    RETURN,
    TRUE,
    FALSE,
    NULL,
    THIS;

    // lowercase
    public String getName(){
        return this.name().toLowerCase();
    }
}
