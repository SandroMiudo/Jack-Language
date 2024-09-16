import java.io.*;
import java.util.Arrays;
import java.util.List;

public class Tokenizer {
    private BufferedInputStream inputStream;
    private final FileOutputStream fileOutputStream;
    private boolean BLOCK_COMMENT = false;

    private final List<String> symbols = List.of("{","}","(",")","[","]",".",",",";","+","-","*","/","&","|",
            "<",">","=","~");

    private final List<String> numbers = List.of("1","2","3","4","5","6","7","8","9","0");

    public Tokenizer(BufferedInputStream inputStream , FileOutputStream fileOutputStream) {
        this.inputStream = inputStream;
        this.fileOutputStream = fileOutputStream;
    }

    private String removeWhiteSpace(String line) {
        // replace all occurrences of ' '+Symbol or Symbol+' ' to Symbol
        line = line.replaceAll("( )+[}]","}");
        line = line.replaceAll("( )+[~]","~");
        line = line.replaceAll("( )+[)]",")");
        line = line.replaceAll("( )+[,]",",");
        line = line.replaceAll("( )+[.]",".");
        line = line.replaceAll("( )+[;]",";");
        line = line.replaceAll("( )+[+]","+");
        line = line.replaceAll("( )+[-]","-");
        line = line.replaceAll("( )+[*]","*");
        line = line.replaceAll("( )+[/]","/");
        line = line.replaceAll("( )+[&]","&");
        line = line.replaceAll("( )+[|]","|");
        line = line.replaceAll("( )+[>]",">");
        line = line.replaceAll("( )+[=]","=");
        line = line.replaceAll("( )+[(]","(");
        line = line.replaceAll("( )+[{]", "{");
        line = line.replaceAll("( )+[<]","<");
        line = line.replaceAll("[<]( )+","<");
        // there might be a better solution :)
        line = line.replaceAll("( )+(let)( )","let ");
        line = line.replaceAll("( )+(var)( )","var ");
        line = line.replaceAll("( )+(while)","while");
        line = line.replaceAll("( )+(if)","if");
        line = line.replaceAll("( )+(function)( )","function ");
        line = line.replaceAll("( )+(return)","return");
        line = line.replaceAll("( )+(do)( )","do ");
        line = line.replaceAll("( )+(method)( )","method ");
        line = line.replaceAll("( )+(static)( )","static ");
        line = line.replaceAll("( )+(constructor)( )","constructor ");
        line = line.replaceAll("( )+(field)( )","field ");
        return line;
    }

    private String clearComments(String line) {
        line = line.replaceAll("(\s)*//(.)*","");
        line = line.replaceAll("(\s)*(/[*])(.)*([*]/)","");
        return line;
    }

    private void tokenize(String line){
        String raw = "";
        String substring = "";
        char c = '"';
        String s = "";
        s += c;
        TokenizerType tokenizerType;
        for(int i = 0; i < line.length(); i++){
            raw += line.charAt(i);
            if(!raw.startsWith(s)){
               raw = raw.strip();
            }
            if((i+1) < line.length()){
                substring = line.substring(i+1);
            }
            if(checkSymbol(raw)){
                tokenizerType = TokenizerType.SYMBOL;
                writeKeywordToken(tokenizerType,raw);
                raw = "";
            }
            else if(checkKeyWord(raw,substring)){
                tokenizerType = TokenizerType.KEYWORD;
                writeKeywordToken(tokenizerType,raw);
                raw = "";
            }
            else if(checkIntegerConstant(raw,substring)){
                if(Integer.parseInt(raw) > 32767 || Integer.parseInt(raw) < 0){
                    continue;
                }
                tokenizerType = TokenizerType.INT_CONST;
                writeKeywordToken(tokenizerType,raw);
                raw = "";
            }
            else if(checkStringConstant(raw,substring)){
                tokenizerType = TokenizerType.STRING_CONST;
                writeKeywordToken(tokenizerType,raw);
                raw = "";
            }
            else if(checkIdentifier(raw,substring)){
                tokenizerType = TokenizerType.IDENTIFIER;
                writeKeywordToken(tokenizerType,raw);
                raw = "";
            }
        }
    }

    private void writeToken(String s){
        try {
            fileOutputStream.write(s.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void readLines() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        writeToken("<tokens>\n");
        while ((line = bufferedReader.readLine()) != null) {
            if(skipLine(line)){
                continue;
            }
            line = clearComments(line);
            line = removeWhiteSpace(line);
            tokenize(line);
        }
        writeToken("</tokens>\n");
    }
    /*

     */ // wenn hier jetzt Code stehen würde dann würden wir diese überspringen
    // TODO: 15.06.22 Fix Comment Problem
    private boolean skipLine(String line){
        if(line.trim().startsWith("/*") && !line.trim().endsWith("*/")) {
            BLOCK_COMMENT = true;
            return true;
        }
        if(!line.trim().startsWith("/*") && line.trim().endsWith("*/")){
            BLOCK_COMMENT = false;
            return true;
        }
        return BLOCK_COMMENT;
    }

    private boolean checkIdentifier(String raw, String line) {
        // character first digit not allowed
        if(raw.length() == 0){
            return false;
        }
        if(raw.substring(0,1).matches("[0-9]")){
            return false;
        }
        if(raw.charAt(0) == '"'){
            return false;
        }
        // non symbols
        for(String s : symbols){
            if(raw.contains(s)){
                return false;
            }
        }
        return symbols.stream().anyMatch(line::startsWith) || line.startsWith(" ");
    }

    // needs to be fixed -> <= / >= should be allowed and not spliced into < , = and > , =
    private boolean checkSymbol(String raw){
        return symbols.stream().anyMatch(x -> x.equals(raw));
    }

    private boolean checkKeyWord(String raw, String line){
        return Arrays.stream(TokenizerKeyWord.values()).map(TokenizerKeyWord::getName).anyMatch(x -> x.equals(raw)) &&
                (line.charAt(0) == ' ' || symbols.stream().anyMatch(line::startsWith));
    }

    private boolean checkIntegerConstant(String raw, String line){
        try{
            Integer i = Integer.parseInt(raw);
        }
        catch (Exception exception){
            return false;
        }
        return numbers.stream().noneMatch(line::startsWith);
    }


    private boolean checkStringConstant(String raw, String line){
        if(raw.length() < 2){
            return false;
        }
        if(raw.charAt(0) == '"' && raw.charAt(raw.length()-1) == '"' && !line.contains("'")){
            for(int i = 1; i < raw.length()-1; i++){
                if(raw.charAt(i) == '"' || raw.charAt(i) == '\n'){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private void writeKeywordToken(TokenizerType tokenizerType, String value){
        value = convertSymbol(value);
        try {
            fileOutputStream.write(tokenizerType.constructT(value).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String convertSymbol(String value){
        return switch (value) {
            case "<" -> "&lt;";
            case "<=" -> "&leq;";
            case ">" -> "&gt;";
            case ">=" -> "&geq;";
            case "&" -> "&amp;";
            default -> value;
        };
    }
}
