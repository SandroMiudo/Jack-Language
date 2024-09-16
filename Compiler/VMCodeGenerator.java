import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.regex.Pattern;

public class VMCodeGenerator {

    private final FileOutputStream fileOutputStream;
    private final Stack<Integer> whileStack = new Stack<>();
    private final Stack<Integer> ifStack = new Stack<>();
    private int whileCounter = 0;
    private int ifCounter = 0;

    public VMCodeGenerator(FileOutputStream fileOutputStream) {
        this.fileOutputStream = fileOutputStream;
    }

    private String reduce(String s){
        if(s.split("[ ]").length == 1) return s;
        return s.strip().split("(<)[a-zA-Z]*(>)|(</)[a-zA-Z]*(>)")[1].strip();
    }

    public void writeAlloc(List<SymbolTable> symbolTables){
        SymbolTable classLevel = symbolTables.get(0);
        String s = classLevel.retrieveFields();
        push(s,symbolTables);
        try {
            fileOutputStream.write("call Memory.alloc 1\n".getBytes());
            fileOutputStream.write("pop pointer 0\n".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void push(String s, List<SymbolTable> symbolTables){
        Entry entry;
        SymbolTable classLevel = symbolTables.get(0);
        SymbolTable subroutine = symbolTables.get(1);
        String r = reduce(s);
        if(classLevel.hit(r).isPresent()){
            entry = classLevel.hit(r).get();
            if(entry.kind.equals("field")){
                writePush("this",entry.index);
            }
            else{
                writePush(entry.kind,entry.index);
            }
        }
        else if(subroutine.hit(r).isPresent()){
            entry = subroutine.hit(r).get();
            // first arg
            if(entry.name.equals("this")) writePush("pointer",0);
            else writePush(entry.kind,entry.index);
        }
        else if(Pattern.compile("[0-9]+").matcher(r).find()){
            writePush("constant",Integer.parseInt(r));
        }
        else if(r.equals("null")){
            writePush("constant",0);
        }
        else if(r.equals("true")){
            writePush("constant",0);
            writeArithmetic("not",true);
        }
        else if(r.equals("false")){
            writePush("constant",0);
        }
        // if not in symbol table
        else if(r.equals("this")){
            writePush("pointer",0);
        }
    }

    public void pop(String s, List<SymbolTable> symbolTables){
        Entry entry;
        SymbolTable classLevel = symbolTables.get(0);
        SymbolTable subroutine = symbolTables.get(1);
        String r = reduce(s);
        if(classLevel.hit(r).isPresent()){
            entry = classLevel.hit(r).get();
            if(entry.kind.equals("static")){
                writePop(entry.kind,entry.index);
            }
            else{
                writePop("this",entry.index);
            }
            //if(entry.type.equals("Array")) writePop("that",0);
        }
        else if(subroutine.hit(r).isPresent()){
            entry = subroutine.hit(r).get();
            /*if(entry.type.equals("Array")) {
                writePop("that",0);
                return;
            }*/
            writePop(entry.kind,entry.index);
        }
    }

    public void writePop(String segment, int index){
        try {
            fileOutputStream.write(String.format("pop %s %s\n",segment,index).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeFunction(List<SymbolTable> symbolTables, String classname, String methodname){
        SymbolTable subroutine = symbolTables.get(1);
        int i = subroutine.retrieveLocalVariables();
        try {
            fileOutputStream.write(String.format("function %s.%s %d\n",classname,methodname,i).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeCall(String s, String classname,int args){
        if(classname.isBlank()){
            try {
                fileOutputStream.write(String.format("call %s %s\n",s,args).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            try {
                fileOutputStream.write(String.format("call %s.%s %s\n",classname,reduce(s),args).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // value entweder else or if
    public void writeLabel(boolean isWhile){
        if(isWhile){
            whileStack.push(whileCounter);
            try {
                fileOutputStream.write(String.format("label WHILE_START%s\n",whileCounter).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            whileCounter++;
            return;
        }
        Integer peek = ifStack.peek();
        try {
            fileOutputStream.write(String.format("label IF_CASE%s\n",peek).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void writeIfStatement(boolean isWhile){
        if(isWhile){
            Integer peek = whileStack.peek();
            try {
                fileOutputStream.write(String.format("if-goto WHILE_END%s\n",peek).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        ifStack.push(ifCounter);
        try {
            fileOutputStream.write(String.format("if-goto IF_CASE%s\n",ifCounter).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        ifCounter++;
    }

    public void writeEndLabel(boolean isWhile){
        if(isWhile){
            Integer pop = whileStack.pop();
            try {
                fileOutputStream.write(String.format("label WHILE_END%s\n",pop).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        Integer pop = ifStack.pop();
        try {
            fileOutputStream.write(String.format("label IF_END%s\n",pop).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    // 0 & 1 -> 1 jump to end label
    public void writeGoto(boolean isWhile){
        if(isWhile){
            try {
                fileOutputStream.write(String.format("goto WHILE_START%s\n",whileStack.peek()).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            fileOutputStream.write(String.format("goto IF_END%s\n",ifStack.peek()).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void writeReturn(){
        try {
            fileOutputStream.write("return\n".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void writePush(String segment, int i){
        try {
            fileOutputStream.write(String.format("push %s %d\n",segment,i).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //[+]|(-)|[*]|(/)|(&amp)|[|]|(&lt)|(&gt)|(=)
    public void writeArithmetic(String arithmetic, boolean trigger) {
        if(trigger){
            if(arithmetic.split(" ").length != 1) arithmetic = reduce(arithmetic);
            switch (arithmetic){
                case "~" -> arithmetic = "not";
                case "-" -> arithmetic = "neg";
            }
        }
        else{
            if(arithmetic.startsWith("&")){
                arithmetic = arithmetic.split("[&]")[1];
            }
            switch (arithmetic) {
                case "+" -> arithmetic = "add";
                case "-" -> arithmetic = "sub";
                case "amp" -> arithmetic = "and";
                case "|" -> arithmetic = "or";
                case "=" -> arithmetic = "eq";
                case "*" -> arithmetic = "call Math.multiply 2";
                case "/" -> arithmetic = "call Math.divide 2";
            }
        }
        try {
            fileOutputStream.write((arithmetic+"\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeDo() {
        writePush("constant",0);
    }

    public void writeString(String token) {
        String reduce = reduce(token);
        writePush("constant",reduce.length());
        writeCall("String.new","",1);
        for(int i = 0; i < reduce.length(); i++){
            writePush("constant",reduce.charAt(i));
            writeCall("String.appendChar","",2);
        }
    }
}
