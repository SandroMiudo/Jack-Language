import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Parser {
    private final List<String> lines = new ArrayList<>();
    private List<EntryBuilder> entryBuilders = new ArrayList<>();
    private Integer POINTER = 0;
    private Integer DEPTH = 0;
    private final String KEYWORD = "<keyword>";
    private final String SYMBOL = "<symbol>";
    private final String INT_CONSTANT = "<integerConstant>";
    private final String STRING_CONSTANT = "<stringConstant>";
    private final String IDENTIFIER = "<identifier>";
    private String className;
    private String methodName;
    private String variable;
    private boolean constructor;
    private boolean activeCall = false;
    private Boolean enableMethod = false;
    private final BufferedInputStream inputStream;
    private final FileOutputStream fileOutputStream;
    private final VMCodeGenerator vmCodeGenerator;
    private final List<SymbolTable> symbolTables;

    // Token Stream
    public Parser(BufferedInputStream inputStream , FileOutputStream fileOutputStream,
                  VMCodeGenerator vmCodeGenerator, List<SymbolTable> symbolTable){
        this.inputStream = inputStream;
        this.fileOutputStream = fileOutputStream;
        this.vmCodeGenerator = vmCodeGenerator;
        this.symbolTables = symbolTable;
    }

    public void readTokens() throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while((line = bufferedReader.readLine()) != null){
            if(ignoreTokenKeyWord(line)){
                continue;
            }
            lines.add(line);
        }
        checkClass(nextToken());
    }

    private String nextToken(){
        String s = lines.get(POINTER);
        POINTER++;
        return s;
    }

    private String currentToken(){
        return lines.get(POINTER-1);
    }

    private void revertToken(int i){
        POINTER -= i;
    }


    private void checkClass(String line) throws Exception {
        if(!Pattern.compile("(class)").matcher(line).find()){
            throw new Exception("Class Keyword has to be defined!");
        }
        writeOutput("<class>");
        DEPTH++;
        writeOutput(line);
        String s = nextToken();
        if(!s.startsWith(IDENTIFIER)){
            throw new Exception("");
        }
        className = s.strip().split("(<identifier>)|(</identifier>)")[1].strip();
        writeOutput(s);
        s = nextToken();
        if(!s.startsWith(SYMBOL) || !Pattern.compile("[{]").matcher(s).find()){
            throw new Exception("");
        }
        writeOutput(s);
        s = nextToken();
        SymbolTable symbolTable = new SymbolTable();
        while(s.startsWith(KEYWORD) && Pattern.compile("(field)|(static)").matcher(s).find()){
            compileClassVarDec(symbolTable);
            s = nextToken();
        }
        symbolTables.add(symbolTable);
        while(s.startsWith(KEYWORD) && Pattern.compile("(constructor)|(function)|(method)").matcher(s).find()){
            if(Pattern.compile("(constructor)").matcher(s).find()){
                constructor = true;
            }
            compileClassSubroutineDec();
            s = nextToken();
        }
        if(!s.startsWith(SYMBOL) || !Pattern.compile("[}]").matcher(s).find()){
            throw new Exception("");
        }
        writeOutput(s);
        DEPTH--;
        writeOutput("</class>");
        // if there are still tokens remaining it can't be correct
        try{
            nextToken();
        }
        catch (IndexOutOfBoundsException indexOutOfBoundsException){
            return;
        }
        throw new Exception("");
    }

    private void writeOutput(String s) throws IOException {
        String tab = Stream.generate(() -> "\t").limit(DEPTH).reduce("",(x,y) -> x+y);
        fileOutputStream.write(String.format("%s%s\n",tab,s).getBytes());
    }

    private void compileClassVarDec(SymbolTable symbolTable) throws Exception {
        writeOutput("<classVarDec>");
        DEPTH++;
        entryBuilders = new ArrayList<>();
        EntryBuilder entryBuilder = new EntryBuilder();
        this.entryBuilders.add(entryBuilder);
        entryBuilder.kind(currentToken());
        writeOutput(currentToken());
        String token = nextToken();
        String next_Token;
        if(Pattern.compile("(int)|(char)|(boolean)").matcher(token).find() && token.startsWith(KEYWORD)){
            entryBuilder.type(token);
            writeOutput(token);
        }
        else if(token.startsWith(IDENTIFIER)){
            entryBuilder.type(token);
            writeOutput(token);
        }
        else{
            throw new Exception("");
        }
        token = nextToken();
        if(!token.startsWith(IDENTIFIER)){
            throw new Exception("");
        }
        entryBuilder.name(token);
        writeOutput(token);
        token = nextToken();
        token = potentialNextIdentifier(token);
        if(!token.startsWith(SYMBOL) || !Pattern.compile("[;]").matcher(token).find()){
            throw new Exception("");
        }
        writeOutput(token);
        DEPTH--;
        symbolTable.addEntry(this.entryBuilders);
        writeOutput("</classVarDec>");
    }

    private void isMethod(){
        if(currentToken().strip().split("(<)[a-z]*(>)|(</)[a-z]*(>)")[1].strip().equals("method")){
            enableMethod = true;
        }
    }

    private void compileClassSubroutineDec() throws Exception {
        writeOutput("<subroutineDec>");
        DEPTH++;
        writeOutput(currentToken());
        isMethod();
        SymbolTable symbolTable = new SymbolTable();
        symbolTables.add(symbolTable);
        String token = nextToken();
        if(token.startsWith(KEYWORD) && Pattern.compile("(void)|(int)|(char)|(boolean)").matcher(token).find()){
            writeOutput(token);
        }
        else if(token.startsWith(IDENTIFIER)){
            writeOutput(token);
        }
        else{
            throw new Exception("");
        }

        token = nextToken();
        if(!token.startsWith(IDENTIFIER)){
            throw new Exception("");
        }
        methodName = token.strip().split("(<identifier>)|(</identifier>)")[1].strip();
        writeOutput(token);
        token = nextToken();
        if(token.startsWith(SYMBOL) && checkCurlyBracketRight(token)){
            writeOutput(token);
        }
        else{
            throw new Exception("");
        }
        compileParameterList(symbolTable);
        token = nextToken();
        if(token.startsWith(SYMBOL) && checkCurlyBracketLeft(token)){
            writeOutput(token);
        }
        else{
            throw new Exception("");
        }
        compileSubRoutineBody(symbolTable);
        DEPTH--;
        removeLastSymbolTable();
        enableMethod = false;
        writeOutput("</subroutineDec>");
    }

    private void removeLastSymbolTable(){
        symbolTables.remove(symbolTables.size()-1);
    }

    private void constructThis(){
        EntryBuilder entryBuilder = new EntryBuilder();
        entryBuilder.name("this");
        entryBuilder.kind("argument");
        entryBuilder.type(className);
        entryBuilders.add(entryBuilder);
    }

    private void compileParameterList(SymbolTable symbolTable) throws IOException {
        writeOutput("<parameterList>");
        DEPTH++;
        entryBuilders = new ArrayList<>();
        if(enableMethod) constructThis();
        String token = nextToken();
        if(checkCurlyBracketLeft(token)){
            revertToken(1);
            DEPTH--;
            symbolTable.addEntry(entryBuilders);
            writeOutput("</parameterList>");
            return;
        }
        String nextToken;
        EntryBuilder entryBuilder = new EntryBuilder();
        String next_nextToken;
        if(token.startsWith(KEYWORD) && Pattern.compile("(int)|(char)|(boolean)").matcher(token).find()){
            entryBuilder.type(token);
            writeOutput(token);
        }
        // className
        else if(token.startsWith(IDENTIFIER)){
            entryBuilder.type(token);
            writeOutput(token);
        }
        else{
            revertToken(1);
            return;
        }
        token = nextToken();
        // it might be better if we throw an exception
        if(!token.startsWith(IDENTIFIER)){
            revertToken(2);
            return;
        }
        entryBuilders.add(entryBuilder);
        writeOutput(token);
        entryBuilder.name(token);
        entryBuilder.kind("argument");
        token = nextToken();
        nextToken = nextToken();
        next_nextToken = nextToken();
        while((token.startsWith(SYMBOL) && checkKomma(token))
            && (nextToken.startsWith(KEYWORD) && Pattern.compile("(int)|(char)|(boolean)").matcher(nextToken).find()
                | nextToken.startsWith(IDENTIFIER)) && (next_nextToken.startsWith(IDENTIFIER))){
            EntryBuilder entryBuilder1 = new EntryBuilder();
            entryBuilder1.type(nextToken);
            entryBuilder1.kind("argument");
            entryBuilder1.name(next_nextToken);
            entryBuilders.add(entryBuilder1);
            writeOutput(token);
            writeOutput(nextToken);
            writeOutput(next_nextToken);
            token = nextToken();
            nextToken = nextToken();
            next_nextToken = nextToken();
        }
        revertToken(3);
        DEPTH--;
        symbolTable.addEntry(entryBuilders);
        writeOutput("</parameterList>");
    }

    private void compileSubRoutineBody(SymbolTable symbolTable) throws Exception {
        writeOutput("<subroutineBody>");
        DEPTH++;
        String token = nextToken();
        if(token.startsWith(SYMBOL) && Pattern.compile("[{]").matcher(token).find()){
            writeOutput(token);
        }
        else{
            throw new Exception("");
        }
        compileVarDec(symbolTable);
        vmCodeGenerator.writeFunction(symbolTables,className,methodName);
        if(constructor) vmCodeGenerator.writeAlloc(symbolTables);
        if(enableMethod) {
            vmCodeGenerator.writePush("argument",0);
            vmCodeGenerator.writePop("pointer",0);
        }
        constructor = false;
        compileStatements();
        token = currentToken();
        if(token.startsWith(SYMBOL) && Pattern.compile("[}]").matcher(token).find()){
            writeOutput(token);
        }
        else{
            throw new Exception("");
        }
        DEPTH--;
        writeOutput("</subroutineBody>");
    }

    private void compileVarDec(SymbolTable symbolTable) throws Exception {
        String token = nextToken();
        while(token.startsWith(KEYWORD) && Pattern.compile("(var)").matcher(token).find()){
            writeOutput("<varDec>");
            entryBuilders = new ArrayList<>();
            DEPTH++;
            EntryBuilder entryBuilder = new EntryBuilder();
            entryBuilders.add(entryBuilder);
            entryBuilder.kind("local");
            writeOutput(token);
            token = nextToken();
            if(token.startsWith(IDENTIFIER)){
                writeOutput(token);
            }
            else if(Pattern.compile("(int)|(char)|(boolean)").matcher(token).find()){
                writeOutput(token);
            }
            else{
                throw new Exception("");
            }
            entryBuilder.type(token);
            token = nextToken();
            if(token.startsWith(IDENTIFIER)){
                writeOutput(token);
                entryBuilder.name(token);
            }
            else{
                throw new Exception("");
            }
            token = nextToken();
            token = potentialNextIdentifier(token);
            if(token.startsWith(SYMBOL) && Pattern.compile("[;]").matcher(token).find()){
                writeOutput(token);
            }
            else{
                throw new Exception("");
            }
            symbolTable.addEntry(entryBuilders);
            token = nextToken();
            DEPTH--;
            writeOutput("</varDec>");
        }
        revertToken(1);
    }

    private String potentialNextIdentifier(String token) throws IOException {
        // get latest entry
        EntryBuilder entryBuilder = this.entryBuilders.get(entryBuilders.size()-1);
        String nextToken;
        nextToken = nextToken();
        while (token.startsWith(SYMBOL) && Pattern.compile("[,]").matcher(token).find()
                && nextToken.startsWith(IDENTIFIER)){
            EntryBuilder entryBuilderNew = new EntryBuilder();
            entryBuilderNew.kind(entryBuilder.getKind());
            entryBuilderNew.type(entryBuilder.getType());
            entryBuilderNew.name(nextToken);
            this.entryBuilders.add(entryBuilderNew);
            writeOutput(token);
            writeOutput(nextToken);
            token = nextToken();
            nextToken = nextToken();
        }
        revertToken(2);
        token = nextToken();
        return token;
    }

    // TODO: 15.06.22 -> Statements zuende machen
    private void compileStatements() throws Exception {
        writeOutput("<statements>");
        DEPTH++;
        String token = nextToken();
        while(Pattern.compile("(let)|(if)|(while)|(do)|(return)").matcher(token).find()){
            switch (token){
                case "<keyword> let </keyword>" -> compileLetStatement();
                case "<keyword> if </keyword>" -> compileIfStatement();
                case "<keyword> while </keyword>" -> compileWhileStatement();
                case "<keyword> do </keyword>" -> compileDoStatement();
                case "<keyword> return </keyword>" -> compileReturnStatement();
            }
            token = nextToken();
        }
        DEPTH--;
        writeOutput("</statements>");
    }

    private void compileReturnStatement() throws Exception {
        writeOutput("<returnStatement>");
        DEPTH++;
        writeOutput(currentToken());
        String token = nextToken();
        if(!checkSemicolon(token)){
            compileExpression();
        }
        else{
            vmCodeGenerator.writeDo();
        }
        writeOutput(currentToken());
        DEPTH--;
        vmCodeGenerator.writeReturn();
        writeOutput("</returnStatement>");
    }
    private void compileDoStatement() throws Exception {
        writeOutput("<doStatement>");
        DEPTH++;
        writeOutput(currentToken());
        String token;
        token = nextToken();
        if(!token.startsWith(IDENTIFIER)){
            throw new Exception("");
        }
        writeOutput(token);
        compileSubRoutineCall();
        token = nextToken();
        if(!checkSemicolon(token)){
            throw new Exception("");
        }
        writeOutput(token);
        DEPTH--;
        vmCodeGenerator.writePop("temp",0);
        writeOutput("</doStatement>");
    }

    private void compileWhileStatement() throws Exception {
        writeOutput("<whileStatement>");
        vmCodeGenerator.writeLabel(true);
        checkStatement(true);
        vmCodeGenerator.writeGoto(true);
        String token;
        token = currentToken();
        if(!checkSnakeBracketLeft(token)){
            throw new Exception("");
        }
        vmCodeGenerator.writeEndLabel(true);
        writeOutput(token);
        DEPTH--;
        writeOutput("</whileStatement>");
    }

    private void checkStatement(boolean isWhile) throws Exception {
        DEPTH++;
        writeOutput(currentToken());
        String token;
        token = nextToken();
        if(!checkCurlyBracketRight(token)){
            throw new Exception("");
        }
        writeOutput(token);
        token = nextToken();
        compileExpression();
        vmCodeGenerator.writeArithmetic("not",false);
        token = currentToken();
        if(!checkCurlyBracketLeft(token)){
            throw new Exception("");
        }
        writeOutput(token);
        token = nextToken();
        if(!checkSnakeBracketRight(token)){
            throw new Exception("");
        }
        writeOutput(token);
        vmCodeGenerator.writeIfStatement(isWhile);
        compileStatements();
    }

    private void compileIfStatement() throws Exception {
        writeOutput("<ifStatement>");
        checkStatement(false);
        vmCodeGenerator.writeGoto(false);
        String token;
        if(!checkSnakeBracketLeft(currentToken())){
            throw new Exception("");
        }
        vmCodeGenerator.writeLabel(false);
        writeOutput(currentToken());
        token = nextToken();
        if(Pattern.compile("(else)").matcher(token).find()){
            writeOutput(token);
            token = nextToken();
            if(!checkSnakeBracketRight(token)){
                throw new Exception("");
            }
            writeOutput(token);
            compileStatements();
            token = currentToken();
            if(!checkSnakeBracketLeft(token)){
                throw new Exception("");
            }
            writeOutput(token);
            // Ausgleich zum reverten
            nextToken();
            //vmCodeGenerator.writeGoto(false);
        }
        vmCodeGenerator.writeEndLabel(false);
        revertToken(1);
        DEPTH--;
        writeOutput("</ifStatement>");
    }

    private boolean checkKomma(String token){
        return Pattern.compile("[,]").matcher(token).find();
    }

    private boolean checkSemicolon(String token){
        return Pattern.compile("[;]").matcher(token).find();
    }
    // (
    private boolean checkCurlyBracketRight(String token){
        return Pattern.compile("[(]").matcher(token).find();
    }
    // {
    private boolean checkSnakeBracketRight(String token){
        return Pattern.compile("[{]").matcher(token).find();
    }
    // }
    private boolean checkSnakeBracketLeft(String token){
        return Pattern.compile("[}]").matcher(token).find();
    }
    // )
    private boolean checkCurlyBracketLeft(String token){
        return Pattern.compile("[)]").matcher(token).find();
    }

    private void compileLetStatement() throws Exception {
        writeOutput("<letStatement>");
        boolean trigger = false;
        DEPTH++;
        writeOutput(currentToken());
        String token;
        token = nextToken();
        variable = token;
        if(token.startsWith(IDENTIFIER)){
            writeOutput(token);
        }
        else{
            throw new Exception("");
        }
        token = nextToken();
        if(token.startsWith(SYMBOL) && token.contains("[")){
            trigger = true;
            vmCodeGenerator.push(variable,symbolTables);
            writeOutput(token);
            token = nextToken();
            compileExpression();
            token = currentToken();
            if(token.startsWith(SYMBOL) && token.contains("]")){
                writeOutput(token);
            }
            else{
                throw new Exception("");
            }
            vmCodeGenerator.writeArithmetic("+",false);
            token = nextToken();
        }
        if(token.startsWith(SYMBOL) && Pattern.compile("[=]").matcher(token).find()){
            writeOutput(token);
        }
        else{
            throw new Exception("");
        }
        token = nextToken();
        compileExpression();
        token = currentToken();
        if(token.startsWith(SYMBOL) && Pattern.compile("[;]").matcher(token).find()){
            writeOutput(token);
        }
        else{
            throw new Exception("");
        }
        if(trigger) writeArray();
        else vmCodeGenerator.pop(variable,symbolTables);
        DEPTH--;
        writeOutput("</letStatement>");
    }

    private void writeArray(){
        vmCodeGenerator.writePop("temp",0);
        vmCodeGenerator.writePop("pointer",1);
        vmCodeGenerator.writePush("temp",0);
        vmCodeGenerator.writePop("that",0);
    }

    // integerConstant and stringConstant don't have to be evaluated cause if in token stream the conditions are met
    private void compileExpression() throws Exception {
        writeOutput("<expression>");
        DEPTH++;
        String subString,arithmetic;
        String token = currentToken();
        compileTerm(token);
        if(checkSemicolon(currentToken()) || checkCurlyBracketLeft(token)){
            DEPTH--;
            writeOutput("</expression>");
            nextToken();
            return;
        }
        token = nextToken();
        subString = subString(token);
        while (Pattern.compile("[+]|(-)|[*]|(/)|(&amp)|[|]|(&lt)|(&gt)|(=)").matcher(subString).find()){
            arithmetic = subString;
            writeOutput(token);
            token = nextToken();
            subString = subString(token);
            compileTerm(token);
            vmCodeGenerator.writeArithmetic(arithmetic,false);
            token = nextToken();
        }
        DEPTH--;
        writeOutput("</expression>");
    }

    private String subString(String token){
        String subString;
        if(Pattern.compile("(&lt;)|(&gt;)").matcher(token).find()){
            subString = token.substring(9,12);
        }
        else if(Pattern.compile("(&amp;)").matcher(token).find()){
            subString = token.substring(9,13);
        }
        else{
            subString = token.substring(9,10);
        }
        return subString;
    }

    private void compileTerm(String token) throws Exception {
        String c;
        writeOutput("<term>");
        DEPTH++;
        if(Pattern.compile("[0-9]+").matcher(token).find() && token.startsWith(INT_CONSTANT)){
            writeOutput(token);
            vmCodeGenerator.push(token,symbolTables);
        }
        else if(token.startsWith(STRING_CONSTANT)){
            writeOutput(token);
            vmCodeGenerator.writeString(token);
        }
        else if(Pattern.compile("(true)|(false)|(null)|(this)").matcher(token).find()){
            writeOutput(token);
            vmCodeGenerator.push(token,symbolTables);
        }
        // subroutineName & varName
        else if(token.startsWith(IDENTIFIER)){
            c = token;
            writeOutput(token);
            token = nextToken();
            if(token.startsWith(SYMBOL) && token.contains("[")){
                vmCodeGenerator.push(c,symbolTables);
                writeOutput(token);
                token = nextToken();
                compileExpression();
                vmCodeGenerator.writeArithmetic("+",false);
                vmCodeGenerator.writePop("pointer",1);
                vmCodeGenerator.writePush("that",0);
                token = currentToken();
                if(Pattern.compile("[]]").matcher(token).find() && token.startsWith(SYMBOL)){
                    writeOutput(token);
                }
                else{
                    throw new Exception("");
                }
                DEPTH--;
                writeOutput("</term>");
                return;
            }
            /*if(checkSemicolon(token) || checkKomma(token)){
                DEPTH--;
                revertToken(1);

                writeOutput("</term>");
                return;
            }
             */
            revertToken(1);
            compileSubRoutineCall();
            // if token-pointer is not increased it is 100% varName
        }
        else if(token.startsWith(SYMBOL) && checkCurlyBracketRight(token)){
            writeOutput(token);
            token = nextToken();
            compileExpression();
            token = currentToken();
            if(!checkCurlyBracketLeft(token)){
                throw new Exception("");
            }
            writeOutput(token);
        }
        else if(Pattern.compile("(-)|(~)").matcher(token).find()){
            String arithmetic = currentToken();
            writeOutput(token);
            token = nextToken();
            compileTerm(token);
            vmCodeGenerator.writeArithmetic(arithmetic,true);
        }
        else{
            token = nextToken();
            if(checkCurlyBracketLeft(token)){
                writeOutput(token);
                return;
            }
            throw new Exception("");
        }
        DEPTH--;
        writeOutput("</term>");
    }

    private void compileSubRoutineCall() throws Exception {
        String currentToken = currentToken();   // might be identifier
        int exCounter = 0;
        String token = nextToken();
        if(Pattern.compile("[(]").matcher(token).find()){
            exCounter += 1;
            writeOutput(token);
            vmCodeGenerator.writePush("pointer",0);
            exCounter += compileExpressionList();
            vmCodeGenerator.writeCall(currentToken,className,exCounter);
            token = currentToken();
            if(Pattern.compile("[)]").matcher(token).find()){
                writeOutput(token);
            }
            else{
                throw new Exception("");
            }
            return;
        }
        else if(Pattern.compile("[.]").matcher(token).find() && token.startsWith(SYMBOL)){
            writeOutput(token);
            token = nextToken();
            if(token.startsWith(IDENTIFIER)){
                String s = currentToken();
                writeOutput(token);
                token = nextToken();
                if(Pattern.compile("[(]").matcher(token).find() && token.startsWith(SYMBOL)){
                    writeOutput(token);
                    Entry hit = isHit(currentToken);
                    if(hit != null){
                        vmCodeGenerator.push(currentToken,symbolTables);
                        currentToken = "<>" + hit.type + "</>";
                        exCounter += 1;
                    }
                    exCounter += compileExpressionList();
                    vmCodeGenerator.writeCall(constructFunctionName(currentToken,s),"",exCounter);
                    token = currentToken();
                    if(Pattern.compile("[)]").matcher(token).find() && token.startsWith(SYMBOL)){
                        writeOutput(token);
                    }
                    else{
                        throw new Exception("");
                    }
                    return;
                }
                else{
                    throw new Exception("");
                }
            }
            else{
                throw new Exception("");
            }
        }
        revertToken(1);
        vmCodeGenerator.push(currentToken(),symbolTables);
    }

    private Entry isHit(String currentToken){
        String s = reduce(currentToken);
        Optional<Optional<Entry>> any = symbolTables.stream().map(x -> x.hit(s)).filter(Optional::isPresent)
                .findAny();
        return any.map(Optional::get).orElse(null);
    }

    private String reduce(String s){
        if(s.split("[ ]").length == 1) return s;
        return s.strip().split("(<)[a-zA-Z]*(>)|(</)[a-zA-Z]*(>)")[1].strip();
    }

    private String constructFunctionName(String s, String r){
        s = s.strip().split("(<)[a-z]*(>)|(</)[a-z]*(>)")[1].strip();
        r = r.strip().split("(<)[a-z]*(>)|(</)[a-z]*(>)")[1].strip();
        return s + "." + r;
    }

    // TODO: 16.06.22
    // Anzahl der Argumente die reingegeben werden -> wichtig für den call
    private int compileExpressionList() throws Exception {
        int counter = 0;
        writeOutput("<expressionList>");
        DEPTH++;
        String token;
        token = nextToken();
        if(checkCurlyBracketLeft(token)){
            DEPTH--;
            writeOutput("</expressionList>");
            return 0;
        }
        compileExpression();
        counter += 1;
        token = currentToken();
        while (token.startsWith(SYMBOL) && checkKomma(token)){
            writeOutput(token);
            token = nextToken();
            compileExpression();
            token = currentToken();
            counter += 1;
        }
        DEPTH--;
        writeOutput("</expressionList>");
        return counter;
    }

    private boolean ignoreTokenKeyWord(String line){
        return line.equals("<tokens>") || line.equals("</tokens>");
    }
}
